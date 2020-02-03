package io.jenkins.plugins.services;

import io.jenkins.plugins.models.*;
import org.apache.commons.lang3.StringUtils;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import javax.inject.Singleton;
import java.util.concurrent.ScheduledExecutorService;

public class DatastoreServiceIntegrationTest {

  private static ServiceLocator locator;
  private static DatastoreService datastoreService;

  private static ScheduledExecutorService mockScheduledExecutorService;

  // BeforeClass because there's no reason to start/stop Elasticsearch for every test when
  // all our operations are read-only
  @BeforeClass
  public static void setUp() throws Exception {
    mockScheduledExecutorService = Mockito.mock(ScheduledExecutorService.class);
    locator  = ServiceLocatorUtilities.bind(
      new io.jenkins.plugins.datastore.Binder(),
      new io.jenkins.plugins.services.Binder(),
      new AbstractBinder() {
      @Override
      protected void configure() {
        bind(mockScheduledExecutorService.getClass()).to(ScheduledExecutorService.class).in(Singleton.class);
      }
    });
    datastoreService = locator.getService(DatastoreService.class);
    locator.getService(PrepareDatastoreService.class).populateDataStore();
  }

  @AfterClass
  public static void tearDown() throws Exception {
    locator.shutdown();
  }

  @Test
  public void testGetPlugin() {
    final Plugin plugin = datastoreService.getPlugin("git");
    Assert.assertNotNull("Git plugin not found", plugin);
    Assert.assertEquals("git", plugin.getName());
    Assert.assertFalse("Categories are empty", plugin.getCategories().isEmpty());
    Assert.assertFalse("Dependencies are empty", plugin.getDependencies().isEmpty());
    Assert.assertFalse("Maintainers are empty", plugin.getMaintainers().isEmpty());
    Assert.assertFalse("Labels are empty", plugin.getLabels().isEmpty());
    Assert.assertNotNull("Stats are null", plugin.getStats());
    Assert.assertNotNull("Scm is null", plugin.getScm());
    Assert.assertTrue("Scm link is blank", StringUtils.isNotBlank(plugin.getScm().getLink()));
    Assert.assertEquals("Issue tracker is jenkins jira", "Jenkins JIRA", plugin.getIssueTracker().getName());
  }

  @Test
  public void testGetPluginUTF8() {
    final Plugin plugin = datastoreService.getPlugin("resource-disposer");
    Assert.assertNotNull("resource-disposer plugin not found", plugin);
    Assert.assertEquals("resource-disposer", plugin.getName());
    Assert.assertFalse("Maintainers are empty", plugin.getMaintainers().isEmpty());
    for (Maintainer maintainer : plugin.getMaintainers()) {
      if (maintainer.getName().equalsIgnoreCase("Oliver Gondža")) {
        return;
      }
    }
    Assert.fail("Should have \"Oliver Gondža\" in maintainers");
  }

  @Test
  public void testGetPluginSecurityWarnings() {
    final Plugin plugin = datastoreService.getPlugin("cucumber-reports");
    Assert.assertNotNull("cucumber-reports plugin not found", plugin);
    Assert.assertEquals("cucumber-reports", plugin.getName());
    Assert.assertNotNull("securityWarnings null", plugin.getSecurityWarnings());
    Assert.assertFalse("securityWarnings are empty", plugin.getSecurityWarnings().isEmpty());
    plugin.getSecurityWarnings().forEach(securityWarning -> {
      Assert.assertNotNull("securityWarnings.version null", securityWarning);
      Assert.assertNotNull("securityWarnings.version.id null", securityWarning.getId());
      Assert.assertNotNull("securityWarnings.version.message null", securityWarning.getMessage());
      Assert.assertNotNull("securityWarnings.version.url null", securityWarning.getUrl());
      Assert.assertNotNull("securityWarnings.version.versions null", securityWarning.getVersions());
      Assert.assertFalse("securityWarnings.version.versions empty", securityWarning.getVersions().isEmpty());
    });
  }

  @Test
  public void testSearch() {
    final Plugins plugins = datastoreService.search(new SearchOptions.Builder().withQuery("git").build());
    Assert.assertNotNull("Search for 'git' is null", plugins);
    Assert.assertTrue("Should return multiple results", plugins.getTotal() > 1);
  }

  @Test
  public void testSearchSortByFirstRelease() {
    final Plugins plugins = datastoreService.search(new SearchOptions.Builder().withSortBy(SortBy.FIRST_RELEASE).build());
    Assert.assertTrue("Should return multiple results", plugins.getTotal() > 1);
    Assert.assertTrue("SortBy.FIRST_RELEASE not correct", plugins.getPlugins().get(0).getFirstRelease().isAfter(plugins.getPlugins().get(1).getFirstRelease()));
  }

  @Test
  public void testSearchSortByInstalled() {
    final Plugins plugins = datastoreService.search(new SearchOptions.Builder().withQuery("git").withSortBy(SortBy.INSTALLED).build());
    Assert.assertNotNull("Search for 'git' sort by installs is null", plugins);
    Assert.assertTrue("Should return multiple results", plugins.getTotal() > 1);
    Assert.assertTrue("SortBy.INSTALLED not correct", plugins.getPlugins().get(0).getStats().getCurrentInstalls() > plugins.getPlugins().get(1).getStats().getCurrentInstalls());
  }

  @Test
  public void testSearchSortByName() {
    final Plugins plugins = datastoreService.search(new SearchOptions.Builder().withQuery("git").withSortBy(SortBy.NAME).build());
    Assert.assertNotNull("Search for 'git' sort by name is null", plugins);
    Assert.assertTrue("Should return multiple results", plugins.getTotal() > 1);
    Assert.assertTrue("SortBy.NAME not correct", plugins.getPlugins().get(0).getName().compareTo(plugins.getPlugins().get(1).getName()) < 0);
  }

  @Test
  public void testSearchSortByRelevance() {
    final Plugins plugins = datastoreService.search(new SearchOptions.Builder().withQuery("git").withSortBy(SortBy.RELEVANCE).build());
    Assert.assertNotNull("Search for 'git' sort by relevance is null", plugins);
    Assert.assertTrue("Should return multiple results", plugins.getTotal() > 1);
  }

  @Test
  public void testSearchSortByTitle() {
    final Plugins plugins = datastoreService.search(new SearchOptions.Builder().withQuery("git").withSortBy(SortBy.TITLE).build());
    Assert.assertNotNull("Search for 'git' sort by title is null", plugins);
    Assert.assertTrue("Should return multiple results", plugins.getTotal() > 1);
    Assert.assertTrue("SortBy.TITLE not correct", plugins.getPlugins().get(0).getTitle().compareTo(plugins.getPlugins().get(1).getTitle()) < 0);
  }

  @Test
  public void testSearchSortByTrend() {
    final Plugins plugins = datastoreService.search(new SearchOptions.Builder().withQuery("git").withSortBy(SortBy.TREND).build());
    Assert.assertNotNull("Search for 'git' sort by trend is null", plugins);
    Assert.assertTrue("Should return multiple results", plugins.getTotal() > 1);
    Assert.assertTrue("SortBy.TREND not correct", plugins.getPlugins().get(0).getStats().getTrend() > plugins.getPlugins().get(1).getStats().getTrend());
  }

  @Test
  public void testSearchSortByUpdated() {
    final Plugins plugins = datastoreService.search(new SearchOptions.Builder().withQuery("git").withSortBy(SortBy.UPDATED).build());
    Assert.assertNotNull("Search for 'git' sort by updated is null", plugins);
    Assert.assertTrue("Should return multiple results", plugins.getTotal() > 1);
    Assert.assertTrue("SortBy.UPDATED not correct", plugins.getPlugins().get(0).getReleaseTimestamp().isAfter(plugins.getPlugins().get(1).getReleaseTimestamp()));
  }

  @Test
  public void testSearchCategories() {
    final Plugins plugins = datastoreService.search(new SearchOptions.Builder().withCategories("scm").build());
    Assert.assertNotNull("Search for categories 'scm' is null", plugins);
    for (Plugin plugin : plugins.getPlugins()) {
      if (plugin.getCategories().contains("scm")) {
        return;
      }
    }
    Assert.fail("Didn't find plugins with categories 'scm'");
  }

  @Test
  public void testSearchLabels() {
    final Plugins plugins = datastoreService.search(new SearchOptions.Builder().withLabels("scm").build());
    Assert.assertNotNull("Search for labels 'scm' is null", plugins);
    for (Plugin plugin : plugins.getPlugins()) {
      if (plugin.getLabels().contains("scm")) {
        return;
      }
    }
    Assert.fail("Didn't find plugins with labels 'scm'");
  }

  @Test
  public void testSearchMaintainers() {
    final Plugins plugins = datastoreService.search(new SearchOptions.Builder().withMaintainers("Kohsuke Kawaguchi").build());
    Assert.assertNotNull("Search for maintainers 'Kohsuke Kawaguchi' is null", plugins);
    for (Plugin plugin : plugins.getPlugins()) {
      for (Maintainer maintainer : plugin.getMaintainers()) {
        if (maintainer.getName().equalsIgnoreCase("Kohsuke Kawaguchi")) {
          return;
        }
      }
    }
    Assert.fail("Didn't find plugins with maintainers 'Kohsuke Kawaguchi'");
  }

  @Test
  public void testSearchRequiredCore() {
    final Plugins plugins = datastoreService.search(new SearchOptions.Builder().withCore("1.505").build());
    Assert.assertNotNull("Search for requiredCore is null", plugins);
    for (Plugin plugin : plugins.getPlugins()) {
      if (!plugin.getRequiredCore().equals("1.505")) {
        Assert.fail("Found plugin with requiredCore not '1.505'");
      }
    }
  }

  @Test
  public void testGetCategories() {
    final Categories categories = datastoreService.getCategories();
    Assert.assertNotNull("Categories null", categories);
    Assert.assertFalse("Categories empty", categories.getCategories().isEmpty());
    Assert.assertEquals("Categories limit doesn't match", categories.getLimit(), categories.getCategories().size());
  }

  @Test
  public void testGetLabels() {
    final Labels labels = datastoreService.getLabels();
    Assert.assertNotNull("Labels null", labels);
    Assert.assertFalse("Labels empty", labels.getLabels().isEmpty());
    Assert.assertEquals("Labels limit doesn't match", labels.getLimit(), labels.getLabels().size());
  }

  @Test
  public void testGetMaintainers() {
    final Maintainers maintainers = datastoreService.getMaintainers();
    Assert.assertNotNull("Maintainers null", maintainers);
    Assert.assertFalse("Maintainers empty", maintainers.getMaintainers().isEmpty());
    Assert.assertEquals("Maintainers limit doesn't match", maintainers.getLimit(), maintainers.getMaintainers().size());
  }

  @Test
  public void testGetVersions() {
    final Versions versions = datastoreService.getVersions();
    Assert.assertNotNull("Versions null", versions);
    Assert.assertFalse("Versions empty", versions.getVersions().isEmpty());
    Assert.assertEquals("Versions limit doesn't match", versions.getLimit(), versions.getVersions().size());
  }

}
