package io.jenkins.plugins.endpoints;

import io.jenkins.plugins.services.PrepareDatastoreService;
import io.jenkins.plugins.utils.VersionUtils;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>Endpoint for retrieving health about the application</p>
 */
@Path("/health")
@Produces(MediaType.APPLICATION_JSON)
public class HealthEndpoint {
  @Inject
  private PrepareDatastoreService prepareDatastoreService;

  @Path("/elasticsearch")
  @GET
  public Map<String, Object> getElasticsearchHealth() {
    final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    final Map<String, Object> result = new HashMap<>();
    final LocalDateTime createdAt = prepareDatastoreService.getCurrentCreatedAt();
    result.put("createdAt", createdAt != null ? formatter.format(createdAt) : null);
    result.put("mappingVersion", VersionUtils.getMappingVersion());
    result.put("elasticsearchVersion", VersionUtils.getElasticsearchVersion());
    return result;
  }

}
