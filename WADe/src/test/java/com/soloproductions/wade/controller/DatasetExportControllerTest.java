package com.soloproductions.wade.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soloproductions.wade.dto.DatasetPostRequest;
import com.soloproductions.wade.entity.HeatmapEntity;
import com.soloproductions.wade.entity.ClusterEntity;
import com.soloproductions.wade.pojo.DataEntry;
import com.soloproductions.wade.repository.HeatmapRepository;
import com.soloproductions.wade.repository.ClusterRepository;
import com.soloproductions.wade.service.DatasetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

/**
 * Tests the export functionality for all three data type controllers:
 * - JsonController
 * - RdfController
 * - SqlController
 * <p>
 * Tests both honorFilters=true and honorFilters=false scenarios.
 */
@SpringBootTest
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@Transactional
@WithMockUser(username = "testuser")
public class DatasetExportControllerTest
{
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private HeatmapRepository heatmapRepository;

    @Autowired
    private ClusterRepository clusterRepository;

    @Autowired
    private DatasetService datasetService;

    private final String EXPORT_URL = "/api/{datasetName}/{datasetType}/{dataType}";
    private String SQL_DATASET_NAME;

    @BeforeEach
    void setUp()
    {
        datasetService.clearUserDatasets("testuser");
        heatmapRepository.deleteAll();
        clusterRepository.deleteAll();
        SQL_DATASET_NAME = "test-export-" + System.nanoTime();
    }

    // ========================================
    // JSON Controller Export Tests
    // ========================================

    @Test
    void testJsonExport_Heatmap_NoFilters() throws Exception
    {
        // Test export of JSON heatmap data without filters
        MvcResult result = mockMvc.perform(get(EXPORT_URL, "bsds300", "heatmap", "json")
                        .param("export", "true")
                        .param("honorFilters", "false"))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();

        // Verify it's valid JSON
        assertNotNull(responseBody);
        assertTrue(responseBody.contains("objects") || responseBody.contains("matrix"),
                "Response should contain JSON heatmap data");
    }

    @Test
    void testJsonExport_Heatmap_WithFilters() throws Exception
    {
        // Test export with filters (range, rangeStart, sort)
        MvcResult result = mockMvc.perform(get(EXPORT_URL, "bsds300", "heatmap", "json")
                        .param("export", "true")
                        .param("honorFilters", "true")
                        .param("mode", "similarity")
                        .param("range", "5")
                        .param("rangeStart", "0")
                        .param("similaritySortCriteria", "strongest_pair")
                        .param("sortDirection", "highest_probability"))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();

        // Verify it's valid JSON and contains filtered data
        assertNotNull(responseBody);
        assertTrue(responseBody.length() > 0, "Filtered export should contain data");
    }

    @Test
    void testJsonExport_Clusters_NoFilters() throws Exception
    {
        // Test export of JSON cluster data without filters
        MvcResult result = mockMvc.perform(get(EXPORT_URL, "bsds300", "clusters", "json")
                        .param("export", "true")
                        .param("honorFilters", "false"))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();

        // Verify it's valid JSON cluster data
        assertNotNull(responseBody);
        assertTrue(responseBody.contains("name") || responseBody.contains("children"),
                "Response should contain JSON cluster data");
    }

    // ========================================
    // RDF Controller Export Tests
    // ========================================

    @Test
    void testRdfExport_Heatmap_NoFilters() throws Exception
    {
        // Test export of RDF/TTL heatmap data without filters
        MvcResult result = mockMvc.perform(get(EXPORT_URL, "bsds300", "heatmap", "rdf")
                        .param("export", "true")
                        .param("honorFilters", "false"))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();

        // Verify it's TTL format
        assertNotNull(responseBody);
        assertTrue(responseBody.contains("@prefix") || responseBody.contains("<http://"),
                "Response should contain TTL/RDF data");
    }

    @Test
    void testRdfExport_Heatmap_WithFilters() throws Exception
    {
        // Test export with filters
        MvcResult result = mockMvc.perform(get(EXPORT_URL, "bsds300", "heatmap", "rdf")
                        .param("export", "true")
                        .param("honorFilters", "true")
                        .param("range", "10")
                        .param("rangeStart", "0")
                        .param("similaritySortCriteria", "average_similarity")
                        .param("sortDirection", "highest_probability"))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();

        // When honorFilters is true, RDF controller returns JSON of filtered results
        assertNotNull(responseBody);
        assertTrue(responseBody.length() > 0, "Filtered export should contain data");
    }

    @Test
    void testRdfExport_Clusters_NoFilters() throws Exception
    {
        // Test export of RDF cluster data without filters
        MvcResult result = mockMvc.perform(get(EXPORT_URL, "bsds300", "clusters", "rdf")
                        .param("export", "true")
                        .param("honorFilters", "false"))
                .andExpect(status().isOk())
                .andReturn();

        byte[] responseBytes = result.getResponse().getContentAsByteArray();
        assertTrue(responseBytes.length > 0, "Export response should not be empty");

        // Parse only a small preview to avoid allocating a large String for huge TTL exports.
        String preview = new String(responseBytes, 0, Math.min(responseBytes.length, 512), StandardCharsets.UTF_8);

        // Verify it's TTL format
        assertTrue(preview.contains("@prefix") || preview.contains("<http://"),
                "Response should contain TTL/RDF cluster data");
    }

    // ========================================
    // SQL Controller Export Tests
    // ========================================

    @Test
    void testSqlExport_Heatmap_NoFilters() throws Exception
    {
        // Create test heatmap data in SQL
        createSqlHeatmapData();

        MvcResult result = mockMvc.perform(get(EXPORT_URL, SQL_DATASET_NAME, "heatmap", "sql")
                        .param("export", "true")
                        .param("honorFilters", "false"))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();

        // Verify it's SQL dump format
        assertNotNull(responseBody);
        assertTrue(responseBody.contains("INSERT INTO") || responseBody.contains("heatmap_entity"),
                "Response should contain SQL dump");
        assertTrue(responseBody.contains(SQL_DATASET_NAME),
                "SQL dump should contain dataset name");
    }

    @Test
    void testSqlExport_Heatmap_WithFilters() throws Exception
    {
        // Create test heatmap data in SQL
        createSqlHeatmapData();

        MvcResult result = mockMvc.perform(get(EXPORT_URL, SQL_DATASET_NAME, "heatmap", "sql")
                        .param("export", "true")
                        .param("honorFilters", "true")
                        .param("range", "2")
                        .param("rangeStart", "0")
                        .param("similaritySortCriteria", "strongest_pair")
                        .param("sortDirection", "highest_probability"))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();

        // When honorFilters is true, should return JSON of filtered results
        assertNotNull(responseBody);
        assertTrue(responseBody.length() > 0, "Filtered export should contain data");
    }

    @Test
    void testSqlExport_Clusters_NoFilters() throws Exception
    {
        // Create test cluster data in SQL
        createSqlClusterData();

        MvcResult result = mockMvc.perform(get(EXPORT_URL, SQL_DATASET_NAME, "clusters", "sql")
                        .param("export", "true")
                        .param("honorFilters", "false"))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();

        // Verify it's SQL dump format
        assertNotNull(responseBody);
        assertTrue(responseBody.contains("INSERT INTO") || responseBody.contains("cluster_entity"),
                "Response should contain SQL dump for clusters");
        assertTrue(responseBody.contains(SQL_DATASET_NAME),
                "SQL dump should contain dataset name");
    }

    @Test
    void testSqlExport_VerifyPostgresDumpFormat() throws Exception
    {
        // Create test data
        createSqlHeatmapData();

        MvcResult result = mockMvc.perform(get(EXPORT_URL, SQL_DATASET_NAME, "heatmap", "sql")
                        .param("export", "true")
                        .param("honorFilters", "false"))
                .andExpect(status().isOk())
                .andReturn();

        String sqlDump = result.getResponse().getContentAsString();

        // Verify PostgreSQL dump structure
        assertTrue(sqlDump.contains("-- PostgreSQL dump"), "Should have PostgreSQL dump header");
        assertTrue(sqlDump.contains("INSERT INTO heatmap_entity"), "Should have INSERT statement");
        assertTrue(sqlDump.contains(SQL_DATASET_NAME), "Should include dataset name");
        assertTrue(sqlDump.contains("VALUES"), "Should have VALUES clause");
        assertTrue(sqlDump.contains(";"), "Should have proper SQL termination");

        // Verify data integrity - check for test objects
        assertTrue(sqlDump.contains("'ObjectA'") || sqlDump.contains("ObjectA"),
                "Should contain test object names");
    }

    // ========================================
    // Helper Methods
    // ========================================

    private void createSqlHeatmapData() throws Exception
    {
        // Create test heatmap entries via API to ensure dataset is registered
        Map<String, Object> pair1 = Map.of("object1", "ObjectA", "object2", "ObjectB", "similarity", 0.8);
        Map<String, Object> pair2 = Map.of("object1", "ObjectC", "object2", "ObjectD", "similarity", 0.6);
        Map<String, Object> data = Map.of("pairs", List.of(pair1, pair2));

        DatasetPostRequest request = new DatasetPostRequest();
        request.setDatasetName(SQL_DATASET_NAME);
        request.setDataType("sql");
        request.setDatasetType("heatmap");
        request.setRequestType("post");
        DataEntry entry = new DataEntry();
        entry.setData(data);
        request.setEntry(entry);

        mockMvc.perform(post("/api/{datasetName}/{datasetType}/{dataType}",
                        SQL_DATASET_NAME, "heatmap", "sql")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    private void createSqlClusterData() throws Exception
    {
        // Create test cluster entries via API to ensure dataset is registered
        Map<String, Object> image1 = Map.of("name", "ImageA", "probability", 0.9, "uri", "http://example.org/imageA");
        Map<String, Object> image2 = Map.of("name", "ImageB", "probability", 0.7, "uri", "http://example.org/imageB");

        Map<String, Object> cluster1 = Map.of("name", "Cluster1", "children", List.of(image1));
        Map<String, Object> cluster2 = Map.of("name", "Cluster2", "children", List.of(image2));

        Map<String, Object> root = Map.of("children", List.of(cluster1, cluster2));

        DatasetPostRequest request = new DatasetPostRequest();
        request.setDatasetName(SQL_DATASET_NAME);
        request.setDataType("sql");
        request.setDatasetType("clusters");
        request.setRequestType("post");
        DataEntry entry = new DataEntry();
        entry.setData(root);
        request.setEntry(entry);

        mockMvc.perform(post("/api/{datasetName}/{datasetType}/{dataType}", SQL_DATASET_NAME, "clusters", "sql")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));
    }
}
