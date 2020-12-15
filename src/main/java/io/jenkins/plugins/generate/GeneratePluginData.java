package io.jenkins.plugins.generate;

import io.jenkins.plugins.commons.JsonObjectMapper;
import io.jenkins.plugins.generate.parsers.*;
import io.jenkins.plugins.models.*;
import io.jenkins.plugins.services.ConfigurationService;
import io.jenkins.plugins.services.impl.DefaultConfigurationService;
import io.jenkins.plugins.services.impl.FetchGithubInfo;
import io.jenkins.plugins.services.impl.HttpClientJiraIssues;
import io.jenkins.plugins.utils.VersionUtils;

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.common.Strings;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;

/**
 * <p>Responsible for generating plugin data that is indexed inside Elasticsearch.</p>
 *
 * <p>Most of the work happens inside each <code>PluginDataParser</code></p>
 */
public class GeneratePluginData {

  private static final Logger logger = LoggerFactory.getLogger(GeneratePluginData.class);

  private static final String UPDATE_CENTER_JSON = "https://updates.jenkins.io/current/update-center.actual.json";

  public static void main(String[] args) {
    final GeneratePluginData generatePluginData = new GeneratePluginData();
    generatePluginData.generate();
  }

  public void generate() {
    final ConfigurationService configurationService = new DefaultConfigurationService();
    final JSONObject updateCenterJson = getUpdateCenterJson();
    final FetchGithubInfo fetchGithubInfo = getGithubInformation(configurationService);
    final List<PluginDataParser> parsers = Arrays.asList(
      new RootPluginDataParser(),
      new LabelsPluginDataParser(),
      new CategoriesPluginDataParser(),
      new DependenciesPluginDataParser(updateCenterJson),
      new ImpliedDependenciesCoreResourceParser(updateCenterJson),
      new FirstReleasePluginDataParser(),
      new MaintainersPluginDataParser(),
      new ScmPluginDataParser(),
      new SecurityWarningsPluginDataParser(updateCenterJson),
      new StatsPluginDataParser(),
      new WikiPluginDataParser()
    );
    final JSONObject pluginsJson = updateCenterJson.getJSONObject("plugins");
    final List<Plugin> plugins = pluginsJson.keySet().stream()
      .map(pluginsJson::getJSONObject)
      .map(pluginJson -> {
        final Plugin plugin = new Plugin();
        parsers.forEach(parser -> parser.parse(pluginJson, plugin));

        Scm scm = plugin.getScm();
        if (scm != null) {
          try {
            URL parsed = new URL(scm.getLink());
            String githubOrganization = parsed.getPath().split("/")[1];
            String githubRepo = parsed.getPath().split("/")[2];
            GithubRepoInformation repoInfo = fetchGithubInfo.getInfoForRepo(githubOrganization, githubRepo);
            if (repoInfo != null) {
              if (Strings.isNullOrEmpty(scm.getIssues())) {
                if (repoInfo.hasGithubIssuesEnabled) {
                  plugin.setIssuesUrl("https://github.com/" + githubOrganization + "/" + githubRepo + "/issues/");
                } else {
                  plugin.setIssuesUrl(
                    configurationService.getJiraURL() + "/issues/?jql=project%3DJENKINS%20AND%20component%3D" + URLEncoder.encode(
                      HttpClientJiraIssues.pluginNameToJiraComponent(plugin.getName()),
                      StandardCharsets.UTF_8.toString()
                    )
                  );
                }
              }
              plugin.setDefaultBranch(repoInfo.defaultBranch);
            }
          } catch (MalformedURLException | UnsupportedEncodingException e){
            logger.error("Error writing scm deata", e);
            return null;
          }


        }
        return plugin;
      })
      .collect(Collectors.toList());
    writePluginsToFile(plugins);
  }

  private JSONObject getUpdateCenterJson() {
    final ResponseHandler<JSONObject> handler = httpResponse -> {
      final StatusLine status = httpResponse.getStatusLine();
      if (status.getStatusCode() == 200) {
        final HttpEntity entity = httpResponse.getEntity();
        final String content = EntityUtils.toString(entity, StandardCharsets.UTF_8);
        try {
          return new JSONObject(content);
        } catch (Exception e) {
          logger.error("Update center returned invalid JSON", e);
          throw new ClientProtocolException("Update center returned invalid JSON");
        }
      } else {
        throw new ClientProtocolException(String.format("Unexpected response from update center - %s", status.toString()));
      }
    };
    logger.info("Begin downloading from update center");
    try (final CloseableHttpClient httpClient = HttpClients.createDefault()) {
      return httpClient.execute(new HttpGet(UPDATE_CENTER_JSON), handler);
    } catch (Exception e) {
      logger.error("Problem communicating with update center", e);
      throw new RuntimeException("Problem communicating with update center", e);
    }
  }

  private void writePluginsToFile(final List<Plugin> plugins) {
    final File data = Paths.get(System.getProperty("user.dir"), "target", "plugins.json.gzip").toFile();
    try(final Writer writer = new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(data)), StandardCharsets.UTF_8))) {
      final String mappingVersion = VersionUtils.getMappingVersion();
      final String elasticsearchVersion = VersionUtils.getElasticsearchVersion();
      JsonObjectMapper.getObjectMapper().writeValue(writer, new GeneratedPluginData(plugins, mappingVersion, elasticsearchVersion));
    } catch (Exception e) {
      logger.error("Problem writing plugin data to file", e);
      throw new RuntimeException(e);
    }
    logger.info("Wrote to " + data.getAbsolutePath());
  }

  private FetchGithubInfo getGithubInformation(final ConfigurationService configurationService) {
    final FetchGithubInfo fetchGithubInfo = new FetchGithubInfo(configurationService);
    try {
      fetchGithubInfo.execute();
    } catch (Exception e) {
      logger.error("Error fetching github information", e);
      throw new RuntimeException(e);
    }
    return fetchGithubInfo;
  }
}
