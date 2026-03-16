package com.soloproductions.wade.controller;

import com.soloproductions.wade.dto.AbstractDatasetRequest;
import com.soloproductions.wade.dto.ClusterDatasetRequest;
import com.soloproductions.wade.dto.SimilarityDatasetRequest;
import com.soloproductions.wade.dto.StandardResponse;
import com.soloproductions.wade.service.DatasetService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import com.soloproductions.wade.service.EmbeddedFusekiServer;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@SuppressWarnings("removal")
public class DatasetFilterTests
{
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DatasetService datasetService;

    @MockBean
    private EmbeddedFusekiServer embeddedFusekiServer;

    @Test
    public void shouldBindClusterGetParamsAndValidate() throws Exception
    {
        StandardResponse<Object> sr = new StandardResponse<>("success", "ok", null);
        Mockito.when(datasetService.handleRequest(Mockito.any())).thenReturn(sr);

        mockMvc.perform(get("/api/bsds300/clusters/json").param("mode", "cluster")
                        .param("range", "5")
                        .param("rangeStart", "1")
                        .param("sortDirection", "highest_probability")
                        .param("clusterName", "Cluster 1"))
                .andExpect(status().isOk());

        ArgumentCaptor<AbstractDatasetRequest> captor = ArgumentCaptor.forClass(AbstractDatasetRequest.class);
        Mockito.verify(datasetService).handleRequest(captor.capture());
        AbstractDatasetRequest req = captor.getValue();
        assertThat(req.getDatasetName()).isEqualTo("bsds300");
        assertThat(req.getDataType()).isEqualTo("json");
        assertThat(req.getDatasetType()).isEqualTo("clusters");
        assertThat(req.getRange()).isEqualTo(5);
        assertThat(req.getRangeStart()).isEqualTo(1);
        assertThat(req.getSortDirection()).isEqualTo("highest_probability");
        assertThat(req).isInstanceOf(ClusterDatasetRequest.class);
        ClusterDatasetRequest cdr = (ClusterDatasetRequest) req;
        assertThat(cdr.getClusterName()).isEqualTo("Cluster 1");
    }

    @Test
    public void shouldBindSimilarityGetParamsAndValidate() throws Exception
    {
        StandardResponse<Object> sr = new StandardResponse<>("success", "ok", null);
        Mockito.when(datasetService.handleRequest(Mockito.any())).thenReturn(sr);

        mockMvc.perform(get("/api/bsds300/heatmap/json").param("mode", "similarity")
                        .param("range", "10")
                        .param("rangeStart", "2")
                        .param("sortDirection", "highest_probability")
                        .param("similaritySortCriteria", "average_similarity"))
                .andExpect(status().isOk());

        ArgumentCaptor<AbstractDatasetRequest> captor = ArgumentCaptor.forClass(AbstractDatasetRequest.class);
        Mockito.verify(datasetService).handleRequest(captor.capture());
        AbstractDatasetRequest req = captor.getValue();
        assertThat(req.getDatasetName()).isEqualTo("bsds300");
        assertThat(req.getDataType()).isEqualTo("json");
        assertThat(req.getDatasetType()).isEqualTo("heatmap");
        assertThat(req.getRange()).isEqualTo(10);
        assertThat(req.getRangeStart()).isEqualTo(2);
        assertThat(req.getSortDirection()).isEqualTo("highest_probability");
        assertThat(req).isInstanceOf(SimilarityDatasetRequest.class);
        SimilarityDatasetRequest sdr = (SimilarityDatasetRequest) req;
        assertThat(sdr.getSimilaritySortCriteria()).isEqualTo("average_similarity");
    }

    @Test
    public void shouldBindSqlSimilarityGetParamsAndValidate() throws Exception
    {
        StandardResponse<Object> sr = new StandardResponse<>("success", "ok", null);
        Mockito.when(datasetService.handleRequest(Mockito.any())).thenReturn(sr);

        mockMvc.perform(get("/api/bsds300/heatmap/sql").param("mode", "similarity")
                        .param("range", "7")
                        .param("rangeStart", "3")
                        .param("sortDirection", "highest_probability")
                        .param("similaritySortCriteria", "average_similarity"))
                .andExpect(status().isOk());

        ArgumentCaptor<AbstractDatasetRequest> captor = ArgumentCaptor.forClass(AbstractDatasetRequest.class);
        Mockito.verify(datasetService).handleRequest(captor.capture());
        AbstractDatasetRequest req = captor.getValue();
        assertThat(req.getDatasetName()).isEqualTo("bsds300");
        assertThat(req.getDataType()).isEqualTo("sql");
        assertThat(req.getDatasetType()).isEqualTo("heatmap");
        assertThat(req.getRange()).isEqualTo(7);
        assertThat(req.getRangeStart()).isEqualTo(3);
        assertThat(req.getSortDirection()).isEqualTo("highest_probability");
        assertThat(req).isInstanceOf(SimilarityDatasetRequest.class);
        SimilarityDatasetRequest sdr = (SimilarityDatasetRequest) req;
        assertThat(sdr.getSimilaritySortCriteria()).isEqualTo("average_similarity");
    }

    @Test
    public void shouldBindSqlClusterGetParamsAndValidate() throws Exception
    {
        StandardResponse<Object> sr = new StandardResponse<>("success", "ok", null);
        Mockito.when(datasetService.handleRequest(Mockito.any())).thenReturn(sr);

        mockMvc.perform(get("/api/bsds300/clusters/sql").param("mode", "cluster")
                        .param("range", "4")
                        .param("rangeStart", "1")
                        .param("sortDirection", "highest_probability")
                        .param("clusterName", "Cluster A"))
                .andExpect(status().isOk());

        ArgumentCaptor<AbstractDatasetRequest> captor = ArgumentCaptor.forClass(AbstractDatasetRequest.class);
        Mockito.verify(datasetService).handleRequest(captor.capture());
        AbstractDatasetRequest req = captor.getValue();
        assertThat(req.getDatasetName()).isEqualTo("bsds300");
        assertThat(req.getDataType()).isEqualTo("sql");
        assertThat(req.getDatasetType()).isEqualTo("clusters");
        assertThat(req.getRange()).isEqualTo(4);
        assertThat(req.getRangeStart()).isEqualTo(1);
        assertThat(req.getSortDirection()).isEqualTo("highest_probability");
        assertThat(req).isInstanceOf(ClusterDatasetRequest.class);
        ClusterDatasetRequest cdr = (ClusterDatasetRequest) req;
        assertThat(cdr.getClusterName()).isEqualTo("Cluster A");
    }
}
