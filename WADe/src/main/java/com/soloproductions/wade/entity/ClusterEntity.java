package com.soloproductions.wade.entity;

import com.soloproductions.wade.dataset.DataEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "cluster")
public class ClusterEntity implements DataEntity
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String username;

    @Column(name = "dataset_name", nullable = false)
    private String datasetName;

    @Column(name = "cluster_name", nullable = true)
    private String clusterName;

    @Column(name = "node_name", nullable = false)
    private String nodeName;

    @Column(name = "parent_name", nullable = true)
    private String parentName;

    @Column(name = "probability", nullable = true)
    private Double probability;

    @Column(name = "uri", nullable = true)
    private String uri;

    public ClusterEntity() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getDatasetName() { return datasetName; }
    public void setDatasetName(String datasetName) { this.datasetName = datasetName; }
    public String getClusterName() { return clusterName; }
    public void setClusterName(String clusterName) { this.clusterName = clusterName; }
    public String getNodeName() { return nodeName; }
    public void setNodeName(String nodeName) { this.nodeName = nodeName; }
    public String getParentName() { return parentName; }
    public void setParentName(String parentName) { this.parentName = parentName; }
    public Double getProbability() { return probability; }
    public void setProbability(Double probability) { this.probability = probability; }
    public String getUri() { return uri; }
    public void setUri(String uri) { this.uri = uri; }
}
