package com.soloproductions.wade.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soloproductions.wade.dto.DatasetDeleteRequest;
import com.soloproductions.wade.dto.DatasetPostRequest;
import com.soloproductions.wade.dto.DatasetPutRequest;
import com.soloproductions.wade.pojo.DataEntry;
import com.soloproductions.wade.entity.ClusterEntity;
import com.soloproductions.wade.repository.ClusterRepository;
import com.soloproductions.wade.service.DatasetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@SuppressWarnings("removal")
@WithMockUser(username = "testuser")
public class SqlClusterControllerTest
{

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ClusterRepository clusterRepository;

    @Autowired
    private DatasetService datasetService;

    @MockBean
    private com.soloproductions.wade.service.EmbeddedFusekiServer embeddedFusekiServer;

    private final String API_URL = "/api/{datasetName}/{datasetType}/{dataType}";
    private String DATASET_NAME;

    @BeforeEach
    void setUp()
    {
        datasetService.clearUserDatasets("testuser");
        clusterRepository.deleteAll();
        DATASET_NAME = "test-clusters-" + System.nanoTime();
    }

    private DatasetPostRequest createClusterRequest(String requestType, Object data)
    {
        DatasetPostRequest request = new DatasetPostRequest();
        request.setDatasetName(DATASET_NAME);
        request.setDataType("sql");
        request.setDatasetType("clusters");
        request.setRequestType(requestType);
        DataEntry entry = new DataEntry();
        entry.setData(data);
        request.setEntry(entry);
        return request;
    }

    @Test
    void testUpdateCluster_byID() throws Exception
    {
        // Create the dataset entry via API using hierarchical payload expected by SQL cluster create
        Map<String, Object> image = Map.of("name", "node1", "probability", 0.4, "uri", "http://example.com/node1.png");
        Map<String, Object> cluster = Map.of("name", "Cluster 0", "children", List.of(image));
        Map<String, Object> root = Map.of("children", List.of(cluster));
        DatasetPostRequest createReq = createClusterRequest("create", root);
        String createResp = mockMvc.perform(post(API_URL, DATASET_NAME, "clusters", "sql")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createReq)))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        long entityId = objectMapper.readTree(createResp).path("data").get(0).path("id").asLong();

        // Now, update it
        Map<String, Object> updatePayload = Map.of("id", entityId, "probability", 0.99);
        DatasetPutRequest request = new DatasetPutRequest();
        request.setDatasetName(DATASET_NAME);
        request.setDataType("sql");
        request.setDatasetType("clusters");
        request.setRequestType("update");
        DataEntry entry = new DataEntry();
        entry.setData(updatePayload);
        request.setEntry(entry);

        mockMvc.perform(put(API_URL, DATASET_NAME, "clusters", "sql")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.probability", is(0.99)));

        ClusterEntity updatedEntity = clusterRepository.findById(entityId).orElseThrow();
        assertEquals(0.99, updatedEntity.getProbability());
    }

    @Test
    void testDeleteCluster_byPayload() throws Exception
    {
        // Create via API using hierarchical payload
        Map<String, Object> delImage = Map.of("name", "toDelete", "Probability", 0.75, "URI", "http://example.com/toDelete.png");
        Map<String, Object> delCluster = Map.of("name", "Cluster 0", "children", List.of(delImage));
        Map<String, Object> delRoot = Map.of("children", List.of(delCluster));
        DatasetPostRequest createReq = createClusterRequest("create", delRoot);
        mockMvc.perform(post(API_URL, DATASET_NAME, "clusters", "sql")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createReq)))
            .andExpect(status().isOk());

        long initialCount = clusterRepository.count();
        assertTrue(initialCount > 0);

        // Now, delete it by payload
        Map<String, Object> node = Map.of("name", "toDelete", "parent", "Cluster 0");
        List<Map<String, Object>> data = List.of(node);
        DatasetDeleteRequest request = new DatasetDeleteRequest();
        request.setDatasetName(DATASET_NAME);
        request.setDataType("sql");
        request.setDatasetType("clusters");
        request.setRequestType("delete");
        DataEntry entry = new DataEntry();
        entry.setData(data);
        request.setEntry(entry);

        mockMvc.perform(delete(API_URL, DATASET_NAME, "clusters", "sql")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.nodeName", is("toDelete")));

        assertEquals(initialCount - 1, clusterRepository.count());
    }

    @Test
    void testDeleteBeforeCreate_ShouldReturnEmpty() throws Exception
    {
        Map<String, Object> node = Map.of("name", "non-existent", "parent", "none");
        List<Map<String, Object>> data = List.of(node);
        DatasetDeleteRequest request = new DatasetDeleteRequest();
        request.setDatasetName(DATASET_NAME);
        request.setDataType("sql");
        request.setDatasetType("clusters");
        request.setRequestType("delete");
        DataEntry entry = new DataEntry();
        entry.setData(data);
        request.setEntry(entry);

        mockMvc.perform(delete(API_URL, DATASET_NAME, "clusters", "sql")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").doesNotExist());

        assertEquals(0, clusterRepository.count());
    }

    @Test
    void testInvalidDataType_ShouldFail() throws Exception
    {
        List<Map<String, Object>> nodes = List.of(Map.of("name", "A", "probability", 0.5));
        DatasetPostRequest request = createClusterRequest("create", nodes);

        mockMvc.perform(post(API_URL, DATASET_NAME, "clusters", "invalid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("error")));
    }

    @Test
    void testReadClusters_Success() throws Exception
    {
        // Create entries via API using hierarchical payload
        Map<String, Object> img0 = Map.of("name", "Cluster 0", "Probability", 1.0, "URI", "http://example.com/cluster0.png");
        Map<String, Object> img1 = Map.of("name", "barn", "Probability", 0.835, "URI", "http://example.com/barn.png");
        Map<String, Object> img2 = Map.of("name", "castle", "Probability", 0.839, "URI", "http://example.com/castle.png");
        Map<String, Object> clusterMap = Map.of("name", "Cluster 0", "children", List.of(img0, img1, img2));
        Map<String, Object> rootMap = Map.of("children", List.of(clusterMap));
        DatasetPostRequest createReq = createClusterRequest("create", rootMap);
        mockMvc.perform(post(API_URL, DATASET_NAME, "clusters", "sql")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createReq)))
            .andExpect(status().isOk());

        // Verify repository contains created entries and that cluster children exist
        List<ClusterEntity> entities = clusterRepository.findByDatasetName(DATASET_NAME);
        assertTrue(entities.stream().anyMatch(e -> "barn".equals(e.getNodeName()) && "Cluster 0".equals(e.getClusterName())));
        assertTrue(entities.stream().anyMatch(e -> "castle".equals(e.getNodeName()) && "Cluster 0".equals(e.getClusterName())));
    }

    @Test
    void testCreateEmptyCluster_andThenAddLeaf() throws Exception
    {
        // Create an empty cluster (no children)
        Map<String, Object> clusterOnly = Map.of("name", "Cluster A");
        Map<String, Object> root = Map.of("children", List.of(clusterOnly));
        DatasetPostRequest createReq = createClusterRequest("create", root);
        mockMvc.perform(post(API_URL, DATASET_NAME, "clusters", "sql")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createReq)))
            .andExpect(status().isOk());

        // Verify cluster root present (nodeName == Cluster A)
        List<ClusterEntity> entities = clusterRepository.findByDatasetName(DATASET_NAME);
        assertTrue(entities.stream().anyMatch(e -> "Cluster A".equals(e.getNodeName())));

        // Now create a leaf node parented to Cluster A using flat payload
        Map<String, Object> leaf = Map.of("name", "leaf1", "parent", "Cluster A", "Probability", 0.42, "URI", "http://example.com/leaf1.png");
        List<Map<String, Object>> data = List.of(leaf);
        DatasetPostRequest leafReq = createClusterRequest("create", data);
        mockMvc.perform(post(API_URL, DATASET_NAME, "clusters", "sql")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(leafReq)))
            .andExpect(status().isOk());

        List<ClusterEntity> all = clusterRepository.findByDatasetName(DATASET_NAME);
        assertTrue(all.stream().anyMatch(e -> "leaf1".equals(e.getNodeName()) && "Cluster A".equals(e.getClusterName())));
    }

    @Test
    void testCreateDuplicateClustersInPayload_ShouldFail() throws Exception
    {
        // Two clusters with the same name in the payload
        Map<String, Object> c1 = Map.of("name", "DupCluster");
        Map<String, Object> c2 = Map.of("name", "DupCluster");
        Map<String, Object> root = Map.of("children", List.of(c1, c2));
        DatasetPostRequest createReq = createClusterRequest("create", root);

        mockMvc.perform(post(API_URL, DATASET_NAME, "clusters", "sql")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createReq)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status", is("error")));
    }

    @Test
    void testCreateDuplicateAgainstDB_ShouldSucceed_Upsert() throws Exception
    {
        // Create a cluster first
        Map<String, Object> clusterOnly = Map.of("name", "ExistingCluster");
        Map<String, Object> root = Map.of("children", List.of(clusterOnly));
        DatasetPostRequest createReq = createClusterRequest("create", root);
        mockMvc.perform(post(API_URL, DATASET_NAME, "clusters", "sql")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createReq)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status", is("success")));

        // Attempt to create the same cluster again via flat payload - should succeed (upsert)
        List<Map<String, Object>> nodes = List.of(Map.of("name", "ExistingCluster", "parent", "root"));
        DatasetPostRequest dupReq = createClusterRequest("create", nodes);
        mockMvc.perform(post(API_URL, DATASET_NAME, "clusters", "sql")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dupReq)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status", is("success")));
    }
}