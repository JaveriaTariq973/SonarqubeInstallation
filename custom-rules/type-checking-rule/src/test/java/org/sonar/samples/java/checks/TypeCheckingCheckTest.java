package org.sonar.samples.java.checks;

import org.junit.jupiter.api.Test;
import org.sonar.java.checks.verifier.CheckVerifier;

class TypeCheckingCheckTest {

  @Test
  void test() {
    CheckVerifier.newVerifier()
        .onFile("src/test/resources/checks/TypeCheckingCheck.java")
        .withCheck(new TypeCheckingCheck())
        .verifyIssues();
  }
}
