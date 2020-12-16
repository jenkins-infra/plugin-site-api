package io.jenkins.plugins.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Scm {

  @JsonProperty("issues")
  private String issues;

  @JsonProperty("link")
  private String link;

  @JsonProperty("inLatestRelease")
  private String inLatestRelease;

  @JsonProperty("sinceLatestRelease")
  private String sinceLatestRelease;

  @JsonProperty("pullRequests")
  private String pullRequests;

  public Scm() {
  }

  public Scm(String issues, String link, String inLatestRelease, String sinceLatestRelease, String pullRequests) {
    this.issues = issues;
    this.link = link;
    this.inLatestRelease = inLatestRelease;
    this.sinceLatestRelease = sinceLatestRelease;
    this.pullRequests = pullRequests;
  }

  public String getIssues() {
    return issues;
  }

  public void setIssues(String issues) {
    this.issues = issues;
  }

  public String getLink() {
    return link;
  }

  public void setLink(String link) {
    this.link = link;
  }

  public String getInLatestRelease() {
    return inLatestRelease;
  }

  public void setInLatestRelease(String inLatestRelease) {
    this.inLatestRelease = inLatestRelease;
  }

  public String getSinceLatestRelease() {
    return sinceLatestRelease;
  }

  public void setSinceLatestRelease(String sinceLatestRelease) {
    this.sinceLatestRelease = sinceLatestRelease;
  }

  public String getPullRequests() {
    return pullRequests;
  }

  public void setPullRequests(String pullRequests) {
    this.pullRequests = pullRequests;
  }
}
