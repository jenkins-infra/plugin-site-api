package io.jenkins.plugins.services.impl;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.message.BasicHeader;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public abstract class GithubExtractor implements WikiExtractor {
  /**
   * Bootstrap class setting !important padding. Scrapped by extractor
   * to avoid !important override.
   */
  public static final String BOOTSTRAP_PADDING_5 = "p-5";

  private static final String API_URL_PATTERN = "https://api.github.com/repos/jenkinsci/%s/%s?ref=%s";

  @Override
  public String getApiUrl(String wikiUrl) {
    GithubMatcher matcher = getDelegate(wikiUrl);
    if (!matcher.find()) {
      return null;
    }

    return String.format(API_URL_PATTERN, matcher.getRepo(), matcher.getEndpoint(), matcher.getBranch());
  }

  @Override
  public String extractHtml(String apiContent, String url, HttpClientWikiService service) {
    GithubMatcher matcher = getDelegate(url);
    if (!matcher.find()) {
      throw new IllegalArgumentException("Invalid github URL" + url);
    }
    final Document html = Jsoup.parse(apiContent);
    final Element mainDiv = html.getElementsByTag("body").get(0).child(0);
    //TODO(oleg_nenashev): Support organization and branch customization?
    convertLinksToAbsolute(service, mainDiv, "jenkinsci", matcher);
    return mainDiv.toString();
  }

  @Override
  public List<Header> getHeaders() {
    Header header = new BasicHeader("Accept", "application/vnd.github.v3.html");
    return Collections.singletonList(header);
  }

  protected abstract GithubMatcher getDelegate(String url);

  protected void convertLinksToAbsolute(HttpClientWikiService service, Element wikiContent, String orgName, GithubMatcher matcher) {
    String repoName = matcher.getRepo();
    String branch = matcher.getBranch();
    String path = matcher.getDirectory();
    String documentationHost = String.format("https://github.com/%s/%s/blob/%s", orgName, repoName, branch);
    String imageHost = String.format("https://cdn.jsdelivr.net/gh/%s/%s@%s", orgName, repoName, branch);
    Elements topLevelHeading = wikiContent.getElementsByTag("H1");
    if (topLevelHeading.size() == 1) {
      topLevelHeading.get(0).remove();
    }

    wikiContent.select("h1, h2, h3, h4, h5, h6")
      .stream()
      .filter(element -> StringUtils.contains(element.id(), "user-content"))
      .forEach(service::stripUserContentIdPrefix);

    wikiContent.getElementsByClass(BOOTSTRAP_PADDING_5).forEach(element -> element.removeClass(BOOTSTRAP_PADDING_5));
    // Relative hyperlinks, we resolve "/docs/rest-api.adoc" as https://github.com/jenkinsci/folder-auth-plugin/blob/master/docs/rest-api.adoc
    wikiContent.getElementsByAttribute("href").forEach(element -> service.replaceAttribute(element, "href", documentationHost, path));

    // Relative image inclusions, we resolve /docs/images/screenshot.png as https://cdn.jsdelivr.net/gh/jenkinsci/folder-auth-plugin@master/docs/images/screenshot.png
    wikiContent.getElementsByAttribute("src").forEach(element -> service.replaceAttribute(element, "src", imageHost, path));
  }

}
