package com.soloproductions.wade.metadata;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username = "testuser")
public class MetadataValidationTest
{
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testStaticDatasetDoesNotInheritCommonDataModels() throws IOException
    {
        DatasetMetadata metadata = new DatasetMetadata();
        com.soloproductions.wade.dataset.AbstractDatasetData.setMetadataStatic(metadata);

        StaticDatasetMetadata common = metadata.getStaticMetadata().get("common");
        assertNotNull(common);
        assertNotNull(common.getDataModels());
        boolean commonHasSql = common.getDataModels().stream().anyMatch(ft -> "sql".equals(ft.getName()));
        assertTrue(commonHasSql, "common should contain sql data model");

        StaticDatasetMetadata bsds = metadata.getStaticMetadata().get("bsds300");
        assertNotNull(bsds);
        List<com.soloproductions.wade.metadata.FeatureTuple> bsdsModels = bsds.getDataModels();
        if (bsdsModels != null)
        {
            boolean bsdsHasSql = bsdsModels.stream().anyMatch(ft -> "sql".equals(ft.getName()));
            assertFalse(bsdsHasSql, "bsds300 must not contain sql in its static dataModels");
        }
    }

    @Test
    void testValidateDataTypeStaticRejectsExtraModel() throws Exception
    {
        // ensure bsds300 JSON dataset is initialized by requesting it (GET will lazily load static dataset)
        var getInit = mockMvc.perform(get("/api/{datasetName}/{datasetType}/{dataType}", "bsds300", "heatmap", "json")
                .param("mode", "similarity")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andReturn();
        String initContent = getInit.getResponse().getContentAsString();
        System.out.println("GET bsds300/json init response: " + initContent);

        // requesting an unsupported data type (sql) should return an error with available models
        mockMvc.perform(get("/api/{datasetName}/{datasetType}/{dataType}", "bsds300", "heatmap", "sql")
                        .param("mode", "similarity")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message", containsString("Available")));
    }

    @Test
    void testValidateDataTypeDynamicRejectsUnknownDataType() throws Exception
    {
        String dynamic = "mdt-dyn-1";

        // create a dynamic dataset with only SQL for HEATMAP via POST (SQL controller supports create)
        var postResult2 = mockMvc.perform(post("/api/{datasetName}/{datasetType}/{dataType}", dynamic, "heatmap", "sql")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isOk())
            .andReturn();
        String postContent2 = postResult2.getResponse().getContentAsString();
        System.out.println("POST " + dynamic + " response: " + postContent2);
        org.assertj.core.api.Assertions.assertThat(postContent2).contains("\"status\":\"success\"");

        // now request an unsupported type (json) and expect an error listing available models
        mockMvc.perform(get("/api/{datasetName}/{datasetType}/{dataType}", dynamic, "heatmap", "json")
                        .param("mode", "similarity")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("error"))
            .andExpect(jsonPath("$.message", containsString("Available")));
    }
}
