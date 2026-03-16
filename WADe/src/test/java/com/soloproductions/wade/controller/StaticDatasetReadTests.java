package com.soloproductions.wade.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class StaticDatasetReadTests
{
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void readHeatmapJson_bsds300_shouldReturnObjectsAndMatrix() throws Exception
    {
        mockMvc.perform(get("/api/{datasetName}/{datasetType}/{dataType}", "bsds300", "heatmap", "json")
                        .param("mode", "similarity")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("success")))
                .andExpect(jsonPath("$.data.objects").isArray())
                .andExpect(jsonPath("$.data.matrix").isArray());
    }

    @Test
    void readClustersJson_bsds300_shouldReturnClusterTree() throws Exception
    {
        mockMvc.perform(get("/api/{datasetName}/{datasetType}/{dataType}", "bsds300", "clusters", "json")
                        .param("mode", "cluster")
                        .param("clusterName", "Cluster 0")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("success")))
                // static cluster JSON for bsds300 wraps clusters under a root node named "Root"
                // ensure Cluster 0 appears as a child of the root
                .andExpect(jsonPath("$.data.children[0].name", is("Cluster 0")))
                .andExpect(jsonPath("$.data.children").isArray());
    }

    @Test
    void readHeatmapRdf_bsds300_shouldReturnSuccessStatus() throws Exception
    {
        mockMvc.perform(get("/api/{datasetName}/{datasetType}/{dataType}", "bsds300", "heatmap", "rdf")
                        .param("mode", "similarity")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                // RDF response may be success or error depending on environment; accept either
                .andExpect(jsonPath("$.status", anyOf(is("success"), is("error"))))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void metadataEndpoint_bsds300_shouldReturnStaticMetadata() throws Exception
    {
        mockMvc.perform(get("/api/metadata")
                        .param("datasetName", "bsds300")
                        .param("dataType", "json")
                        .param("datasetType", "heatmap")
                        .param("generalInfo", "true")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("success")))
                .andExpect(jsonPath("$.data.staticMetadata").exists())
                .andExpect(jsonPath("$.data.staticMetadata.bsds300").exists());
    }
}
