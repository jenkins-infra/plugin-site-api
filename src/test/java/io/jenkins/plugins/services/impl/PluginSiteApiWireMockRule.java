package io.jenkins.plugins.services.impl;

import com.github.tomakehurst.wiremock.common.SingleRootFileSource;
import com.github.tomakehurst.wiremock.junit.WireMockRule;

import java.io.File;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

public class PluginSiteApiWireMockRule extends WireMockRule {
  private final String wiremockPath;
  private final List<String> urls;

  public PluginSiteApiWireMockRule(Class<?> clazz, List<String> urls) {
    super(options().dynamicPort().usingFilesUnderDirectory("src/test/resources/wiremocks/" + clazz.getSimpleName()));
    this.wiremockPath = "src/test/resources/wiremocks/" + clazz.getSimpleName();
    this.urls = urls;
  }

  @Override
  protected void before() {
    super.before();
    if (Boolean.getBoolean("enable.recording")) {
      File recordingsDir = new File(wiremockPath + "/mappings");
      recordingsDir.mkdirs();
      File filesDir = new File(wiremockPath + "/__files");
      filesDir.mkdirs();
      this.enableRecordMappings(
        new SingleRootFileSource(recordingsDir.getAbsolutePath()),
        new SingleRootFileSource(filesDir.getAbsolutePath())
      );

      for (String url : urls) {
        stubFor(get(urlMatching(".*")).atPriority(10)
          .willReturn(aResponse().proxiedFrom(url)));
      }
    }
  }

  @Override
  protected void after() {
    super.after();
    for (String url : urls) {
      this.snapshotRecord(recordSpec().forTarget(url)
        .extractTextBodiesOver(1024));
    }

  }
}
