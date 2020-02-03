package io.jenkins.plugins.generate.parsers;

import io.jenkins.plugins.generate.PluginDataParser;
import io.jenkins.plugins.models.IssueTracker;
import io.jenkins.plugins.models.Plugin;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class RootPluginDataParser implements PluginDataParser {

  private static final DateTimeFormatter BUILD_DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.US);

  // java.time DateTimeFormatter.ISO_LOCAL_DATE_TIME uses nano-of-second where we're using milliseconds
  private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SS'Z'", Locale.US);

  @Override
  public void parse(JSONObject pluginJson, Plugin plugin) {
    plugin.setExcerpt(pluginJson.optString("excerpt", null));
    plugin.setGav(pluginJson.optString("gav", null));
    plugin.setName(pluginJson.getString("name"));
    plugin.setPreviousVersion(pluginJson.optString("previousVersion", null));
    plugin.setRequiredCore(pluginJson.optString("requiredCore"));
    plugin.setSha1(pluginJson.optString("sha1", null));
    plugin.setTitle(pluginJson.optString("title", null));
    plugin.setUrl(pluginJson.optString("url", null));
    JSONObject issueTracker = (JSONObject) pluginJson.opt("issueTracker");
    if (issueTracker != null) {
      plugin.setIssueTracker(new IssueTracker(issueTracker.getString("name"), issueTracker.getString("url")));
      plugin.setVersion(pluginJson.optString("version", null));
    }
    if (StringUtils.isNotBlank(pluginJson.optString("buildDate", null))) {
      final LocalDate buildDate = LocalDate.parse(pluginJson.getString("buildDate"), BUILD_DATE_FORMATTER);
      plugin.setBuildDate(buildDate);
    }
    if (StringUtils.isNotBlank(pluginJson.optString("previousTimestamp", null))) {
      final LocalDateTime previousTimestamp = LocalDateTime.parse(pluginJson.getString("previousTimestamp"), TIMESTAMP_FORMATTER);
      plugin.setPreviousTimestamp(previousTimestamp);
    }
    if (StringUtils.isNotBlank(pluginJson.optString("releaseTimestamp", null))) {
      final LocalDateTime releaseTimestamp = LocalDateTime.parse(pluginJson.getString("releaseTimestamp"), TIMESTAMP_FORMATTER);
      plugin.setReleaseTimestamp(releaseTimestamp);
    }
  }
}
