package io.jenkins.plugins.generate.parsers;

import io.jenkins.plugins.models.Scm;
import io.jenkins.plugins.models.Plugin;
import io.jenkins.plugins.models.GithubRepoInformation;
import io.jenkins.plugins.generate.PluginDataParser;
import io.jenkins.plugins.models.Plugin;
import io.jenkins.plugins.services.ConfigurationService;
import io.jenkins.plugins.services.impl.FetchGithubInfo;
import io.jenkins.plugins.services.impl.HttpClientJiraIssues;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.net.MalformedURLException;
import java.io.UnsupportedEncodingException;

public class GithubDataParser implements PluginDataParser {
  private final ConfigurationService configurationService;
  private final FetchGithubInfo fetchGithubInfo;

  private static final Logger logger = LoggerFactory.getLogger(GithubDataParser.class);

  public GithubDataParser(final ConfigurationService configurationService) {
    this.configurationService = configurationService;
    this.fetchGithubInfo = new FetchGithubInfo(configurationService);
    try {
      this.fetchGithubInfo.execute();
    } catch (Exception e) {
      logger.error("Error fetching github information", e);
      throw new RuntimeException(e);
    }
  }

  public final URL safeParse(String url) {
    try {
      URL parsed = new URL(url);
      return parsed;
    } catch (MalformedURLException e) {
      return null;
    }
  }

  public final String safeUrlEncode(String word) {
    try {
      return URLEncoder.encode(word, StandardCharsets.UTF_8.toString());
    } catch (UnsupportedEncodingException e) {
      logger.error("safeUrlEncode error", e);
      return word;
    }
  }

  @Override
  public void parse(JSONObject pluginJson, Plugin plugin) {
    Scm scm = plugin.getScm();
    if (scm == null) {
      return;
    }

    URL parsed = safeParse(scm.getLink());
    if (parsed == null) {
      logger.error("Error writing scm data for " + plugin.getName() + ": " + scm.getLink());
      return;
    }

    String githubOrganization = parsed.getPath().split("/")[1];
    String githubRepo = parsed.getPath().split("/")[2];
    GithubRepoInformation repoInfo = fetchGithubInfo.getInfoForRepo(githubOrganization, githubRepo);
    if (repoInfo != null) {
      if (StringUtils.trimToNull(scm.getIssues()) != null) {
        if (repoInfo.hasGithubIssuesEnabled) {
          plugin.setIssuesUrl("https://github.com/" + githubOrganization + "/" + githubRepo + "/issues/");
        } else {
          plugin.setIssuesUrl(
            configurationService.getJiraURL() +
            "/issues/?jql=project%3DJENKINS%20AND%20component%3D" +
            safeUrlEncode(HttpClientJiraIssues.pluginNameToJiraComponent(plugin.getName()))
          );
        }
      }
      plugin.setDefaultBranch(repoInfo.defaultBranch);
    }
  }
}
