package io.jenkins.plugins.services.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import static org.asciidoctor.Asciidoctor.Factory.create;

import org.apache.http.Header;
import org.asciidoctor.Asciidoctor;
import org.commonmark.node.*;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

public class GithubContentsExtractor extends GithubExtractor {
  private final static class GithubContentMatcher implements GithubMatcher {
    private final Matcher matcher;

    private GithubContentMatcher(Matcher matcher) {
      this.matcher = matcher;
    }

    @Override
    public String getEndpoint() {
      return matcher.group(3);
    }

    @Override
    public String getDirectory() {
      return "/";
    }

    @Override
    public String getBranch() {
      String branch = matcher.group(2);
      return branch == null ? "master" : branch;
    }

    @Override
    public boolean find() {
      return matcher.find();
    }

    @Override
    public String getRepo() {
      return matcher.group(1);
    }
  }

  private static final Pattern REPO_PATTERN = Pattern.compile("https?://github.com/jenkinsci/([^/.]+)/blob/([^/]+)/(.+\\.(md|adoc))$");
  private static final String API_URL_PATTERN = "https://raw.githubusercontent.com/jenkinsci/%s/%s/%s";
  private static final Asciidoctor asciidoctor = create();

  @Override
  protected GithubMatcher getDelegate(String url) {
    final Matcher matcher = REPO_PATTERN.matcher(url);
    return new GithubContentMatcher(matcher);
  }

  @Override
  public String getApiUrl(String wikiUrl) {
    GithubMatcher matcher = getDelegate(wikiUrl);

    if (!matcher.find()) {
      return null;
    }

    return String.format(API_URL_PATTERN, matcher.getRepo(), matcher.getBranch(), matcher.getEndpoint());
  }

  @Override
  public String extractHtml(String apiContent, String url, HttpClientWikiService service) {
    return super.extractHtml(this.getHTMLContent(apiContent, url), url, service);
  }

  public String getHTMLContent(String apiContent, String url) {
    if (url.toLowerCase().endsWith(".adoc")) {
      return asciidoctor.convert(apiContent,new HashMap<String, Object>());
    }
    if (url.toLowerCase().endsWith(".md")) {
      Parser parser = Parser.builder().build();
      Node document = parser.parse(apiContent);
      HtmlRenderer renderer = HtmlRenderer.builder().build();
      return renderer.render(document);
    }
    return apiContent;
  }

  @Override
  public List<Header> getHeaders() {
    return Collections.emptyList();
  }
}
