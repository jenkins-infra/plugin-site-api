package io.jenkins.plugins.services.impl;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import org.apache.commons.io.FileUtils;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.jenkins.plugins.models.GithubRepoInformation;
import io.jenkins.plugins.services.ConfigurationService;

public class FetchGithubInfo extends HttpClient {
  private Logger logger = LoggerFactory.getLogger(FetchGithubInfo.class);
  private Map<String, GithubRepoInformation> repoInformationMap = new HashMap<String, GithubRepoInformation>();

  @Inject
  public FetchGithubInfo(ConfigurationService configurationService) {
    super(configurationService);
  }

  public GithubRepoInformation getInfoForRepo(String organization, String repo) {
    return repoInformationMap.get(organization + "/" + repo);
  }

  public Map<String, GithubRepoInformation> execute() throws UnsupportedEncodingException, IOException {
    final File file = new File(getClass().getClassLoader().getResource("repos.graphql").getFile());
    final String organization = "jenkinsci";
    final Map<String, GithubRepoInformation> tempRepoInformationMap = new HashMap<String, GithubRepoInformation>();

    boolean hasNextPage = true;
    String endCursor = null;
    int page = 0;

    while (hasNextPage) {
      logger.info(String.format("Fetching page %d of github repo information", page));

      HttpPost httpPost = new HttpPost(this.configurationService.getGithubApiBase() + "/graphql");

      JSONObject json = new JSONObject();
      json.put(
        "query",
        String.format(
          FileUtils.readFileToString(file, StandardCharsets.UTF_8),
          "\"" + organization.replace("\"", "\\\"") + "\"",
          endCursor == null ? "null" : "\"" + endCursor.replace("\"", "\\\"") + "\""
        )
      );
      StringEntity entity = new StringEntity(json.toString());
      httpPost.setEntity(entity);
      httpPost.setHeader("Accept", "application/json");
      httpPost.setHeader("Content-type", "application/json");

      String bodyString = this.getHttpContent(httpPost);

      JSONObject jsonResponse = new JSONObject(bodyString);
      if (jsonResponse.has("errors")) {
        throw new IOException(jsonResponse.getJSONArray("errors").toString());
      }

      if (jsonResponse.has("message") && !jsonResponse.has("data")) {
        throw new IOException(jsonResponse.getString("message"));
      }

      JSONObject repositories = jsonResponse.getJSONObject("data").getJSONObject("organization")
          .getJSONObject("repositories");

      hasNextPage = repositories.getJSONObject("pageInfo").getBoolean("hasNextPage");
      endCursor = repositories.getJSONObject("pageInfo").getString("endCursor");
      page++;

      for (Object repository : repositories.getJSONArray("edges")) {
        JSONObject node = ((JSONObject) repository).getJSONObject("node");
        if (node.optJSONObject("defaultBranchRef") == null) {
          // empty repo, so ignore everything else
          continue;
        }

        String name = node.getString("name");

        GithubRepoInformation repoInformation = new GithubRepoInformation();
        repoInformation.defaultBranch = node.getJSONObject("defaultBranchRef").getString("name");
        repoInformation.hasGithubIssuesEnabled = node.getBoolean("hasIssuesEnabled");

        tempRepoInformationMap.put(organization + "/" + name, repoInformation);
      }
    }
    synchronized (repoInformationMap) {
      repoInformationMap = tempRepoInformationMap;
    }
    logger.info("Retrieved GitHub repo data");

    return repoInformationMap;
  }
}
