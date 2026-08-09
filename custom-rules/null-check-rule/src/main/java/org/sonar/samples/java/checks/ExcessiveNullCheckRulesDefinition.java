package org.sonar.samples.java.checks;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.sonar.api.rule.Severity;
import org.sonar.api.server.rule.RulesDefinition;

public class ExcessiveNullCheckRulesDefinition implements RulesDefinition {

  public static final String REPOSITORY_KEY = CustomRuleRepository.REPOSITORY_KEY;
  public static final String REPOSITORY_NAME = "Custom Java Rules";
  private static final String RULE_KEY = "ExcessiveNullCheck";
  private static final String HTML_RESOURCE_PATH = "/org/sonar/l10n/java/rules/custom-java/ExcessiveNullCheck.html";

  @Override
  public void define(Context context) {
    NewRepository repository = context
        .createRepository(REPOSITORY_KEY, "java")
        .setName(REPOSITORY_NAME);

    repository.createRule(RULE_KEY)
        .setName("Fields whose null-checks are duplicated across many methods should be replaced with a Null Object")
        .setSeverity(Severity.MINOR)
        .setHtmlDescription(loadResource(HTML_RESOURCE_PATH))
        .setTags("design", "brain-overload");

    repository.done();
  }

  private String loadResource(String path) {
    try (InputStream is = getClass().getResourceAsStream(path)) {
      if (is == null) {
        throw new IllegalStateException("Resource not found on classpath: " + path);
      }
      return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to load resource: " + path, e);
    }
  }
}
