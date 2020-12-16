package io.jenkins.plugins.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class JiraIssues {
    @JsonProperty("issues")
    public List<JiraIssue> issues = new ArrayList<JiraIssue>();

    @JsonProperty("total")
    public int getTotal() {
      return this.issues.size();
    }

    public JiraIssues() {

    }

}
