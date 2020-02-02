package io.jenkins.plugins.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public class IssueTracker {

  @JsonProperty("name")
  private String name;
  @JsonProperty("url")
  private String url;

  @SuppressWarnings("unused") // used by jackson
  public IssueTracker() {
  }

  public IssueTracker(String name, String url) {
    this.name = name;
    this.url = url;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }
}
