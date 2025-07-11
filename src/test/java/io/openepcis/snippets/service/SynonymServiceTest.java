package io.openepcis.snippets.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the SynonymService class.
 * These tests verify that the SynonymService correctly loads and retrieves synonyms
 * from the synonym-map.json file.
 */
public class SynonymServiceTest {

    private SynonymService synonymService;

    @BeforeEach
    public void setup() {
        synonymService = new SynonymService();
    }

    @Test
    public void testGetSynonymsForUri() {
        // Test with "uri" which is a key in the synonym-map.json
        Set<String> synonyms = synonymService.getSynonyms("uri");
        
        System.out.println("[DEBUG_LOG] Synonyms for 'uri': " + synonyms);
        
        // Verify that the synonyms include the expected values from synonym-map.json
        assertTrue(synonyms.contains("url"), "Synonyms should include 'url'");
        assertTrue(synonyms.contains("link"), "Synonyms should include 'link'");
        assertTrue(synonyms.contains("address"), "Synonyms should include 'address'");
        assertTrue(synonyms.contains("uniform resource identifier"), "Synonyms should include 'uniform resource identifier'");
        assertTrue(synonyms.contains("resource identifier"), "Synonyms should include 'resource identifier'");
        
        // Verify that the original term is included
        assertTrue(synonyms.contains("uri"), "Synonyms should include the original term 'uri'");
    }

    @Test
    public void testGetSynonymsForUrl() {
        // Test with "url" which is both a key and a value in the synonym-map.json
        Set<String> synonyms = synonymService.getSynonyms("url");
        
        System.out.println("[DEBUG_LOG] Synonyms for 'url': " + synonyms);
        
        // Verify that the synonyms include the expected values from synonym-map.json
        assertTrue(synonyms.contains("uri"), "Synonyms should include 'uri'");
        assertTrue(synonyms.contains("link"), "Synonyms should include 'link'");
        assertTrue(synonyms.contains("address"), "Synonyms should include 'address'");
        assertTrue(synonyms.contains("uniform resource locator"), "Synonyms should include 'uniform resource locator'");
        assertTrue(synonyms.contains("web address"), "Synonyms should include 'web address'");
        assertTrue(synonyms.contains("hyperlink"), "Synonyms should include 'hyperlink'");
        
        // Verify that the original term is included
        assertTrue(synonyms.contains("url"), "Synonyms should include the original term 'url'");
    }

    @Test
    public void testGetSynonymsForPharma() {
        // Test with "pharma" which is a key in the synonym-map.json
        Set<String> synonyms = synonymService.getSynonyms("pharma");
        
        System.out.println("[DEBUG_LOG] Synonyms for 'pharma': " + synonyms);
        
        // Verify that the synonyms include the expected values from synonym-map.json
        assertTrue(synonyms.contains("pharmaceutical"), "Synonyms should include 'pharmaceutical'");
        assertTrue(synonyms.contains("drug"), "Synonyms should include 'drug'");
        assertTrue(synonyms.contains("medicine"), "Synonyms should include 'medicine'");
        assertTrue(synonyms.contains("medication"), "Synonyms should include 'medication'");
        assertTrue(synonyms.contains("pharmacy"), "Synonyms should include 'pharmacy'");
        assertTrue(synonyms.contains("therapeutics"), "Synonyms should include 'therapeutics'");
        
        // Verify that the original term is included
        assertTrue(synonyms.contains("pharma"), "Synonyms should include the original term 'pharma'");
    }

    @Test
    public void testGetSynonymsForDrug() {
        // Test with "drug" which is a value in the synonym-map.json
        Set<String> synonyms = synonymService.getSynonyms("drug");
        
        System.out.println("[DEBUG_LOG] Synonyms for 'drug': " + synonyms);
        
        // Verify that the synonyms include the expected values from synonym-map.json
        assertTrue(synonyms.contains("pharma"), "Synonyms should include 'pharma'");
        assertTrue(synonyms.contains("pharmaceutical"), "Synonyms should include 'pharmaceutical'");
        assertTrue(synonyms.contains("medicine"), "Synonyms should include 'medicine'");
        assertTrue(synonyms.contains("medication"), "Synonyms should include 'medication'");
        assertTrue(synonyms.contains("pharmacy"), "Synonyms should include 'pharmacy'");
        assertTrue(synonyms.contains("therapeutics"), "Synonyms should include 'therapeutics'");
        
        // Verify that the original term is included
        assertTrue(synonyms.contains("drug"), "Synonyms should include the original term 'drug'");
    }

    @Test
    public void testGetSynonymsForRetail() {
        // Test with "retail" which is a key in the synonym-map.json
        Set<String> synonyms = synonymService.getSynonyms("retail");
        
        System.out.println("[DEBUG_LOG] Synonyms for 'retail': " + synonyms);
        
        // Verify that the synonyms include the expected values from synonym-map.json
        assertTrue(synonyms.contains("store"), "Synonyms should include 'store'");
        assertTrue(synonyms.contains("shop"), "Synonyms should include 'shop'");
        assertTrue(synonyms.contains("commerce"), "Synonyms should include 'commerce'");
        assertTrue(synonyms.contains("sales"), "Synonyms should include 'sales'");
        assertTrue(synonyms.contains("merchant"), "Synonyms should include 'merchant'");
        assertTrue(synonyms.contains("seller"), "Synonyms should include 'seller'");
        
        // Verify that the original term is included
        assertTrue(synonyms.contains("retail"), "Synonyms should include the original term 'retail'");
    }

    @Test
    public void testGetSynonymsForNonExistentTerm() {
        // Test with a term that doesn't exist in the synonym-map.json
        Set<String> synonyms = synonymService.getSynonyms("nonexistent");
        
        System.out.println("[DEBUG_LOG] Synonyms for 'nonexistent': " + synonyms);
        
        // Verify that only the original term is included
        assertEquals(1, synonyms.size(), "Should only contain the original term");
        assertTrue(synonyms.contains("nonexistent"), "Synonyms should include the original term 'nonexistent'");
    }

    @Test
    public void testGetSynonymsForShortTerm() {
        // Test with a term that is too short (less than 2 characters)
        Set<String> synonyms = synonymService.getSynonyms("a");
        
        System.out.println("[DEBUG_LOG] Synonyms for 'a': " + synonyms);
        
        // Verify that an empty set is returned
        assertTrue(synonyms.isEmpty(), "Should return an empty set for terms less than 2 characters");
    }

    @Test
    public void testGetSynonymsForNullTerm() {
        // Test with a null term
        Set<String> synonyms = synonymService.getSynonyms(null);
        
        System.out.println("[DEBUG_LOG] Synonyms for null: " + synonyms);
        
        // Verify that an empty set is returned
        assertTrue(synonyms.isEmpty(), "Should return an empty set for null terms");
    }
}