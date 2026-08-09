# TypeChecking — a custom SonarQube rule

## What it detects

Fowler & Beck's **"Switch Statements"** smell (also called Type Checking, or
— in your terminology — **Conditional Complexity**): behavior that's
selected by repeatedly testing an object's type or a "type code" field,
rather than by polymorphic dispatch. This rule implements two independent
detectors, mirroring the two forms JDeodorant looks for:

1. **Repeated type-code switch** — the same enum-typed field is used as a
   `switch` selector in 2+ methods of a class. Every new enum value forces
   a change in every one of those switches.
2. **instanceof chain** — an `if` / `else if` chain with 3+ branches, each
   testing the same variable via `instanceof`. Every new subtype forces
   another branch.

Both are flagged as candidates for **Replace Conditional with
Polymorphism** or **Replace Type Code with State/Strategy**.

## Relationship to JDeodorant

> N. Tsantalis, T. Chaikalis, A. Chatzigeorgiou — "JDeodorant: Identification
> and Removal of Type-Checking Bad Smells," CSMR 2008.

JDeodorant is an Eclipse plug-in using its own detection strategy and
specification language, and it doesn't just detect the smell — it also
**ranks** refactoring opportunities by their impact on design and can
**apply** the refactoring automatically. This rule reimplements only the
detection half, built directly against SonarJava's public AST/semantic
API so it runs as an ordinary SonarQube rule in CI rather than requiring
Eclipse.

| | JDeodorant | This rule |
|---|---|---|
| Environment | Eclipse plug-in, custom spec language | SonarQube / SonarLint, standard Java rule API |
| Detects type-code switches | Yes | Yes (enum-typed fields only) |
| Detects instanceof chains | Yes | Yes |
| Ranks smells by refactoring impact | Yes | No |
| Applies the refactoring automatically | Yes | No — flags only |
| Runs unattended in CI / quality gate | No | Yes |

## Why this is a closer match than SonarQube's S134 / S3776

S134 (nesting depth) and S3776 (Cognitive Complexity) fire on *any*
deeply nested or branchy code, regardless of what the branches represent.
This rule is deliberately narrower: it only fires when the branching is
**keyed on type** — an enum field or an `instanceof` test — which is
exactly the shape Fowler's smell (and JDeodorant) targets. A flat,
non-nested switch over five enum values will trip this rule while
comfortably staying under S134's nesting threshold — the gap your earlier
research identified in S134/S3776's coverage.

## Project layout

```
src/main/java/org/sonar/samples/java/
  TypeCheckingPlugin.java                    # plugin entry point
  checks/CustomRuleRepository.java           # registers the check
  checks/TypeCheckingCheck.java              # the rule logic (both detectors)

src/main/resources/org/sonar/l10n/java/rules/java/
  TypeChecking.html                           # rule description (SonarQube UI)
  TypeChecking.json                           # rule metadata

src/test/resources/checks/TypeCheckingCheck.java   # noncompliant + compliant fixtures
src/test/java/.../TypeCheckingCheckTest.java        # CheckVerifier-based test
```

## Building and installing

1. Set `sonar.java.version` in `pom.xml` to match your SonarQube instance's
   bundled SonarJava analyzer version.
2. `mvn clean package` to produce the plugin JAR under `target/`.
3. Copy the JAR into `$SONARQUBE_HOME/extensions/plugins/` and restart
   SonarQube.
4. Activate `TypeChecking` in your Java quality profile. Tune
   `minSwitchOccurrences` (default 2) and `minInstanceofChainLength`
   (default 3) to match your codebase's tolerance.

## Known limitations

- Only **enum-typed** fields are recognized as type codes for the switch
  detector. `int`/`String` type codes (the more primitive, more smelly
  variant Fowler also describes) aren't matched — widen `fieldSelector()`
  in the check if you need that.
- The instanceof-chain detector requires all branches to test the *same*
  variable in one connected if/else-if chain; scattered, unconnected
  instanceof checks aren't correlated.
- Flags a *candidate* only — it doesn't verify the branches share a
  meaningful common abstraction, which remains a human design judgment.
- No automatic refactoring or opportunity ranking, unlike JDeodorant.

## Relevance to your complexity/test-quality correlation study

Pairing this rule with S134/S3776 gives you both a precise, semantically
targeted Conditional Complexity signal (this rule) and the looser
structural proxies you were already using — letting you check whether the
structural proxies' correlation with test quality actually holds up once
you isolate the type-dispatch cases specifically.
