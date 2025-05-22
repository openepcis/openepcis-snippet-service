package io.openepcis.snippets;

import java.io.IOException;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import io.openepcis.snippets.model.Snippet;
import io.openepcis.snippets.service.SnippetService;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

@Path("/snippet")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "snippets", description = "Snippet operations")
public class SnippetResource {

    private static final Logger LOG = Logger.getLogger(SnippetResource.class);
    private static final int DEFAULT_LIMIT = 10;

    @Inject
    SnippetService snippetService;

    @Inject
    ObjectMapper objectMapper;

    @POST
    @Operation(summary = "Create a new snippet", description = "Creates a new code snippet from the provided JSON")
    @APIResponses(value = {
            @APIResponse(responseCode = "201", description = "Snippet created successfully", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Snippet.class))),
            @APIResponse(responseCode = "400", description = "Invalid request"),
            @APIResponse(responseCode = "500", description = "Internal server error")
    })
    public Response createSnippet(String requestBody) {
        try {
            // Use the service to create the snippet
            Snippet snippet = snippetService.createSnippet(requestBody);

            // Return the snippet without the source field
            return Response.status(Status.CREATED).entity(snippet.withoutSource()).build();
        } catch (IllegalArgumentException e) {
            // Handle validation errors
            LOG.debug("Validation error creating snippet: " + e.getMessage());
            return Response.status(Status.BAD_REQUEST)
                    .entity(e.getMessage())
                    .build();
        } catch (IOException e) {
            LOG.error("Error creating snippet", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                    .entity("Error creating snippet: " + e.getMessage())
                    .build();
        } catch (Exception e) {
            LOG.error("Unexpected error creating snippet", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                    .entity("Unexpected error creating snippet")
                    .build();
        }
    }

    @GET
    @Operation(summary = "Search for snippets", description = "Search for snippets based on the provided search text")
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "Successful operation", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(type = SchemaType.ARRAY, implementation = Snippet.class))),
            @APIResponse(responseCode = "500", description = "Internal server error")
    })
    public Response getSnippets(
            @Parameter(description = "Text to search for in snippets") @QueryParam("searchText") String searchText) {
        try {
            // Use the service to search for snippets
            List<Snippet> snippets = snippetService.searchSnippets(searchText, DEFAULT_LIMIT);
            return Response.ok(snippets).build();
        } catch (IOException e) {
            LOG.error("Error retrieving snippets", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                    .entity("Error retrieving snippets: " + e.getMessage())
                    .build();
        }
    }

}
