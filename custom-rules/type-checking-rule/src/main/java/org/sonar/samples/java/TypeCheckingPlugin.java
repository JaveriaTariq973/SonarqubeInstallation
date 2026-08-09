package org.sonar.samples.java;

import org.sonar.api.Plugin;
import org.sonar.samples.java.checks.CustomRuleRepository;
import org.sonar.samples.java.checks.TypeCheckingRulesDefinition;

public class TypeCheckingPlugin implements Plugin {
  @Override
  public void define(Context context) {
    context.addExtension(CustomRuleRepository.class);
    context.addExtension(TypeCheckingRulesDefinition.class);
  }
}
