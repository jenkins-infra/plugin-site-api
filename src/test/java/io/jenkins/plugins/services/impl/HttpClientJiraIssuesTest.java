package io.jenkins.plugins.services.impl;

import com.google.common.base.Strings;
import io.jenkins.plugins.models.JiraIssues;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;

public class HttpClientJiraIssuesTest {
  @Rule
  public PluginSiteApiWireMockRule wireMockRule = new PluginSiteApiWireMockRule(HttpClientJiraIssuesTest.class, Arrays.asList(
    "https://issues.jenkins-ci.org"
  ));

  private HttpClientJiraIssues httpClientJiraIssues;

  @Before
  public void setUp() {

    this.httpClientJiraIssues = new HttpClientJiraIssues(new DefaultConfigurationService() {
      @Override
      protected String getJiraUsername() {
        String username = super.getJiraUsername();
        if (Strings.isNullOrEmpty(username)) {
          return "username";
        }
        return username;
      }

      @Override
      protected String getJiraPassword() {
        String password = super.getJiraPassword();
        if (Strings.isNullOrEmpty(password)) {
          return "password";
        }
        return password;
      }

      @Override
      public String getJiraURL() {
        return wireMockRule.baseUrl();
      }
    });
  }

  @Test
  public void testHappyPath() throws IOException {
    JiraIssues issues = this.httpClientJiraIssues.getIssues("git");

    assertEquals(512, issues.getTotal());
    assertEquals(512, issues.issues.size());
  }

  @Test
  public void testComponentNotFoundName() throws IOException {
    JiraIssues issues = this.httpClientJiraIssues.getIssues("cors-filter-plugin");

    assertEquals(0, issues.getTotal());
    assertEquals(0, issues.issues.size());
  }

  @Test
  public void testCapitalComponentName() throws IOException {
    String componentName = this.httpClientJiraIssues.getJiraComponentName("MixedCasePlugin");

    assertEquals("mixedcaseplugin-plugin", componentName);
  }
}
