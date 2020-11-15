package io.jenkins.plugins.services.impl;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import io.jenkins.plugins.services.ConfigurationService;
import io.jenkins.plugins.services.ServiceException;
import io.jenkins.plugins.services.WikiService;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * <p>Implementation of <code>WikiService</code> powered by <code>HttpClient</code></p>
 *
 * <p>For performance reasons the content for a plugin url is cached using a <code>LoadingCache</code> for 6 hours</p>
 */
public class HttpClientWikiService extends HttpClient implements WikiService {

  private Logger logger = LoggerFactory.getLogger(HttpClientWikiService.class);

  private LoadingCache<String, String> wikiContentCache;

  public static final List<WikiExtractor> WIKI_URLS = new ArrayList<>();

  public static final String EXTERNAL_DOCUMENTATION_PREFIX = "Documentation for this plugin is here: ";

  static {
      WIKI_URLS.add(new ConfluenceApiExtractor());
      WIKI_URLS.add(new ConfluenceDirectExtractor());
      WIKI_URLS.add(new GithubReadmeExtractor());
      WIKI_URLS.add(new GithubContentsExtractor());
  }

  @Inject
  public HttpClientWikiService(ConfigurationService configurationService) {
      super(configurationService);
  }

  @PostConstruct
  public void postConstruct() {
    wikiContentCache = CacheBuilder.newBuilder()
      .expireAfterWrite(6, TimeUnit.HOURS)
      .maximumSize(1000)
      .build(new CacheLoader<String, String>() {
        @Override
        public String load(String url) throws Exception {
          // Load and clean the wiki content
          return doGetWikiContent(url);
        }
      });
  }

  public boolean isValidWikiUrl(String url) {
    return getExtractor(url).isPresent();
  }

  @Override
  public String getWikiContent(String url) throws ServiceException {
    if (StringUtils.isNotBlank(url)) {
      if (!isValidWikiUrl(url)) {
        return getNonWikiContent(url);
      }
      try {
        // This is what fires the CacheLoader that's defined in the postConstruct.
        return wikiContentCache.get(url);
      } catch (Exception e) {
        logger.error("Problem getting wiki content", e);
        return getNonWikiContent(url);
      }
    } else {
      return getNoDocumentationFound();
    }
  }

  private String doGetWikiContent(String wikiUrl) {
    for (WikiExtractor extractor: WIKI_URLS) {
      String apiUrl = extractor.getApiUrl(wikiUrl);
      if (apiUrl != null) {
        HttpRequestBase httpRequest = new HttpGet(apiUrl);
        extractor.getHeaders().stream().forEach(httpRequest::setHeader);
        httpRequest.setHeader("User-Agent", "jenkins-wiki-exporter/actually-plugin-site-api");
        String content = getHttpContent(httpRequest);
        if (content == null) {
          return null; // error logged in getHttpContent
        }
        return extractor.extractHtml(content, wikiUrl, this);
      }
    }
    return null;
  }

  private Optional<WikiExtractor> getExtractor(String url) {
    return WIKI_URLS.stream().filter(t -> (t.getApiUrl(url) != null)).findFirst();
  }

  /**
   * @param element element to be processed
   * @param attributeName attribute name
   * @param host part of URL including protocol and host, no trailing slash
   * @param path path to parent folder, including initial and trailing slash
   */
  public void replaceAttribute(Element element, String attributeName, String host, String path) {
    final String attribute = element.attr(attributeName);
    if (attribute.startsWith("/")) {
      element.attr(attributeName, host + attribute);
    } else if (!attribute.startsWith("http:") && !attribute.startsWith("https:")
        && !attribute.startsWith("#")) {
      element.attr(attributeName, host + path + attribute);
    }
  }

  /**
   * GitHub adds user-content- to some html elements like links and headings, which breaks hyperlinking in ToCs
   *
   * @param element element to be processed
   */
  public void stripUserContentIdPrefix(Element element) {
    final String attribute = element.attr("id");
    element.attr("id", attribute.replace("user-content-", ""));
  }

  public static String getNonWikiContent(String url) {
    final Element body = Jsoup.parseBodyFragment("<div></div>").body();
    final Element div = body.select("div").first();
    div.text(EXTERNAL_DOCUMENTATION_PREFIX);
    final Element link = div.appendElement("a");
    link.text(url);
    link.attr("href", url);
    return body.html();
  }

  public static String getNoDocumentationFound() {
    final Element body = Jsoup.parseBodyFragment("<div></div>").body();
    final Element div = body.select("div").first();
    div.text("No documentation for this plugin could be found");
    return body.html();
  }

  public Element getElementByClassFromText(String className, String content) {
    if (content == null || content.trim().isEmpty()) {
      logger.warn("Can't clean null content");
      return null;
    }
    final Document html = Jsoup.parse(content);
    final Elements elements = html.getElementsByClass(className);
    if (elements.isEmpty()) {
      logger.warn("wiki-content not found in content");
      return null;
    }
    return elements.first();
  }

  /**
   * @param wikiContent top level element to be traversed
   * @param host part of URL including protocol and host, no trailing slash
   * @param path path to parent folder, including initial and trailing slash
   */
  public void convertLinksToAbsolute(Element wikiContent, String host, String path) {
    wikiContent.getElementsByAttribute("href").forEach(element -> replaceAttribute(element, "href", host, path));
    wikiContent.getElementsByAttribute("src").forEach(element -> replaceAttribute(element, "src", host, path));
  }

}
