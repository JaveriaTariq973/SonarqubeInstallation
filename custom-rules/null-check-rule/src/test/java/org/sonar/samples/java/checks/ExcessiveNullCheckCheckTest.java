package org.sonar.samples.java.checks;

import org.junit.jupiter.api.Test;
import org.sonar.java.checks.verifier.CheckVerifier;

class ExcessiveNullCheckCheckTest {

  @Test
  void test() {
    CheckVerifier.newVerifier()
        .onFile("src/test/resources/checks/ExcessiveNullCheckCheck.java")
        .withCheck(new ExcessiveNullCheckCheck())
        .verifyIssues();
  }
}
