package org.sonar.samples.java.checks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.IfStatementTree;
import org.sonar.plugins.java.api.tree.InstanceOfTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.ParenthesizedTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.SwitchStatementTree;
import org.sonar.plugins.java.api.tree.Tree;

/**
 * Custom SonarQube rule inspired by JDeodorant's "Type Checking" bad-smell
 * detector (Tsantalis, Chaikalis &amp; Chatzigeorgiou, CSMR 2008), which
 * corresponds to Fowler &amp; Beck's "Switch Statements" code smell — more
 * commonly discussed today as Conditional Complexity.
 *
 * <p>JDeodorant flags this smell when a class either (a) carries a "type
 * code" field that repeatedly drives branching logic across its methods,
 * or (b) contains a chain of type tests (classic {@code instanceof} chains)
 * that dispatch behavior by concrete type. Both patterns are resolved by
 * "Replace Conditional with Polymorphism" or "Replace Type Code with
 * State/Strategy."
 *
 * <p>Unlike SonarQube's built-in S134 (nesting depth) or S3776 (Cognitive
 * Complexity), which are blind to *why* a structure is complex, this rule
 * specifically targets type-based dispatch: it does not fire on switches
 * or if-chains that aren't discriminating on type.
 *
 * <p>Two independent detectors are implemented:
 * <ol>
 *   <li><b>Repeated type-code switch</b>: the same enum-typed field is used
 *       as a switch selector in two or more methods of the class.</li>
 *   <li><b>instanceof chain</b>: an if/else-if chain of three or more
 *       branches, each testing the same variable with {@code instanceof}.</li>
 * </ol>
 */
@Rule(key = "TypeChecking", name = "Classes should not dispatch behavior by repeatedly switching or testing on type")
public class TypeCheckingCheck extends IssuableSubscriptionVisitor {

  private static final int DEFAULT_MIN_SWITCH_OCCURRENCES = 2;
  private static final int DEFAULT_MIN_INSTANCEOF_CHAIN_LENGTH = 3;

  @RuleProperty(
      key = "minSwitchOccurrences",
      description = "Minimum number of distinct methods that must switch on the same type-code field before an issue is raised",
      defaultValue = "" + DEFAULT_MIN_SWITCH_OCCURRENCES)
  public int minSwitchOccurrences = DEFAULT_MIN_SWITCH_OCCURRENCES;

  @RuleProperty(
      key = "minInstanceofChainLength",
      description = "Minimum number of instanceof branches in a single if/else-if chain before an issue is raised",
      defaultValue = "" + DEFAULT_MIN_INSTANCEOF_CHAIN_LENGTH)
  public int minInstanceofChainLength = DEFAULT_MIN_INSTANCEOF_CHAIN_LENGTH;

  private final Set<IfStatementTree> alreadyReportedAsPartOfChain = new HashSet<>();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.CLASS, Tree.Kind.IF_STATEMENT);
  }

  @Override
  public void setContext(org.sonar.plugins.java.api.JavaFileScannerContext context) {
    alreadyReportedAsPartOfChain.clear();
    super.setContext(context);
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree.is(Tree.Kind.CLASS)) {
      checkRepeatedTypeCodeSwitch((ClassTree) tree);
    } else if (tree.is(Tree.Kind.IF_STATEMENT)) {
      checkInstanceofChain((IfStatementTree) tree);
    }
  }

  // ---- Detector 1: same type-code field switched on in several methods ----

  private void checkRepeatedTypeCodeSwitch(ClassTree classTree) {
    Map<Symbol, Set<MethodTree>> switchesByField = new HashMap<>();

    for (Tree member : classTree.members()) {
      if (member.is(Tree.Kind.METHOD, Tree.Kind.CONSTRUCTOR)) {
        MethodTree method = (MethodTree) member;
        if (method.block() == null) {
          continue;
        }
        method.block().accept(new SwitchOnFieldCollector(switchesByField, method));
      }
    }

    for (Map.Entry<Symbol, Set<MethodTree>> entry : switchesByField.entrySet()) {
      if (entry.getValue().size() >= minSwitchOccurrences) {
        reportIssue(
            classTree.simpleName(),
            String.format(
                "Type-code field '%s' drives a switch statement in %d different methods of this class. "
                    + "Consider Replace Conditional with Polymorphism or Replace Type Code with State/Strategy.",
                entry.getKey().name(), entry.getValue().size()));
      }
    }
  }

  private static class SwitchOnFieldCollector extends BaseTreeVisitor {
    private final Map<Symbol, Set<MethodTree>> switchesByField;
    private final MethodTree currentMethod;

    SwitchOnFieldCollector(Map<Symbol, Set<MethodTree>> switchesByField, MethodTree currentMethod) {
      this.switchesByField = switchesByField;
      this.currentMethod = currentMethod;
    }

    @Override
    public void visitSwitchStatement(SwitchStatementTree tree) {
      Symbol field = fieldSelector(tree.expression());
      if (field != null) {
        switchesByField.computeIfAbsent(field, k -> new HashSet<>()).add(currentMethod);
      }
      super.visitSwitchStatement(tree);
    }

    private Symbol fieldSelector(ExpressionTree expr) {
      Symbol symbol = resolveSymbol(unwrap(expr));
      if (symbol == null || !symbol.isVariableSymbol() || symbol.isLocalVariable()) {
        return null;
      }
      Type type = symbol.type();
      // Restrict to the classic "type code" shape: an enum-typed field.
      // (Loosen this check if you also want to catch int/String type codes.)
      if (type != null && type.symbol() != null && type.symbol().isEnum()) {
        return symbol;
      }
      return null;
    }
  }

  // ---- Detector 2: if/else-if chain of instanceof checks on the same variable ----

  private void checkInstanceofChain(IfStatementTree ifStatement) {
    if (alreadyReportedAsPartOfChain.contains(ifStatement)) {
      return;
    }

    Symbol chainVariable = instanceofVariable(ifStatement.condition());
    if (chainVariable == null) {
      return;
    }

    List<IfStatementTree> chain = new ArrayList<>();
    IfStatementTree current = ifStatement;
    while (current != null) {
      Symbol variable = instanceofVariable(current.condition());
      if (variable == null || !variable.equals(chainVariable)) {
        break;
      }
      chain.add(current);
      alreadyReportedAsPartOfChain.add(current);

      StatementTree elseStatement = current.elseStatement();
      current = elseStatement instanceof IfStatementTree ? (IfStatementTree) elseStatement : null;
    }

    if (chain.size() >= minInstanceofChainLength) {
      reportIssue(
          chain.get(0),
          String.format(
              "This if/else-if chain tests the type of '%s' in %d branches. "
                  + "Consider Replace Conditional with Polymorphism.",
              chainVariable.name(), chain.size()));
    }
  }

  private Symbol instanceofVariable(ExpressionTree condition) {
    ExpressionTree unwrapped = unwrap(condition);
    if (!unwrapped.is(Tree.Kind.INSTANCE_OF)) {
      return null;
    }
    InstanceOfTree instanceOf = (InstanceOfTree) unwrapped;
    return resolveSymbol(unwrap(instanceOf.expression()));
  }

  private static ExpressionTree unwrap(ExpressionTree expr) {
    ExpressionTree current = expr;
    while (current.is(Tree.Kind.PARENTHESIZED_EXPRESSION)) {
      current = ((ParenthesizedTree) current).expression();
    }
    return current;
  }

  private static Symbol resolveSymbol(ExpressionTree expr) {
    if (expr.is(Tree.Kind.IDENTIFIER)) {
      return ((IdentifierTree) expr).symbol();
    }
    if (expr.is(Tree.Kind.MEMBER_SELECT)) {
      return ((MemberSelectExpressionTree) expr).identifier().symbol();
    }
    return null;
  }
}
