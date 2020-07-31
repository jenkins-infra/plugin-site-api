package io.jenkins.plugins.models;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Plugin {

  // Shouldn't have do specify serializer/deserializer but it produces a JSON object
  // if the JavaTimeModule is registered
  @JsonProperty("buildDate")
  @JsonFormat(pattern = "yyyy-MM-dd")
  @JsonSerialize(using = LocalDateSerializer.class)
  @JsonDeserialize(using = LocalDateDeserializer.class)
  private LocalDate buildDate;

  @JsonProperty("categories")
  private Set<String> categories;

  @JsonProperty("dependencies")
  private List<Dependency> dependencies = new ArrayList<>();

  @JsonProperty("maintainers")
  private List<Maintainer> maintainers;

  @JsonProperty("excerpt")
  private String excerpt;

  @JsonProperty("gav")
  private String gav;

  @JsonProperty("labels")
  private Set<String> labels;

  @JsonProperty("name")
  private String name;

  // Shouldn't have do specify serializer/deserializer but it produces a JSON object
  // if the JavaTimeModule is registered
  @JsonProperty("previousTimestamp")
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SS'Z'")
  @JsonSerialize(using = LocalDateTimeSerializer.class)
  @JsonDeserialize(using = LocalDateTimeDeserializer.class)
  private LocalDateTime previousTimestamp;

  @JsonProperty("previousVersion")
  private String previousVersion;

  // Shouldn't have do specify serializer/deserializer but it produces a JSON object
  // if the JavaTimeModule is registered
  @JsonProperty("releaseTimestamp")
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SS'Z'")
  @JsonSerialize(using = LocalDateTimeSerializer.class)
  @JsonDeserialize(using = LocalDateTimeDeserializer.class)
  private LocalDateTime releaseTimestamp;

  @JsonProperty("requiredCore")
  private String requiredCore;

  @JsonProperty("scm")
  private Scm scm;

  @JsonProperty("sha1")
  private String sha1;

  @JsonProperty("stats")
  private Stats stats;

  @JsonProperty("title")
  private String title;

  @JsonProperty("url")
  private String url;

  @JsonProperty("version")
  private String version;

  @JsonProperty("securityWarnings")
  private List<SecurityWarning> securityWarnings;

  @JsonProperty("wiki")
  private Wiki wiki;

  @JsonProperty
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SS'Z'")
  @JsonSerialize(using = LocalDateTimeSerializer.class)
  @JsonDeserialize(using = LocalDateTimeDeserializer.class)
  private LocalDateTime firstRelease;

  @JsonProperty("issueTracker")
  private IssueTracker issueTracker;

  public Plugin() {
  }

  public Plugin(LocalDate buildDate, Set<String> categories, List<Dependency> dependencies, List<Maintainer> maintainers,
                String excerpt, String gav, Set<String> labels, String name, LocalDateTime previousTimestamp,
                String previousVersion, LocalDateTime releaseTimestamp, String requiredCore, Scm scm, String sha1,
                Stats stats, String title, String url, String version, List<SecurityWarning> securityWarnings, Wiki wiki,
                LocalDateTime firstRelease, IssueTracker issueTracker) {
    this.buildDate = buildDate;
    this.categories = categories;
    this.dependencies = dependencies;
    this.maintainers = maintainers;
    this.excerpt = excerpt;
    this.gav = gav;
    this.labels = labels;
    this.name = name;
    this.previousTimestamp = previousTimestamp;
    this.previousVersion = previousVersion;
    this.releaseTimestamp = releaseTimestamp;
    this.requiredCore = requiredCore;
    this.scm = scm;
    this.sha1 = sha1;
    this.stats = stats;
    this.title = title;
    this.url = url;
    this.version = version;
    this.securityWarnings = securityWarnings;
    this.wiki = wiki;
    this.firstRelease = firstRelease;
    this.issueTracker = issueTracker;
  }

  public LocalDate getBuildDate() {
    return buildDate;
  }

  public void setBuildDate(LocalDate buildDate) {
    this.buildDate = buildDate;
  }

  public Set<String> getCategories() {
    return categories;
  }

  public void setCategories(Set<String> categories) {
    this.categories = categories;
  }

  public List<Dependency> getDependencies() {
    return dependencies;
  }

  public void setDependencies(List<Dependency> dependencies) {
    this.dependencies = dependencies;
  }

  public void addDependencies(Collection<Dependency> dependencies) {
    this.dependencies.addAll(dependencies);
  }

  public List<Maintainer> getMaintainers() {
    return maintainers;
  }

  public void setMaintainers(List<Maintainer> maintainers) {
    this.maintainers = maintainers;
  }

  public String getExcerpt() {
    return excerpt;
  }

  public void setExcerpt(String excerpt) {
    this.excerpt = excerpt;
  }

  public String getGav() {
    return gav;
  }

  public void setGav(String gav) {
    this.gav = gav;
  }

  public Set<String> getLabels() {
    return labels;
  }

  public void setLabels(Set<String> labels) {
    this.labels = labels;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public LocalDateTime getPreviousTimestamp() {
    return previousTimestamp;
  }

  public void setPreviousTimestamp(LocalDateTime previousTimestamp) {
    this.previousTimestamp = previousTimestamp;
  }

  public String getPreviousVersion() {
    return previousVersion;
  }

  public void setPreviousVersion(String previousVersion) {
    this.previousVersion = previousVersion;
  }

  public LocalDateTime getReleaseTimestamp() {
    return releaseTimestamp;
  }

  public void setReleaseTimestamp(LocalDateTime releaseTimestamp) {
    this.releaseTimestamp = releaseTimestamp;
  }

  public String getRequiredCore() {
    return requiredCore;
  }

  public void setRequiredCore(String requiredCore) {
    this.requiredCore = requiredCore;
  }

  public Scm getScm() {
    return scm;
  }

  public void setScm(Scm scm) {
    this.scm = scm;
  }

  public String getSha1() {
    return sha1;
  }

  public void setSha1(String sha1) {
    this.sha1 = sha1;
  }

  public Stats getStats() {
    return stats;
  }

  public void setStats(Stats stats) {
    this.stats = stats;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public List<SecurityWarning> getSecurityWarnings() {
    return securityWarnings;
  }

  public void setSecurityWarnings(List<SecurityWarning> securityWarnings) {
    this.securityWarnings = securityWarnings;
  }

  public Wiki getWiki() {
    return wiki;
  }

  public void setWiki(Wiki wiki) {
    this.wiki = wiki;
  }

  public LocalDateTime getFirstRelease() {
    return firstRelease;
  }

  public void setFirstRelease(LocalDateTime firstRelease) {
    this.firstRelease = firstRelease;
  }

  public IssueTracker getIssueTracker() {
    return issueTracker;
  }

  public void setIssueTracker(IssueTracker issueTracker) {
    this.issueTracker = issueTracker;
  }
}
