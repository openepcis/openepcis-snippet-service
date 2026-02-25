package io.openepcis.snippets;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.openepcis.snippets.constants.Constants;
import io.openepcis.snippets.model.Snippet;
import io.openepcis.snippets.service.SnippetService;
import io.quarkus.security.Authenticated;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.io.IOException;
import java.util.List;

@Path("/snippet")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "snippets", description = "Snippet operations")
@SecurityScheme(securitySchemeName = "bearer-auth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
@Slf4j
public class SnippetResource {

    @Inject
    SnippetService snippetService;

    @Inject
    ObjectMapper objectMapper;

    @POST
    @Authenticated
    @SecurityRequirement(name = "bearer-auth")
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
            log.debug("Validation error creating snippet: {}", e.getMessage());
            return Response.status(Status.BAD_REQUEST)
                    .entity(e.getMessage())
                    .build();
        } catch (IOException e) {
            log.error("Error creating snippet", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                    .entity("Error creating snippet: " + e.getMessage())
                    .build();
        } catch (Exception e) {
            log.error("Unexpected error creating snippet", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                    .entity("Unexpected error creating snippet")
                    .build();
        }
    }

    @DELETE
    @Path("/{id}")
    @Authenticated
    @SecurityRequirement(name = "bearer-auth")
    @Operation(summary = "Delete a snippet", description = "Delete a snippet by its $id")
    @APIResponses(value = {
            @APIResponse(responseCode = "204", description = "Snippet deleted successfully"),
            @APIResponse(responseCode = "404", description = "Snippet not found"),
            @APIResponse(responseCode = "500", description = "Internal server error")
    })
    public Response deleteSnippet(@PathParam("id") String id) {
        try {
            snippetService.delete(id);
            return Response.noContent().build();
        } catch (IOException e) {
            log.error("Error deleting snippet", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                    .entity("Error deleting snippet: " + e.getMessage())
                    .build();
        }
    }

    @GET
    @PermitAll
    @Operation(summary = "Search for snippets", description = "Search for snippets based on the provided search text")
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "Successful operation", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(type = SchemaType.ARRAY, implementation = Snippet.class))),
            @APIResponse(responseCode = "500", description = "Internal server error")
    })
    public Response getSnippets(
            @Parameter(description = "Text to search for in snippets") @QueryParam("searchText") String searchText) {
        try {
            // Use the service to search for snippets
            List<Snippet> snippets = snippetService.searchSnippets(searchText, Constants.DEFAULT_LIMIT);
            return Response.ok(snippets).build();
        } catch (IOException e) {
            log.error("Error retrieving snippets", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                    .entity("Error retrieving snippets: " + e.getMessage())
                    .build();
        }
    }

}
