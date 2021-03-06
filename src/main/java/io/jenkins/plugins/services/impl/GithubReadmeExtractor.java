package io.jenkins.plugins.services.impl;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.jenkins.plugins.models.Plugin;

public class GithubReadmeExtractor extends GithubExtractor {

  private final static class GithubReadmeMatcher implements GithubMatcher {
    private final Matcher matcher;
    private final Plugin plugin;

    private GithubReadmeMatcher(Plugin plugin, Matcher matcher) {
      this.matcher = matcher;
      this.plugin = plugin;
    }

    @Override
    public String getEndpoint() {
      return README_ENDPOINT;
    }

    @Override
    public String getDirectory() {
      return "/";
    }

    @Override
    public String getBranch() {
      String branch = matcher.group(3);
      return branch == null ? plugin.getDefaultBranch() : branch;
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

  private static final String README_ENDPOINT = "readme";
  private static final Pattern REPO_PATTERN = Pattern
      .compile("https?://github.com/jenkinsci/([^/.]+)(\\.git|/tree/([^/]+))?/?$");

  @Override
  protected GithubMatcher getDelegate(Plugin plugin) {
    final Matcher matcher = REPO_PATTERN.matcher(plugin.getWikiUrl());
    return new GithubReadmeMatcher(plugin, matcher);
  }

}
