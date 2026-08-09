package org.sonar.samples.java.checks;

import java.util.Collections;
import java.util.List;

import org.sonar.plugins.java.api.CheckRegistrar;

/**
 * Registers this repository's custom checks with the SonarJava analyzer.
 * Wire this class into your plugin's {@code Plugin.java} the same way
 * any other custom-rules-plugin sample does.
 */
public class CustomRuleRepository implements CheckRegistrar {

  public static final String REPOSITORY_KEY = "custom-java";

  @Override
  public void register(RegistrarContext registrarContext) {
    registrarContext.registerClassesForRepository(
        REPOSITORY_KEY,
        Collections.singletonList(ExcessiveNullCheckCheck.class),
        Collections.emptyList());
  }

  public List<Class<?>> checkClasses() {
    return Collections.singletonList(ExcessiveNullCheckCheck.class);
  }
}
