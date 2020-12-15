package io.jenkins.plugins.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class JiraIssue {
    @JsonProperty("key")
    public String key;

    @JsonProperty("issueType")
    public String issueType;

    @JsonProperty("priority")
    public String priority;

    @JsonProperty("status")
    public String status;

    @JsonProperty("resolution")
    public String resolution;

    @JsonProperty("summary")
    public String summary;

    @JsonProperty("assignee")
    public String assignee;

    @JsonProperty("reporter")
    public String reporter;

    @JsonProperty("created")
    public String created;

    @JsonProperty("updated")
    public String updated;

    @JsonProperty("url")
    public String getUrl() {
      return "https://issues.jenkins.io/browse/" + this.key;
    }

    public JiraIssue() {

    }

}
