package org.sonar.samples.java.checks;

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
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

/**
 * Custom SonarQube rule inspired by:
 * "Automated detection of code smells caused by null checking conditions
 * in Java programs" (IEEE SAC).
 *
 * <p>Where that paper used regular expressions to spot recurring
 * null-checking conditionals, this rule uses SonarJava's AST + semantic
 * model to find the same pattern more reliably: a field whose null-ness
 * (== null / != null) is tested independently in several different
 * methods of the same class. That repetition is the Wake/Fowler "Null
 * Check" smell — the missing abstraction is a Null Object that would let
 * callers stop asking "is this null?" and just invoke behavior.
 *
 * <p>This is a starting point, not a drop-in replacement for the paper's
 * evaluation: it does not attempt duplicated-code detection or automatic
 * refactoring, only smell identification, and it only looks at direct
 * {@code == null} / {@code != null} comparisons (not {@code Objects.isNull},
 * {@code Optional.isPresent()}, or {@code instanceof} patterns — see
 * "Limitations" in the accompanying rule description).
 */
@Rule(key = "ExcessiveNullCheck", name = "Fields whose null-checks are duplicated across many methods should be replaced with a Null Object")
public class ExcessiveNullCheckCheck extends IssuableSubscriptionVisitor {

  private static final int DEFAULT_THRESHOLD = 3;

  @RuleProperty(
      key = "threshold",
      description = "Minimum number of distinct methods that must null-check the same field before an issue is raised",
      defaultValue = "" + DEFAULT_THRESHOLD)
  public int threshold = DEFAULT_THRESHOLD;

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTree classTree = (ClassTree) tree;

    // field symbol -> set of methods (by identity) in which it is null-checked
    Map<Symbol, Set<MethodTree>> nullChecksByField = new HashMap<>();

    for (Tree member : classTree.members()) {
      if (member.is(Tree.Kind.METHOD, Tree.Kind.CONSTRUCTOR)) {
        MethodTree method = (MethodTree) member;
        if (method.block() == null) {
          continue; // abstract / interface method, no body to scan
        }
        NullCheckCollector collector = new NullCheckCollector(nullChecksByField, method);
        method.block().accept(collector);
      }
    }

    for (Map.Entry<Symbol, Set<MethodTree>> entry : nullChecksByField.entrySet()) {
      int methodCount = entry.getValue().size();
      if (methodCount >= threshold) {
        Symbol field = entry.getKey();
        reportIssue(
            classTree.simpleName(),
            String.format(
                "Field '%s' is null-checked in %d different methods of this class. "
                    + "Consider introducing a Null Object (or java.util.Optional) so callers "
                    + "no longer need to repeat this check.",
                field.name(), methodCount));
      }
    }
  }

  /**
   * Walks a single method body and records, for every field-vs-null
   * comparison found, which field was checked and in which method.
   */
  private static class NullCheckCollector extends BaseTreeVisitor {

    private final Map<Symbol, Set<MethodTree>> nullChecksByField;
    private final MethodTree currentMethod;

    NullCheckCollector(Map<Symbol, Set<MethodTree>> nullChecksByField, MethodTree currentMethod) {
      this.nullChecksByField = nullChecksByField;
      this.currentMethod = currentMethod;
    }

    @Override
    public void visitBinaryExpression(BinaryExpressionTree tree) {
      if (tree.is(Tree.Kind.EQUAL_TO, Tree.Kind.NOT_EQUAL_TO)) {
        Symbol field = fieldComparedToNull(tree);
        if (field != null) {
          nullChecksByField.computeIfAbsent(field, k -> new HashSet<>()).add(currentMethod);
        }
      }
      super.visitBinaryExpression(tree);
    }

    /**
     * If exactly one side of the comparison is the {@code null} literal
     * and the other side resolves to a field (instance or static, not a
     * local variable or parameter), return that field's symbol.
     */
    private Symbol fieldComparedToNull(BinaryExpressionTree tree) {
      ExpressionTree left = tree.leftOperand();
      ExpressionTree right = tree.rightOperand();

      ExpressionTree candidate;
      if (left.is(Tree.Kind.NULL_LITERAL)) {
        candidate = right;
      } else if (right.is(Tree.Kind.NULL_LITERAL)) {
        candidate = left;
      } else {
        return null;
      }

      Symbol symbol = resolveSymbol(candidate);
      if (symbol != null && symbol.isVariableSymbol() && !symbol.isLocalVariable()) {
        return symbol;
      }
      return null;
    }

    private Symbol resolveSymbol(ExpressionTree expr) {
      if (expr.is(Tree.Kind.IDENTIFIER)) {
        return ((IdentifierTree) expr).symbol();
      }
      if (expr.is(Tree.Kind.MEMBER_SELECT)) {
        return ((MemberSelectExpressionTree) expr).identifier().symbol();
      }
      return null;
    }
  }
}
