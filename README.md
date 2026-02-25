# OpenEPCIS Snippet Service

A REST service built with Quarkus that provides APIs for storing and retrieving JSON schema snippets using OpenSearch. The service allows you to save JSON schema snippets and search them based on their title and description fields with support for fuzzy search, wildcard matching, and synonyms.

## Table of Contents

- [Prerequisites](#prerequisites)
- [Quick Start](#quick-start)
- [OpenSearch Setup](#opensearch-setup)
- [Running the Application](#running-the-application)
- [Verifying the Setup](#verifying-the-setup)
- [API Documentation](#api-documentation)
- [Authentication](#authentication)
- [Configuration](#configuration)
- [Packaging and Deployment](#packaging-and-deployment)
- [Troubleshooting](#troubleshooting)

## Prerequisites

Before running this application, ensure you have the following installed:

| Requirement | Version | Check Command |
|-------------|---------|---------------|
| Java | 21 or higher | `java -version` |
| Maven | 3.8.1 or higher | `mvn -version` |
| Docker | Latest | `docker --version` |

## Quick Start

```bash
# 1. Start OpenSearch (without security for local development)
docker run -d --name opensearch-snippet -p 9200:9200 -p 9600:9600 -e "discovery.type=single-node" -e "DISABLE_SECURITY_PLUGIN=true" opensearchproject/opensearch:latest

# 2. Verify OpenSearch is running
curl http://localhost:9200

# 3. Start the application
mvn compile quarkus:dev

# 4. Access the application
# Swagger UI: http://localhost:8080/swagger-ui
# API Endpoint: http://localhost:8080/snippet
```

## OpenSearch Setup

The application requires an OpenSearch instance. For local development, run OpenSearch using Docker.

### OpenSearch without Security (Recommended for Local Development)

```bash
docker run -d --name opensearch \
  -p 9200:9200 -p 9600:9600 \
  -e "discovery.type=single-node" \
  -e "DISABLE_SECURITY_PLUGIN=true" \
  opensearchproject/opensearch:latest
```

### Verify OpenSearch is Running

```bash
curl http://localhost:9200
```

Expected response:
```json
{
  "name" : "opensearch-node",
  "cluster_name" : "docker-cluster",
  "cluster_uuid" : "...",
  "version" : {
    "distribution" : "opensearch",
    "number" : "2.x.x",
    ...
  },
  "tagline" : "The OpenSearch Project: https://opensearch.org/"
}
```

### Managing OpenSearch Container

```bash
# Stop OpenSearch
docker stop opensearch

# Start OpenSearch (after stopping)
docker start opensearch

# Remove OpenSearch container
docker rm -f opensearch

# View OpenSearch logs
docker logs opensearch
```

## Running the Application

### Development Mode (with Live Reload)

```bash
mvn compile quarkus:dev
```

The application will start on `http://localhost:8080`. Any code changes will be automatically reloaded.

**Available URLs in Dev Mode:**
- Application: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui
- OpenAPI Spec: http://localhost:8080/openapi
- Quarkus Dev UI: http://localhost:8080/q/dev-ui

### What Happens on Startup

1. The application connects to OpenSearch at `localhost:9200`
2. If the `snippets` index doesn't exist, it's automatically created with proper mappings
3. The REST API becomes available for storing and searching snippets

You should see this in the logs:
```
INFO  [io.ope.sni.rep.SnippetRepository] Created OpenSearch index: snippets
```
Or if it already exists:
```
INFO  [io.ope.sni.rep.SnippetRepository] OpenSearch index already exists: snippets
```

## Verifying the Setup

### Step 1: Check the Application is Running

```bash
curl http://localhost:8080/snippet
```

Expected response (empty array initially):
```json
[]
```

### Step 2: Add a Test Snippet

```bash
curl -X POST http://localhost:8080/snippet \
  -H "Content-Type: application/json" \
  -d '{
    "$schema": "https://json-schema.org/draft/2020-12/schema",
    "$id": "https://example.com/test-snippet",
    "title": "Test Snippet",
    "description": "This is a test snippet for verifying the setup",
    "definitions": {
      "testType": {
        "type": "string"
      }
    }
  }'
```

### Step 3: Search for the Snippet

```bash
curl "http://localhost:8080/snippet?searchText=test"
```

You should see the snippet you just created in the response.

## API Documentation

### Endpoints Overview

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/snippet` | Bearer token | Create a new snippet |
| GET | `/snippet` | Public | Search snippets |
| DELETE | `/snippet/{id}` | Bearer token | Delete a snippet by ID |

### POST /snippet - Create Snippet

Saves a JSON schema snippet to OpenSearch.

**Request:**
```bash
curl -X POST http://localhost:8080/snippet \
  -H "Content-Type: application/json" \
  -d '{
    "$id": "https://openepcis.github.io/example/snippet.json",
    "$schema": "http://json-schema.org/draft-07/schema#",
    "title": "CPI EPC URI",
    "description": "Specifies value for the epcList field in EPC URI format",
    "definitions": {
      "epc-uri-cpi": {
        "type": "string",
        "pattern": "^urn:epc:id:cpi:.*$"
      }
    }
  }'
```

**Required Fields:**
- `$id` - Unique identifier (URI format)
- `$schema` - JSON schema version
- `title` - Snippet title
- `description` - Snippet description
- `definitions` OR `$defs` - Schema definitions (one or the other, not both)

**Response:** `201 Created` with the saved snippet

### GET /snippet - Search Snippets

Retrieves snippets matching the search criteria.

**Query Parameters:**
- `searchText` (optional) - Text to search in title and description fields

**Examples:**
```bash
# Get all snippets (returns latest 10)
curl http://localhost:8080/snippet

# Search for snippets containing "CPI"
curl "http://localhost:8080/snippet?searchText=CPI"

# Search with multiple words
curl "http://localhost:8080/snippet?searchText=epc%20uri"
```

**Search Features:**
- Fuzzy matching (handles typos)
- Wildcard search
- Synonym support (e.g., "pharma" matches "pharmaceutical", "drug", "medicine")
- Results sorted by creation date (newest first)

### DELETE /snippet/{id} - Delete Snippet

Deletes a snippet by its `$id`.

**Request:**
```bash
curl -X DELETE "http://localhost:8080/snippet/https://example.com/test-snippet"
```

**Response:** `204 No Content`

## Authentication

Write operations (`POST`, `DELETE`) require a valid JWT Bearer token from Keycloak. Read operations (`GET`) are public.

The service validates tokens against a Keycloak realm using the OIDC extension (`quarkus-oidc`) in **service** (resource server) mode — it only validates incoming Bearer tokens and never redirects to a login page.

| Endpoint | Access |
|----------|--------|
| `GET /snippet` | Public |
| `POST /snippet` | Authenticated (Bearer token) |
| `DELETE /snippet/{id}` | Authenticated (Bearer token) |
| `/q/*` (health) | Public |
| `/swagger-ui`, `/openapi` | Public |

### Obtaining a Token

```bash
TOKEN=$(curl -s -X POST \
   "https://keycloak.dev.epcis.cloud/realms/openepcis/protocol/openid-connect/token" \
   -H "Content-Type: application/x-www-form-urlencoded" \
   -d "grant_type=client_credentials" \
   -d "client_id={client_id}" \
   -d "client_secret={client_secret}" \
   -d "username={username}" \
   -d "password={password}" \
   -d "scope=openid" | jq -r '.access_token')
echo "$TOKEN"
```

### Using the Token

```bash
curl -X POST http://localhost:8080/snippet \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{ ... }'
```

Swagger UI also provides an **Authorize** button to enter a Bearer token for testing protected endpoints.

### OIDC Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `QUARKUS_OIDC_AUTH_SERVER_URL` | `https://keycloak.dev.epcis.cloud/realms/openepcis` | Keycloak realm URL |
| `QUARKUS_OIDC_CLIENT_ID` | `backend-service` | OIDC client ID |

## Configuration

### Application Configuration

Configuration is in `src/main/resources/application.yaml`:

```yaml
quarkus:
  http:
    port: ${QUARKUS_HTTP_PORT:8080}
  opensearch:
    hosts: ${QUARKUS_OPENSEARCH_HOSTS:localhost:9200}
    protocol: ${QUARKUS_OPENSEARCH_PROTOCOL:http}
  oidc:
    auth-server-url: ${QUARKUS_OIDC_AUTH_SERVER_URL:https://keycloak.dev.epcis.cloud/realms/openepcis}
    client-id: ${QUARKUS_OIDC_CLIENT_ID:backend-service}
    application-type: service
```

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `QUARKUS_OPENSEARCH_HOSTS` | `localhost:9200` | OpenSearch host and port |
| `QUARKUS_OPENSEARCH_PROTOCOL` | `http` | Protocol (http or https) |
| `QUARKUS_OIDC_AUTH_SERVER_URL` | `https://keycloak.dev.epcis.cloud/realms/openepcis` | Keycloak realm URL |
| `QUARKUS_OIDC_CLIENT_ID` | `backend-service` | OIDC client ID |
| `QUARKUS_HTTP_PORT` | `8080` | HTTP listen port |

**Example: Using Custom OpenSearch Host**
```bash
export OPENSEARCH_HOST=my-opensearch-server:9200
export OPENSEARCH_PROTOCOL=https
mvn compile quarkus:dev
```

## Packaging and Deployment

### Build JAR

```bash
mvn package
java -jar target/quarkus-app/quarkus-run.jar
```

### Build Uber-JAR

```bash
mvn package -Dquarkus.package.type=uber-jar
java -jar target/*-runner.jar
```

### Build Native Executable

```bash
# With GraalVM installed
mvn package -Dnative

# Without GraalVM (uses Docker)
mvn package -Dnative -Dquarkus.native.container-build=true

# Run native executable
./target/openepcis-snippet-service-1.0.0-SNAPSHOT-runner
```

## Troubleshooting

### Connection Refused to OpenSearch

**Error:**
```
java.net.ConnectException: Connection refused
```

**Solutions:**
1. Ensure OpenSearch is running: `docker ps | grep opensearch`
2. Check OpenSearch is accessible: `curl http://localhost:9200`
3. If using Docker, ensure port 9200 is mapped correctly

### OpenSearch Authentication Error

**Error:**
```
org.opensearch.client.ResponseException: Unauthorized
```

**Solution:**
The application doesn't support authentication. Run OpenSearch with security disabled:
```bash
docker rm -f opensearch
docker run -d --name opensearch \
  -p 9200:9200 -p 9600:9600 \
  -e "discovery.type=single-node" \
  -e "DISABLE_SECURITY_PLUGIN=true" \
  opensearchproject/opensearch:latest
```

### Index Not Created

**Symptoms:**
- Search returns errors about missing index

**Solution:**
1. Check application logs for errors during startup
2. Verify OpenSearch connection: `curl http://localhost:9200/_cat/indices`
3. Restart the application after fixing OpenSearch connection

### Empty Search Results

**Symptoms:**
- Search returns `[]` even after adding snippets

**Solutions:**
1. Verify snippets were created successfully (POST should return 201)
2. Wait a moment for OpenSearch to index the document
3. Try searching without any search text: `curl http://localhost:8080/snippet`

### Port Already in Use

**Error:**
```
Port 8080 is already in use
```

**Solutions:**
1. Find and kill the process using the port:
   ```bash
   lsof -i :8080
   kill -9 <PID>
   ```
2. Or use a different port:
   ```bash
   mvn compile quarkus:dev -Dquarkus.http.port=8091
   ```

## Related Resources

- [Quarkus Documentation](https://quarkus.io/)
- [OpenSearch Documentation](https://opensearch.org/docs/latest/)
- [JSON Schema](https://json-schema.org/)

## License

This project is part of the OpenEPCIS community. For more information, visit [https://openepcis.io](https://openepcis.io).
