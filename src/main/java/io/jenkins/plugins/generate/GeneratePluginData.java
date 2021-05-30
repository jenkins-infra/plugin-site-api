package io.jenkins.plugins.generate;

import io.jenkins.plugins.commons.JsonObjectMapper;
import io.jenkins.plugins.generate.parsers.*;
import io.jenkins.plugins.models.*;
import io.jenkins.plugins.utils.VersionUtils;
import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
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
    final JSONObject updateCenterJson = getUpdateCenterJson();
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
        return plugin;
      })
      .collect(Collectors.toList());
      plugins.forEach(p -> computeTrend(p, plugins));
    writePluginsToFile(plugins);
  }

  private void computeTrend(Plugin plugin, List<Plugin> plugins) {
    double dependentInstallPctLastMonth = plugins.stream().filter(p -> isDependency(p, plugin))
      .map(p -> getInstallPct(p, 1))
      .max(Double::compare).orElse(0.0);
    double installPctLastMonth = getInstallPct(plugin, 1);
    double installPctMonthBefore = getInstallPct(plugin, 2);
    double independence = Math.max(0, 1 - dependentInstallPctLastMonth / installPctLastMonth);
    int trend =  (int) ((installPctLastMonth - installPctMonthBefore) * independence * 1E4);
    plugin.getStats().setTrend(trend);
  }

  private double getInstallPct(Plugin p, int monthsAgo) {
    List<InstallationPercentage> stats = p.getStats().getInstallationsPercentage();
    if (stats != null && stats.size() >= monthsAgo) {
      return stats.get(stats.size() - monthsAgo).getPercentage();
    }
    return 0;
  }

  private boolean isDependency(Plugin p, Plugin plugin) {
    return p.getDependencies().stream().anyMatch(d -> d.getName().equals(plugin.getName()));
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

  private void writePluginsToFile(List<Plugin> plugins) {
    final File data = Paths.get(System.getProperty("user.dir"), "target", "plugins.json.gzip").toFile();
    try(final Writer writer = new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(data)), StandardCharsets.UTF_8))) {
      final String mappingVersion = VersionUtils.getMappingVersion();
      final String elasticsearchVersion = VersionUtils.getElasticsearchVersion();
      JsonObjectMapper.getObjectMapper().writeValue(writer, new GeneratedPluginData(plugins, mappingVersion, elasticsearchVersion));
    } catch (Exception e) {
      logger.error("Problem writing plugin data to file", e);
      throw new RuntimeException(e);
    }
  }

}
