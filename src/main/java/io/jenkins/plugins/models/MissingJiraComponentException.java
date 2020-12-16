package io.jenkins.plugins.models;

import java.util.Objects;

public class MissingJiraComponentException extends Exception {
  private static final long serialVersionUID = 128010869245346324L;
  private final String pluginName;
  private final String componentName;

  public MissingJiraComponentException(String pluginName, String componentName) {
    super(String.format("ComponentName(%s) and PluginName(%s) do not match.", componentName, pluginName));
    this.pluginName = pluginName;
    this.componentName = componentName;
  }

  public String getPluginName() {
    return pluginName;
  }

  public String getComponentName() {
    return componentName;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof MissingJiraComponentException)) return false;
    MissingJiraComponentException that = (MissingJiraComponentException) o;
    return Objects.equals(pluginName, that.pluginName) && Objects.equals(componentName, that.componentName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(pluginName, componentName);
  }

}
