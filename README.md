# OpenEPCIS Snippet Service

This is a REST service built with Quarkus that provides APIs for storing and retrieving JSON schema snippets using OpenSearch. The service allows you to save JSON schema snippets and search them based on their title and description fields.

This project uses Quarkus, the Supersonic Subatomic Java Framework.

If you want to learn more about Quarkus, please visit its website: https://quarkus.io/ .

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:
```shell script
mvn compile quarkus:dev
```

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at http://localhost:8080/q/dev/.

## Packaging and running the application

The application can be packaged using:
```shell script
mvn package
```
It produces the `quarkus-run.jar` file in the `target/quarkus-app/` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `target/quarkus-app/lib/` directory.

The application is now runnable using `java -jar target/quarkus-app/quarkus-run.jar`.

If you want to build an _über-jar_, execute the following command:
```shell script
mvn package -Dquarkus.package.type=uber-jar
```

The application, packaged as an _über-jar_, is now runnable using `java -jar target/*-runner.jar`.

## Creating a native executable

You can create a native executable using: 
```shell script
mvn package -Dnative
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using: 
```shell script
mvn package -Dnative -Dquarkus.native.container-build=true
```

You can then execute your native executable with: `./target/openepcis-snippet-service-1.0.0-SNAPSHOT-runner`

If you want to learn more about building native executables, please consult https://quarkus.io/guides/maven-tooling.

## API Documentation

### API: Save Snippet

- **Method**: POST
- **Path**: /snippet
- **Description**: Saves a JSON schema snippet to OpenSearch
- **Request Body**: JSON schema snippet
- **Response**: The saved snippet with HTTP 201 Created status

#### Example Request:
```json
{
    "$id": "https://openepcis.github.io/openepcis-event-sentry/json-schema-epcis-snippets/epc-uri-cpi-0.1.0.json",
    "$schema": "http://json-schema.org/draft-07/schema#",
    "description": "Specifies value for the epcList, childEPCs, inputEPCList, outputEPCList, or parentID field, expressed as a Component / Part Identifier (CPI) in EPC URI (URN) format, relevant for automotive and technical industries.",
    "title": "CPI EPC URI",
    "definitions": {
        "epc-uri-cpi": {
            "type": "string",
            "pattern": "^urn:epc:id:cpi:((\\d{6}\\.(\\%2[3dfDF]|\\%3[0-9]|\\%4[1-9A-Fa-f]|\\%5[0-9Aa]|[0-9A-Z-]){1,24})|(\\d{7}\\.(\\%2[3dfDF]|\\%3[0-9]|\\%4[1-9A-Fa-f]|\\%5[0-9Aa]|[0-9A-Z-]){1,23})|(\\d{8}\\.(\\%2[3dfDF]|\\%3[0-9]|\\%4[1-9A-Fa-f]|\\%5[0-9Aa]|[0-9A-Z-]){1,22})|(\\d{9}\\.(\\%2[3dfDF]|\\%3[0-9]|\\%4[1-9A-Fa-f]|\\%5[0-9Aa]|[0-9A-Z-]){1,21})|(\\d{10}\\.(\\%2[3dfDF]|\\%3[0-9]|\\%4[1-9A-Fa-f]|\\%5[0-9Aa]|[0-9A-Z-]){1,20})|(\\d{11}\\.(\\%2[3dfDF]|\\%3[0-9]|\\%4[1-9A-Fa-f]|\\%5[0-9Aa]|[0-9A-Z-]){1,19})|(\\d{12}\\.(\\%2[3dfDF]|\\%3[0-9]|\\%4[1-9A-Fa-f]|\\%5[0-9Aa]|[0-9A-Z-]){1,18}))\\.[\\d]{1,12}$"
        }
    }
}
```

### API: Get Snippets

- **Method**: GET
- **Path**: /snippet
- **Description**: Retrieves the latest 10 snippets from OpenSearch
- **Query Parameters**: 
  - `searchText` (optional): Text to search in title and description fields
- **Response**: List of snippets matching the search criteria

#### Example Request:
```
GET /snippet?searchText=CPI
```

## Prerequisites

- Java 21 or higher
- Maven 3.8.1 or higher
- OpenSearch instance running on localhost:9200 (configurable in application.yaml)

## OpenAPI Documentation

This service includes comprehensive OpenAPI documentation. When the application is running, you can access the documentation at:

- OpenAPI Specification: http://localhost:8090/openapi
- Interactive API Documentation: http://localhost:8090/swagger-ui

The documentation provides detailed information about the available endpoints, request/response schemas, and example values.

## Related Guides

- REST Jackson ([guide](https://quarkus.io/guides/rest#json-serialisation)): Jackson serialization support for Quarkus REST. This extension is not compatible with the quarkus-resteasy extension, or any of the extensions that depend on it
- YAML Configuration ([guide](https://quarkus.io/guides/config-yaml)): Use YAML to configure your Quarkus application

## Provided Code

### YAML Config

Configure your application with YAML

[Related guide section...](https://quarkus.io/guides/config-reference#configuration-examples)

The Quarkus application configuration is located in `src/main/resources/application.yml`.

### REST

Easily start your REST Web Services

[Related guide section...](https://quarkus.io/guides/getting-started-reactive#reactive-jax-rs-resources)
