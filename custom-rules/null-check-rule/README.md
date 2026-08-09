# ExcessiveNullCheck — a custom SonarQube rule

## What it detects

A field whose null-ness (`== null` / `!= null`) is checked independently in
**3 or more distinct methods** (configurable) of the same class. That
repetition is the Wake/Fowler **Null Check** smell: the class is missing a
proper representation of "absent," so every caller re-implements the same
guard instead of the class doing it once, internally.

This is the same underlying idea as:

> "Automated detection of code smells caused by null checking conditions in
> Java programs" (IEEE Symposium on Applied Computing) — which used
> **regular expressions** over source text to spot recurring
> null-checking conditionals and automate their elimination via the
> **Null Object** design pattern.

## How this differs from the paper's approach

| | Paper (regex-based) | This rule (AST-based) |
|---|---|---|
| Detection mechanism | Regex over source text | SonarJava AST + symbol table |
| Distinguishes fields from locals/params with the same name | Not reliably | Yes — uses `Symbol.isLocalVariable()` |
| Cross-method correlation | Ad hoc | Explicit: groups checks by resolved field `Symbol` |
| Integrates into CI/quality gate | No | Yes — runs as a normal SonarQube rule with severity, remediation cost, and a configurable threshold |
| Automatic refactoring | Yes (their focus) | No — this rule only **flags** candidates; it does not generate the Null Object |

The regex approach is simpler to stand up outside SonarQube, but it will
misfire on shadowed names, string literals containing the field name, or
checks split across multiple lines. The AST-based version avoids all of
that at the cost of needing to be built against SonarJava's API.

## Project layout

```
src/main/java/org/sonar/samples/java/
  ExcessiveNullCheckPlugin.java              # plugin entry point
  checks/CustomRuleRepository.java           # registers the check
  checks/ExcessiveNullCheckCheck.java        # the rule logic

src/main/resources/org/sonar/l10n/java/rules/java/
  ExcessiveNullCheck.html                     # rule description (shown in SonarQube UI)
  ExcessiveNullCheck.json                     # rule metadata (severity, tags, remediation cost)

src/test/resources/checks/ExcessiveNullCheckCheck.java   # noncompliant + compliant fixtures
src/test/java/.../ExcessiveNullCheckCheckTest.java        # CheckVerifier-based test
```

## Building and installing

1. Set `sonar.java.version` in `pom.xml` to match the SonarJava analyzer
   version bundled with your target SonarQube Server/Cloud instance.
2. `mvn clean package` — produces a plugin JAR under `target/`.
3. Drop the JAR into `$SONARQUBE_HOME/extensions/plugins/` and restart
   SonarQube (or use SonarLint's custom-rules mechanism if targeting the IDE).
4. Activate the `ExcessiveNullCheck` rule in your Java quality profile and
   set the `threshold` property if 3 methods is too strict/lenient for
   your codebase.

## Known limitations (documented in the rule's HTML description too)

- Only direct `== null` / `!= null` comparisons are matched — not
  `Objects.isNull(...)`, `Optional.isPresent()`, or `instanceof` guard
  idioms.
- Counts distinct **methods** with a check, not distinct **checks** — two
  checks on the same field in one method count once.
- Flags a *candidate* for the Null Object refactoring; it doesn't verify a
  sensible default/no-op behavior actually exists for the field's type —
  that judgment is left to the developer.
- No duplicated-code detection or automatic refactoring, unlike the
  paper's tool — this rule's scope is limited to smell identification.

## Relevance to complexity/test-quality correlation studies

If you're using this alongside SonarQube's built-in rules (S134, S1067,
S2301, S2259, S3776) to study code-complexity metrics vs. LLM-generated
test quality, this rule gives you an actual, checkable "Null Check" signal
to include — filling the gap noted earlier that no mainstream SonarQube/PMD
rule targets this smell directly.
