package io.jenkins.plugins.services.impl;

import com.google.common.base.Strings;
import io.jenkins.plugins.models.JiraIssue;
import io.jenkins.plugins.models.JiraIssues;
import io.jenkins.plugins.models.MissingJiraComponentException;
import io.jenkins.plugins.services.ConfigurationService;
import io.sentry.Sentry;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;

public class HttpClientJiraIssues extends HttpClient {
  private Logger logger = LoggerFactory.getLogger(HttpClientJiraIssues.class);

  @Inject
  public HttpClientJiraIssues(ConfigurationService configurationService) {
    super(configurationService);
  }

  @Override
  public String getHttpContent(final HttpRequestBase httpRequest) {
    try {
      httpRequest.setURI(new URI(this.configurationService.getJiraURL() + httpRequest.getURI().getRawPath()));
    } catch (URISyntaxException e) {
      logger.error("Unable to set url", e);
    }
    return super.getHttpContent(httpRequest);
  }

  @Override
  protected boolean isValidStatusCode(int statusCode) {
    return statusCode == HttpStatus.SC_OK || statusCode == HttpStatus.SC_BAD_REQUEST;
  }

  public JiraIssues getIssues(String pluginName) throws IOException {
    return getIssues(pluginName, 0);
  }

  public JiraIssues getIssues(String pluginName, int startAt) throws IOException {
    int maxResults = 100;
    Sentry.getContext().addExtra("plugin-name", pluginName);
    String component = pluginName.replaceAll("-plugin$", "") + "-plugin";
    JiraIssues jiraIssues = new JiraIssues();

    String query = URLEncoder.encode("project=JENKINS AND status in (Open, \"In Progress\", Reopened) AND component=" + component, "UTF-8");
    String url = "/rest/api/2/search?startAt=" + startAt + "&maxResults=" + maxResults + "&jql=" + query;
    String jsonInput = getHttpContent(new HttpGet(url));
    if (Strings.isNullOrEmpty(jsonInput)) {
      String msg = "[" + pluginName + "] Empty return value for " + url;
      logger.debug(msg);
      Sentry.capture(new Error(msg));
      return jiraIssues;
    }

    JSONObject obj = new JSONObject(jsonInput);
    if (obj.has("errorMessages")) {
      logger.warn("[" + pluginName + "] JSON Response with error: " + jsonInput);
      if (obj.getJSONArray("errorMessages").join("|").contains("\"The value '" + component + "' does not exist for the field 'component'.\"")) {
        Sentry.capture(new MissingJiraComponentException(pluginName, component));
      } else {
        Sentry.capture(new Error(obj.getJSONArray("errorMessages").join("|")));
      }

      return jiraIssues;
    }

    JSONArray jsonIssues = obj.getJSONArray("issues");
    for (Object issue : jsonIssues) {
      JSONObject jsonIssue = (JSONObject) issue;

      JiraIssue jiraIssue = new JiraIssue();
      jiraIssue.key = jsonIssue.getString("key");

      JSONObject fields = jsonIssue.getJSONObject("fields");
      jiraIssue.created = fields.optString("created");
      jiraIssue.updated = fields.optString("updated");
      jiraIssue.summary = fields.optString("summary");

      JSONObject issuetype = fields.optJSONObject("issuetype");
      JSONObject priority = fields.optJSONObject("priority");
      JSONObject status = fields.optJSONObject("status");
      JSONObject reporter = fields.optJSONObject("reporter");
      JSONObject assignee = fields.optJSONObject("assignee");
      JSONObject resolution = fields.optJSONObject("resolution");

      jiraIssue.issueType = issuetype != null ? issuetype.optString("name") : null;
      jiraIssue.priority =  priority != null ? priority.optString("name") : null;
      jiraIssue.status =  status != null ? status.optString("status") : null;
      jiraIssue.resolution = resolution != null ? resolution.optString("name") : null;
      jiraIssue.reporter = reporter != null ? reporter.optString("displayName") : null;
      jiraIssue.assignee = assignee != null ? assignee.optString("displayName") : null;

      jiraIssues.issues.add(jiraIssue);

    }
    if (obj.getInt("startAt") + jsonIssues.length() < obj.getInt("total")) {
      jiraIssues.issues.addAll(getIssues(pluginName, startAt + maxResults).issues);
    }

    return jiraIssues;
  }
}
