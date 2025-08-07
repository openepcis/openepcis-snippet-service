package io.openepcis.snippets.constants;

/**
 * Constants used throughout the OpenEPCIS Snippet Service.
 * This class centralizes all constant values to improve maintainability.
 */
public final class Constants {

    public static final String TEMPLATE_OPENEPCIS_SNIPPET_INDEX_TEMPLATE = "template/openepcis-snippet-index-template.json";
    public static final String INDEX_NAME = "snippets";
    public static final String TEMPLATE = "template";
    public static final String SETTINGS = "settings";
    public static final String MAPPINGS = "mappings";
    public static final int DEFAULT_LIMIT = 10;

    // Query related constants
    public static final String FUZZINESS_LEVEL = "2";
    public static final int PREFIX_LENGTH = 2;
    public static final String MINIMUM_SHOULD_MATCH = "1";
    public static final String SYNONYM_FILE_PATH = "synonyms/synonym-map.json";
    public static final String ID = "$id";
    public static final String ID_KEYWORD = "$id.keyword";
    public static final String TITLE = "title";
    public static final String DESCRIPTION = "description";
    public static final String SOURCE = "source";
    public static final String CREATED_AT = "createdAt";
}
