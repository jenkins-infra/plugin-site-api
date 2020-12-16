package io.jenkins.plugins.services.impl;

import io.jenkins.plugins.commons.JsonObjectMapper;
import io.jenkins.plugins.commons.JwtHelper;
import io.jenkins.plugins.models.GeneratedPluginData;
import io.jenkins.plugins.services.ConfigurationService;
import io.jenkins.plugins.services.ServiceException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.kohsuke.github.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

import static io.jenkins.plugins.commons.JwtHelper.createJWT;

/**
 * <p>Default implementation of <code>ConfigurationService</code></p>
 */
public class DefaultConfigurationService implements ConfigurationService {

  private final Logger logger = LoggerFactory.getLogger(DefaultConfigurationService.class);

  private enum ModifyType {
    ETAG,
    LAST_MODIFIED,
    NONE
  }

  private ModifyType modifyType;
  private String modifyValue;

  private transient String cachedToken;
  private transient long tokenCacheTime;

  public DefaultConfigurationService() {
    this.modifyType = null;
    this.modifyValue = null;
  }

  @Override
  public GeneratedPluginData getIndexData() throws ServiceException {
    String url = getDataFileUrl();
    if (url.startsWith("file:///")) {
      String path = url.substring(7);
      final File dataFile = new File(path);
      final String data = readGzipFile(dataFile);
      try {
        final GeneratedPluginData generated = JsonObjectMapper.getObjectMapper().readValue(data, GeneratedPluginData.class);
        modifyType = ModifyType.NONE;
        modifyValue = null;
        return generated;
      } catch (Exception ex) {
        logger.error("Problem getting data file", ex);
        throw new ServiceException("Problem getting data file", ex);
      }
    }


    final CloseableHttpClient httpClient = HttpClients.createDefault();
    try {

      if (!hasPluginDataChanged(httpClient, url)) {
        logger.info("Plugin data file hasn't changed");
        return null;
      }
      final HttpGet get = new HttpGet(url);
      final CloseableHttpResponse response = httpClient.execute(get);
      if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
        final HttpEntity entity = response.getEntity();
        final InputStream inputStream = entity.getContent();
        final File dataFile = File.createTempFile("plugins", ".json.gzip");
        FileUtils.copyToFile(inputStream, dataFile);
        final String data = readGzipFile(dataFile);
        final GeneratedPluginData generated = JsonObjectMapper.getObjectMapper().readValue(data, GeneratedPluginData.class);
        if (response.containsHeader("ETag")) {
          modifyType = ModifyType.ETAG;
          modifyValue = response.getLastHeader("ETag").getValue();
          logger.info(String.format("Using ETag [%s]", modifyValue));
        } else if (response.containsHeader("Last-Modified")) {
          modifyType = ModifyType.LAST_MODIFIED;
          modifyValue = response.getLastHeader("Last-Modified").getValue();
          logger.info(String.format("Using Last-Modified [%s]", modifyValue));
        } else {
          modifyType = ModifyType.NONE;
          modifyValue = null;
          logger.info("ETag and Last-Modified are not supported by the server");
        }
        return generated;
      } else {
        logger.error("Data file not found");
        throw new RuntimeException("Data file not found");
      }
    } catch (Exception e) {
      logger.error("Problem getting data file", e);
      throw new ServiceException("Problem getting data file", e);
    } finally {
      try {
        httpClient.close();
      } catch (IOException e) {
        logger.warn("Problem closing HttpClient", e);
      }
    }
  }

  public String getJiraURL() {
    return "https://issues.jenkins.io";
  }

  protected String getJiraUsername() {
    return StringUtils.trimToNull(System.getenv("JIRA_USERNAME"));
  }

  protected String getJiraPassword() {
    return StringUtils.trimToNull(System.getenv("JIRA_PASSWORD"));
  }

  public List<Header> getJiraCredentials() {
    String encoding = Base64.getEncoder().encodeToString((this.getJiraUsername() + ":" + this.getJiraPassword()).getBytes());
    return Collections.singletonList(new BasicHeader(HttpHeaders.AUTHORIZATION, "Basic " + encoding));
  }

  // borrowed from https://github.com/jenkinsci/github-branch-source-plugin/blob/07b69ab4f9d9ed718416ac63af67102de5e172fa/src/main/java/org/jenkinsci/plugins/github_branch_source/GitHubAppCredentials.java#L107
  @SuppressWarnings("deprecation") // preview features are required for GitHub app integration, GitHub api adds deprecated to all preview methods
  String generateAppInstallationToken(String appId, String appPrivateKey) {
    try {
      String jwtToken = createJWT(appId, appPrivateKey);
      GitHub gitHubApp = new GitHubBuilder().withEndpoint(this.getGithubApiBase())
        .withJwtToken(jwtToken)
        .build();

      GHApp app = gitHubApp.getApp();

      List<GHAppInstallation> appInstallations = app.listInstallations().asList();
      if (appInstallations.isEmpty()) {
        throw new IllegalArgumentException(String.format("Couldn't authenticate with GitHub app ID %s", appId));
      }
      GHAppInstallation appInstallation = appInstallations.stream()
          .findAny()
          .orElseThrow(() -> new IllegalArgumentException(String.format("Couldn't authenticate with GitHub app ID %s", appId)));

      GHAppInstallationToken appInstallationToken = appInstallation
        .createToken(appInstallation.getPermissions())
        .create();

      return appInstallationToken.getToken();
    } catch (IOException e) {
      throw new IllegalArgumentException(String.format("Couldn't authenticate with GitHub app ID %s", appId), e);
    }
  }

  public String getGithubAppId() {
    String appId = StringUtils.trimToNull(System.getenv("GITHUB_APP_ID"));
    if (appId != null) {
      return appId;
    }
    return StringUtils.trimToNull(System.getProperty("github.app.id"));
  }

  public String getGithubToken() {
    String token = StringUtils.trimToNull(System.getenv("GITHUB_TOKEN"));
    if (token != null) {
      return token;
    }
    return StringUtils.trimToNull(System.getProperty("github.token"));
  }


  public String getGithubAppPrivateKey() {
    String filePath = StringUtils.trimToNull(System.getenv("GITHUB_APP_PRIVATE_KEY"));
    if (filePath == null) {
      filePath = StringUtils.trimToNull(System.getProperty("github.app.private_key"));
    }
    if (filePath != null) {
      try {
        return new String(Files.readAllBytes(Paths.get(filePath)), StandardCharsets.UTF_8);
      } catch (IOException e) {
        logger.error("Unable to read private key", e);
      }
    }
    return null;
  }

  public List<Header> getGithubCredentials() {
    if (this.getGithubAppId() != null) {
      long now = System.currentTimeMillis();
      String appInstallationToken;
      if (cachedToken != null && now - tokenCacheTime < JwtHelper.VALIDITY_MS /* extra buffer */ / 2) {
        appInstallationToken = cachedToken;
      } else {
        appInstallationToken = generateAppInstallationToken(this.getGithubAppId(), this.getGithubAppPrivateKey());
        cachedToken = appInstallationToken;
        tokenCacheTime = now;
      }
      return Collections.singletonList(new BasicHeader(HttpHeaders.AUTHORIZATION, "Bearer " + appInstallationToken));
    }
    if (this.getGithubToken() != null) {
      return Collections.singletonList(new BasicHeader(HttpHeaders.AUTHORIZATION, "Bearer " + this.getGithubToken()));
    }

    logger.warn("No GitHub Client ID specified, using anonymous credentials to access github api");
    return Collections.emptyList();
  }

  private String getDataFileUrl() {
    if (System.getenv().containsKey("DATA_FILE_URL")) {
      final String url = StringUtils.trimToNull(System.getenv("DATA_FILE_URL"));
      if (url == null) {
        throw new RuntimeException("Environment variable 'DATA_FILE_URL' is empty");
      }
      return url;
    } else {
      final String url = StringUtils.trimToNull(System.getProperty("data.file.url"));
      if (url == null) {
        throw new RuntimeException("System property 'data.file.url' is not given");
      }
      return url;
    }
  }

  private String readGzipFile(final File file) {
    try(final BufferedReader reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(file)), StandardCharsets.UTF_8))) {
      return reader.lines().collect(Collectors.joining());
    } catch (Exception e) {
      logger.error("Problem decompressing plugin data", e);
      throw new RuntimeException("Problem decompressing plugin data", e);
    }
  }

  private boolean hasPluginDataChanged(CloseableHttpClient httpClient, String url) {
    if (modifyType == null || modifyType == ModifyType.NONE) {
      return true;
    }
    final HttpHead head = new HttpHead(url);
    switch (modifyType) {
      case ETAG:
        logger.info(String.format("Using ETag [%s]", modifyValue));
        head.addHeader("If-None-Match", modifyValue);
        break;
      case LAST_MODIFIED:
        logger.info(String.format("Using Last-Modified [%s]", modifyValue));
        head.addHeader("If-Modified-Since", modifyValue);
        break;
    }
    try {
      final CloseableHttpResponse response = httpClient.execute(head);
      return response.getStatusLine().getStatusCode() != HttpStatus.SC_NOT_MODIFIED;
    } catch (Exception e) {
      logger.error("Problem determining if plugin data file changed", e);
      throw new ServiceException("Problem determining if plugin data file changed", e);
    }
  }

  @Override
  public String getGithubApiBase() {
    return "https://api.github.com";
  }
}
