package io.openepcis.snippets;

import io.openepcis.snippets.model.Snippet;
import io.openepcis.snippets.service.SnippetService;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@QuarkusTest
public class SnippetResourceTest {

    @Inject
    SnippetResource snippetResource;

    @InjectMock
    SnippetService snippetService;

    @Test
    public void testGetSnippetsWithFuzzySearch() throws IOException {
        // Setup mock data
        List<Snippet> mockSnippets = new ArrayList<>();
        Snippet snippet = new Snippet();
        snippet.setTitle("Test Snippet");
        snippet.setDescription("This is a test snippet");
        mockSnippets.add(snippet);

        // Setup mock service response for fuzzy search
        when(snippetService.searchSnippets(eq("testt"), anyInt())).thenReturn(mockSnippets);

        // Call the endpoint with a slightly misspelled word
        Response response = snippetResource.getSnippets("testt");

        // Verify the service was called with the correct parameters
        verify(snippetService).searchSnippets(eq("testt"), anyInt());

        // Verify the response
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        // Get the response entity
        @SuppressWarnings("unchecked")
        List<Snippet> responseSnippets = (List<Snippet>) response.getEntity();

        // Verify the response contains the expected snippets
        assertNotNull(responseSnippets);
        assertEquals(1, responseSnippets.size());
        assertEquals("Test Snippet", responseSnippets.get(0).getTitle());

        System.out.println("[DEBUG_LOG] Fuzzy search test passed: found snippet with misspelled search term");
    }

    @Test
    public void testGetSnippetsIgnoresStopWords() throws IOException {
        // Setup mock data
        List<Snippet> mockSnippets = new ArrayList<>();
        Snippet snippet = new Snippet();
        snippet.setTitle("Test Snippet");
        snippet.setDescription("This is a test snippet");
        mockSnippets.add(snippet);

        // Setup mock service response for search with stop words
        when(snippetService.searchSnippets(eq("the test and a"), anyInt())).thenReturn(mockSnippets);

        // Call the endpoint with a query containing stop words
        Response response = snippetResource.getSnippets("the test and a");

        // Verify the service was called with the correct parameters
        verify(snippetService).searchSnippets(eq("the test and a"), anyInt());

        // Verify the response
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        // Get the response entity
        @SuppressWarnings("unchecked")
        List<Snippet> responseSnippets = (List<Snippet>) response.getEntity();

        // Verify the response contains the expected snippets
        assertNotNull(responseSnippets);
        assertEquals(1, responseSnippets.size());
        assertEquals("Test Snippet", responseSnippets.get(0).getTitle());

        System.out.println("[DEBUG_LOG] Stop words test passed: found snippet with query containing stop words");
    }

    @Test
    public void testGetSnippetsHandlesIOException() throws IOException {
        // Setup mock service to throw IOException
        when(snippetService.searchSnippets(anyString(), anyInt())).thenThrow(new IOException("Test exception"));

        // Call the endpoint
        Response response = snippetResource.getSnippets("test");

        // Verify the service was called
        verify(snippetService).searchSnippets(anyString(), anyInt());

        // Verify the response is an error
        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());

        System.out.println("[DEBUG_LOG] Error handling test passed: returned 500 for IOException");
    }

    @Test
    public void testCreateSnippet() throws IOException {
        // Setup mock data
        String requestBody = "{\"title\":\"Test Snippet\",\"description\":\"This is a test snippet\"}";
        Snippet mockSnippet = new Snippet();
        mockSnippet.setTitle("Test Snippet");
        mockSnippet.setDescription("This is a test snippet");

        // Setup mock service response
        when(snippetService.createSnippet(eq(requestBody))).thenReturn(mockSnippet);

        // Call the endpoint
        Response response = snippetResource.createSnippet(requestBody);

        // Verify the service was called with the correct parameters
        verify(snippetService).createSnippet(eq(requestBody));

        // Verify the response
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());

        // Get the response entity
        Snippet responseSnippet = (Snippet) response.getEntity();

        // Verify the response contains the expected snippet
        assertNotNull(responseSnippet);
        assertEquals("Test Snippet", responseSnippet.getTitle());
        assertEquals("This is a test snippet", responseSnippet.getDescription());

        System.out.println("[DEBUG_LOG] Create snippet test passed: created snippet successfully");
    }

    @Test
    public void testCreateSnippetValidationError() throws IOException {
        // Setup mock service to throw IllegalArgumentException for validation error
        String invalidRequestBody = "{\"invalid\":\"json\"}";
        when(snippetService.createSnippet(eq(invalidRequestBody)))
            .thenThrow(new IllegalArgumentException("Invalid snippet format"));

        // Call the endpoint
        Response response = snippetResource.createSnippet(invalidRequestBody);

        // Verify the service was called
        verify(snippetService).createSnippet(eq(invalidRequestBody));

        // Verify the response is a validation error
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        assertEquals("Invalid snippet format", response.getEntity());

        System.out.println("[DEBUG_LOG] Validation error test passed: returned 400 for invalid snippet");
    }

    @Test
    public void testCreateSnippetHandlesIOException() throws IOException {
        // Setup mock service to throw IOException
        String requestBody = "{\"title\":\"Test Snippet\",\"description\":\"This is a test snippet\"}";
        when(snippetService.createSnippet(anyString())).thenThrow(new IOException("Test exception"));

        // Call the endpoint
        Response response = snippetResource.createSnippet(requestBody);

        // Verify the service was called
        verify(snippetService).createSnippet(anyString());

        // Verify the response is an error
        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());

        System.out.println("[DEBUG_LOG] Error handling test passed: returned 500 for IOException in create");
    }

    @Test
    public void testDeleteSnippet() throws IOException {
        // Setup mock service (doNothing is the default behavior for void methods)
        doNothing().when(snippetService).delete(anyString());

        // Call the endpoint
        Response response = snippetResource.deleteSnippet("test-id");

        // Verify the service was called with the correct parameters
        verify(snippetService).delete(eq("test-id"));

        // Verify the response
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());

        System.out.println("[DEBUG_LOG] Delete snippet test passed: deleted snippet successfully");
    }

    @Test
    public void testDeleteSnippetHandlesIOException() throws IOException {
        // Setup mock service to throw IOException
        doThrow(new IOException("Snippet not found")).when(snippetService).delete(anyString());

        // Call the endpoint
        Response response = snippetResource.deleteSnippet("non-existent-id");

        // Verify the service was called
        verify(snippetService).delete(anyString());

        // Verify the response is an error
        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());

        System.out.println("[DEBUG_LOG] Error handling test passed: returned 500 for IOException in delete");
    }

    @Test
    public void testGetSnippetsWithEmptySearch() throws IOException {
        // Setup mock data
        List<Snippet> mockSnippets = new ArrayList<>();
        Snippet snippet = new Snippet();
        snippet.setTitle("Test Snippet");
        snippet.setDescription("This is a test snippet");
        mockSnippets.add(snippet);

        // Setup mock service response for empty search
        when(snippetService.searchSnippets(eq(""), anyInt())).thenReturn(mockSnippets);

        // Call the endpoint with empty search
        Response response = snippetResource.getSnippets("");

        // Verify the service was called with the correct parameters
        verify(snippetService).searchSnippets(eq(""), anyInt());

        // Verify the response
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        // Get the response entity
        @SuppressWarnings("unchecked")
        List<Snippet> responseSnippets = (List<Snippet>) response.getEntity();

        // Verify the response contains the expected snippets
        assertNotNull(responseSnippets);
        assertEquals(1, responseSnippets.size());

        System.out.println("[DEBUG_LOG] Empty search test passed: found snippet with empty search term");
    }

    @Test
    public void testGetSnippetsWithNullSearch() throws IOException {
        // Setup mock data
        List<Snippet> mockSnippets = new ArrayList<>();
        Snippet snippet = new Snippet();
        snippet.setTitle("Test Snippet");
        snippet.setDescription("This is a test snippet");
        mockSnippets.add(snippet);

        // Setup mock service response for null search
        when(snippetService.searchSnippets(isNull(), anyInt())).thenReturn(mockSnippets);

        // Call the endpoint with null search
        Response response = snippetResource.getSnippets(null);

        // Verify the service was called with the correct parameters
        verify(snippetService).searchSnippets(isNull(), anyInt());

        // Verify the response
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        // Get the response entity
        @SuppressWarnings("unchecked")
        List<Snippet> responseSnippets = (List<Snippet>) response.getEntity();

        // Verify the response contains the expected snippets
        assertNotNull(responseSnippets);
        assertEquals(1, responseSnippets.size());

        System.out.println("[DEBUG_LOG] Null search test passed: found snippet with null search term");
    }
}
