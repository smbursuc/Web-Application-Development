package com.soloproductions.wade.metadata;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soloproductions.wade.service.DatasetService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username = "testuser")
public class DynamicDatasetMetadataTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DatasetService datasetService;

    @BeforeEach
    void setup() {
        datasetService.clearUserDatasets("testuser");
    }

    @Test
    void createSqlDataset_thenFetchMetadata_shouldReturnStaticMetadataForNewDataset() throws Exception {
        String datasetName = "test-sql-dataset-" + System.nanoTime();

        // create a SQL heatmap dataset via controller (POST to the dataset API)
        // send a minimal payload that creates one pair so the dataset is registered
        java.util.Map<String, Object> pair = java.util.Map.of("object1", "A", "object2", "B", "similarity", 0.5);
        java.util.Map<String, Object> data = java.util.Map.of("pairs", java.util.List.of(pair));
        java.util.Map<String, Object> entry = java.util.Map.of("data", data);
        java.util.Map<String, Object> payloadMap = new java.util.HashMap<>();
        payloadMap.put("datasetName", datasetName);
        payloadMap.put("dataType", "sql");
        payloadMap.put("datasetType", "heatmap");
        payloadMap.put("requestType", "create");
        payloadMap.put("entry", entry);
        String payload = objectMapper.writeValueAsString(payloadMap);

        mockMvc.perform(post("/api/{datasetName}/{datasetType}/{dataType}", datasetName, "heatmap", "sql")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isOk());

        // now request metadata with generalInfo=true — dynamic dataset should appear in staticMetadata
        mockMvc.perform(get("/api/metadata")
                        .param("datasetName", datasetName)
                        .param("dataType", "sql")
                        .param("datasetType", "heatmap")
                        .param("generalInfo", "true")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("success")))
                .andExpect(jsonPath("$.data.staticMetadata['" + datasetName + "']", notNullValue()));
    }
}
