package com.soloproductions.wade.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soloproductions.wade.dataset.DataType;
import com.soloproductions.wade.dataset.DatasetData;
import com.soloproductions.wade.dataset.DatasetType;
import com.soloproductions.wade.service.DatasetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username = "testuser")
public class DatasetValidationControllerTest
{
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DatasetService datasetService;

    @BeforeEach
    void setUp()
    {
        datasetService.clearUserDatasets("testuser");
    }

    @Test
    public void nonExistentDataset_returnsError() throws Exception
    {
        mockMvc.perform(get("/api/{datasetName}/{datasetType}/{dataType}", "nope", "clusters", "json")
                        .param("mode", "cluster")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("error")))
                .andExpect(jsonPath("$.message", containsString("does not exist")));
    }

    @Test
    public void invalidDatasetType_messageShowsAllowed() throws Exception
    {
        // bsds300 is a predefined dataset name so name validation passes
        mockMvc.perform(get("/api/{datasetName}/{datasetType}/{dataType}", "bsds300", "badtype", "json")
                        .param("mode", "cluster")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("error")))
                .andExpect(jsonPath("$.message", containsString("invalid")));
    }

    @Test
    public void missingDataTypeForDynamicDataset_returnsAvailableTypes() throws Exception
    {
        // create a dynamic dataset with only JSON for CLUSTERS via POST to the creation endpoint
        mockMvc.perform(post("/api/{datasetName}/{datasetType}/{dataType}", "dyn2", "clusters", "sql")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("success")));

        // now request with a missing data type (rdf) to trigger validation error and list available types
        mockMvc.perform(get("/api/{datasetName}/{datasetType}/{dataType}", "dyn2", "clusters", "rdf")
                        .param("mode", "cluster")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("error")))
                .andExpect(jsonPath("$.message", containsString("Available types")));
    }
}
