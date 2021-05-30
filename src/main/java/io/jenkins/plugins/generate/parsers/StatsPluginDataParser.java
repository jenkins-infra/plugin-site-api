package io.jenkins.plugins.generate.parsers;

import io.jenkins.plugins.generate.PluginDataParser;
import io.jenkins.plugins.models.*;
import org.eclipse.jgit.api.Git;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Collectors;

public class StatsPluginDataParser implements PluginDataParser {

  private static final Logger logger = LoggerFactory.getLogger(StatsPluginDataParser.class);

  private final Path statisticsPath;

  public StatsPluginDataParser() {
    try {
      statisticsPath = Files.createTempDirectory("infra-statistics");
      logger.info("Cloning jenkins-infra/infra-statistics");
      Git.cloneRepository().setURI("git://github.com/jenkins-infra/infra-statistics.git").setBranch("gh-pages").setDirectory(statisticsPath.toFile()).call();
      logger.info("Finished cloning jenkins-infra/infra-statistics");
    } catch (Exception e) {
      logger.error("Problem downloading plugin statistics", e);
      throw new RuntimeException(e);
    }
  }

  @Override
  public void parse(JSONObject pluginJson, Plugin plugin) {
    final String name = pluginJson.getString("name");
    try {
      final Path file = statisticsPath.resolve(String.format("plugin-installation-trend%c%s.stats.json", File.separatorChar, name));
      final Stats stats = new Stats();
      if (Files.exists(file)) {
        logger.info(String.format("Processing statistics for %s", name));
        final JSONObject statsJson = new JSONObject(Files.lines(file).collect(Collectors.joining("\n")));
        final JSONObject installations = statsJson.getJSONObject("installations");
        final JSONObject installationsPercentage = statsJson.getJSONObject("installationsPercentage");
        final JSONObject installationsPerVersion = statsJson.getJSONObject("installationsPerVersion");
        final JSONObject installationsPercentagePerVersion = statsJson.getJSONObject("installationsPercentagePerVersion");
        stats.setInstallations(installations.keySet().stream().map(timestamp ->
          new Installation(
            Long.valueOf(timestamp),
            installations.getInt(timestamp)
          )
        ).sorted(Comparator.comparingLong(Installation::getTimestamp)).collect(Collectors.toList()));
        stats.setInstallationsPercentage(installationsPercentage.keySet().stream().map(timestamp ->
          new InstallationPercentage(
            Long.valueOf(timestamp),
            installationsPercentage.getDouble(timestamp)
          )
        ).sorted(Comparator.comparing(InstallationPercentage::getTimestamp)).collect(Collectors.toList()));
        stats.setInstallationsPerVersion(installationsPerVersion.keySet().stream().map(version ->
          new InstallationVersion(
            version,
            installationsPerVersion.getInt(version)
          )
        ).sorted(Comparator.comparing(InstallationVersion::getVersion)).collect(Collectors.toList()));
        stats.setInstallationsPercentagePerVersion(installationsPercentagePerVersion.keySet().stream().map(version ->
          new InstallationPercentageVersion(
            version,
            installationsPercentagePerVersion.getDouble(version)
          )
        ).sorted(Comparator.comparing(InstallationPercentageVersion::getVersion)).collect(Collectors.toList()));
        stats.setCurrentInstalls(!stats.getInstallations().isEmpty() ? stats.getInstallations().get(stats.getInstallations().size()-1).getTotal() : 0);
      } else {
        logger.warn(String.format("No statistics available for %s", name));
      }
      plugin.setStats(stats);
    } catch (Exception e) {
      logger.error(String.format("Problem parsing statistics for %s", name), e);
    }
  }
}
