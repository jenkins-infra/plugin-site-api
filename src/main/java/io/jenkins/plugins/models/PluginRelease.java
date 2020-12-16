package io.jenkins.plugins.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.vladsch.flexmark.ext.emoji.EmojiExtension;
import com.vladsch.flexmark.ext.emoji.EmojiShortcutType;
import com.vladsch.flexmark.ext.gfm.issues.GfmIssuesExtension;
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.ext.gfm.tasklist.TaskListExtension;
import com.vladsch.flexmark.ext.gfm.users.GfmUsersExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.data.MutableDataSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Date;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PluginRelease {
  private Logger logger = LoggerFactory.getLogger(PluginRelease.class);

  @JsonProperty("tag_name") final private String tagName;
  @JsonProperty("name") final private String name;
  @JsonProperty("published_at") final private Date publishedAt;
  @JsonProperty("html_url") final private String htmlUrl;
  private String body;

  @JsonProperty("bodyHTML") public String getBodyHTML() {
    MutableDataSet options = new MutableDataSet();

    options.set(Parser.EXTENSIONS, Arrays.asList(
      TablesExtension.create(),
      StrikethroughExtension.create(),
      GfmUsersExtension.create(),
      TaskListExtension.create(),
      GfmIssuesExtension.create(),
      EmojiExtension.create()
    ));
    options.set(EmojiExtension.USE_SHORTCUT_TYPE, EmojiShortcutType.GITHUB);
    try {
      options.set(GfmIssuesExtension.GIT_HUB_ISSUES_URL_ROOT, new URI(this.htmlUrl + "/../../../issues").normalize().toString());
    } catch (URISyntaxException e) {
      logger.error("Unable to process html_url", e);
    }

    HtmlRenderer htmlRenderer = HtmlRenderer.builder(options).escapeHtml(true).build();
    Parser markdownParser = Parser.builder(options).build();
    return htmlRenderer.render(markdownParser.parse(this.body.replaceAll("<!--.*?-->", "")));
  }


  @JsonCreator
  public PluginRelease(@JsonProperty("tag_name") String tagName, @JsonProperty("name") String name, @JsonProperty("published_at") Date publishedAt, @JsonProperty("html_url") String htmlUrl, @JsonProperty("body") String body) {
    this.tagName = tagName;
    this.name = name;
    this.publishedAt = publishedAt;
    this.htmlUrl = htmlUrl;
    this.body = body;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof PluginRelease)) {
      return false;
    }
    return this.tagName.equals(((PluginRelease) obj).tagName) &&
      this.name.equals(((PluginRelease) obj).name) &&
      this.publishedAt.equals(((PluginRelease) obj).publishedAt) &&
      this.body.equals(((PluginRelease) obj).body);
  }
}
