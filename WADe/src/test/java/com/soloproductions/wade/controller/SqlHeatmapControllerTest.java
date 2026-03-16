package com.soloproductions.wade.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soloproductions.wade.dto.DatasetPostRequest;
import com.soloproductions.wade.pojo.DataEntry;
import com.soloproductions.wade.entity.HeatmapEntity;
import com.soloproductions.wade.repository.HeatmapRepository;
import com.soloproductions.wade.service.DatasetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser(username = "testuser")
public class SqlHeatmapControllerTest
{

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private HeatmapRepository heatmapRepository;

    @Autowired
    private DatasetService datasetService;

    private final String API_URL = "/api/{datasetName}/{datasetType}/{dataType}";
    private String DATASET_NAME;

    @BeforeEach
    void setUp()
    {
        datasetService.clearUserDatasets("testuser");
        heatmapRepository.deleteAll();
        DATASET_NAME = "test-heatmap-" + System.nanoTime();
    }

    private DatasetPostRequest createHeatmapRequest(String requestType, Object data)
    {
        DatasetPostRequest request = new DatasetPostRequest();
        request.setDatasetName(DATASET_NAME);
        request.setDataType("sql");
        request.setDatasetType("heatmap");
        request.setRequestType(requestType);
        DataEntry entry = new DataEntry();
        entry.setData(data);
        request.setEntry(entry);
        return request;
    }

    @Test
    void testCreateHeatmap_Success() throws Exception
    {
        Map<String, Object> pair = Map.of("object1", "A", "object2", "B", "similarity", 0.5);
        Map<String, Object> data = Map.of("pairs", List.of(pair));
        DatasetPostRequest request = createHeatmapRequest("create", data);

        var result = mockMvc.perform(post(API_URL, DATASET_NAME, "heatmap", "sql")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn();

        String response = result.getResponse().getContentAsString();
        int status = result.getResponse().getStatus();

        if (status != 200 || response.isEmpty())
        {
            throw new AssertionError("Status: " + status + ", Response: " + response);
        }

        // Now try the jsonPath
        try
        {
            mockMvc.perform(post(API_URL, DATASET_NAME, "heatmap", "sql")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(1)));
        }
        catch (Exception e)
        {
            throw new AssertionError("JsonPath failed: " + e.getMessage() + ", Response: " + response);
        }

        List<HeatmapEntity> entities = heatmapRepository.findAll();
        assertEquals(1, entities.size());
        assertEquals("A", entities.get(0).getObject1());
        assertEquals("B", entities.get(0).getObject2());
        assertEquals(0.5, entities.get(0).getSimilarity());
    }

    @Test
    void testUpdateHeatmap_UpsertOnCreate() throws Exception
    {
        // First creation
        Map<String, Object> pair1 = Map.of("object1", "A", "object2", "B", "similarity", 0.5);
        Map<String, Object> data1 = Map.of("pairs", List.of(pair1));
        DatasetPostRequest request1 = createHeatmapRequest("create", data1);

        mockMvc.perform(post(API_URL, DATASET_NAME, "heatmap", "sql")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isOk());

        assertEquals(1, heatmapRepository.count());

        // Second creation of the same pair should update (upsert)
        Map<String, Object> pair2 = Map.of("object1", "B", "object2", "A", "similarity", 0.9);
        Map<String, Object> data2 = Map.of("pairs", List.of(pair2));
        DatasetPostRequest request2 = createHeatmapRequest("create", data2);

        mockMvc.perform(post(API_URL, DATASET_NAME, "heatmap", "sql")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].similarity", is(0.9)));

        List<HeatmapEntity> entities = heatmapRepository.findAll();
        assertEquals(1, entities.size());
        assertEquals(0.9, entities.get(0).getSimilarity());
    }

    @Test
    void testDeleteHeatmap_byPayload() throws Exception
    {
        // ensure dataset exists via API create (registers dataset in service)
        Map<String, Object> regPair = Map.of("object1", "X", "object2", "Y", "similarity", 0.75);
        Map<String, Object> regData = Map.of("pairs", List.of(regPair));
        DatasetPostRequest regRequest = createHeatmapRequest("create", regData);
        mockMvc.perform(post(API_URL, DATASET_NAME, "heatmap", "sql")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(regRequest)))
            .andExpect(status().isOk());

        // Now delete it by payload (entity was created via POST above)
        Map<String, Object> pair = Map.of("object1", "X", "object2", "Y");
        Map<String, Object> data = Map.of("pairs", List.of(pair));
        DatasetPostRequest request = createHeatmapRequest("delete", data);

        var delResult = mockMvc.perform(delete(API_URL, DATASET_NAME, "heatmap", "sql")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andReturn();

        String delResp = delResult.getResponse().getContentAsString();
        int delStatus = delResult.getResponse().getStatus();
        if (delStatus != 200)
        {
            throw new AssertionError("Delete response status: " + delStatus + ", body: " + delResp);
        }
        System.out.println("DELETE response body: " + delResp);
        assertTrue(delResp.contains("object1") || delResp.contains("error"), "Unexpected delete response: " + delResp);
        assertEquals(0, heatmapRepository.count());
    }

    @Test
    void testUpdateHeatmap_byID() throws Exception
    {
        // Create the entity through the API (registers dataset and persists entity)
        Map<String, Object> regPair = Map.of("object1", "C", "object2", "D", "similarity", 0.1);
        Map<String, Object> regData = Map.of("pairs", List.of(regPair));
        DatasetPostRequest regRequest = createHeatmapRequest("create", regData);

        var createResult = mockMvc.perform(post(API_URL, DATASET_NAME, "heatmap", "sql")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regRequest)))
                .andReturn();

        String createResp = createResult.getResponse().getContentAsString();
        int createStatus = createResult.getResponse().getStatus();
        if (createStatus != 200 || createResp.isEmpty()) {
            throw new AssertionError("Create failed. Status: " + createStatus + ", body: " + createResp);
        }

        // Extract created entity id from response.data[0].id
        Map<?,?> parsed = objectMapper.readValue(createResp, Map.class);
        Object dataObj = parsed.get("data");
        long entityId = -1L;
        if (dataObj instanceof List) {
            List<?> dataList = (List<?>) dataObj;
            if (!dataList.isEmpty() && dataList.get(0) instanceof Map) {
                Map<?,?> first = (Map<?,?>) dataList.get(0);
                Object idObj = first.get("id");
                if (idObj instanceof Number) entityId = ((Number) idObj).longValue();
            }
        }
        if (entityId < 0) throw new AssertionError("Could not extract created entity id from response: " + createResp);

        // Now, update it
        Map<String, Object> updatePayload = Map.of("id", entityId, "similarity", 0.99);
        DatasetPostRequest request = createHeatmapRequest("update", updatePayload);

        var putResult = mockMvc.perform(put(API_URL, DATASET_NAME, "heatmap", "sql")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andReturn();

        String putResp = putResult.getResponse().getContentAsString();
        int putStatus = putResult.getResponse().getStatus();
        if (putStatus != 200)
        {
            throw new AssertionError("Put response status: " + putStatus + ", body: " + putResp);
        }
        System.out.println("PUT response body: " + putResp);
        assertTrue(putResp.contains("similarity") || putResp.contains("error"), "Unexpected put response: " + putResp);

        HeatmapEntity updatedEntity = heatmapRepository.findById(entityId).orElseThrow();
        assertEquals(0.99, updatedEntity.getSimilarity());
    }

    @Test
    void testDeleteBeforeCreate_ShouldReturnEmpty() throws Exception
    {
        Map<String, Object> pair = Map.of("object1", "non-existent", "object2", "pair");
        Map<String, Object> data = Map.of("pairs", List.of(pair));
        DatasetPostRequest request = createHeatmapRequest("delete", data);

        mockMvc.perform(delete(API_URL, DATASET_NAME, "heatmap", "sql")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").doesNotExist());

        assertEquals(0, heatmapRepository.count());
    }

    @Test
    void testInvalidDataType_ShouldFail() throws Exception
    {
        Map<String, Object> pair = Map.of("object1", "A", "object2", "B", "similarity", 0.5);
        Map<String, Object> data = Map.of("pairs", List.of(pair));
        DatasetPostRequest request = createHeatmapRequest("create", data);
        request.setDataType("sql"); // Keep valid in request

        mockMvc.perform(post(API_URL, DATASET_NAME, "heatmap", "invalid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("error")));
    }
}
