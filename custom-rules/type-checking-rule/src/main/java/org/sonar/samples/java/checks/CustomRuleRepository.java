package org.sonar.samples.java.checks;

import java.util.Collections;

import org.sonar.plugins.java.api.CheckRegistrar;

/**
 * Registers this repository's custom checks with the SonarJava analyzer.
 */
public class CustomRuleRepository implements CheckRegistrar {

  public static final String REPOSITORY_KEY = "custom-java";

  @Override
  public void register(RegistrarContext registrarContext) {
    registrarContext.registerClassesForRepository(
        REPOSITORY_KEY,
        Collections.singletonList(TypeCheckingCheck.class),
        Collections.emptyList());
  }
}
