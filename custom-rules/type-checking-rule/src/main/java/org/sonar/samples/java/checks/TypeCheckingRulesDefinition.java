package org.sonar.samples.java.checks;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.sonar.api.rule.Severity;
import org.sonar.api.server.rule.RulesDefinition;

public class TypeCheckingRulesDefinition implements RulesDefinition {

  public static final String REPOSITORY_KEY = CustomRuleRepository.REPOSITORY_KEY;
  public static final String REPOSITORY_NAME = "Custom Java Rules";
  private static final String RULE_KEY = "TypeChecking";
  private static final String HTML_RESOURCE_PATH = "/org/sonar/l10n/java/rules/custom-java/TypeChecking.html";

  @Override
  public void define(Context context) {
    NewRepository repository = context
        .createRepository(REPOSITORY_KEY, "java")
        .setName(REPOSITORY_NAME);

    repository.createRule(RULE_KEY)
        .setName("Classes should not dispatch behavior by repeatedly switching or testing on type")
        .setSeverity(Severity.MAJOR)
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
