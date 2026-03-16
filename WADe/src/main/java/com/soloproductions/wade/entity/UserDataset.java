package com.soloproductions.wade.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "user_dataset", uniqueConstraints = {@UniqueConstraint(columnNames = {"username", "dataset_name", "dataset_type", "data_type"})})
public class UserDataset
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String username;

    @Column(name = "dataset_name", nullable = false)
    private String datasetName;

    @Column(name = "dataset_type", nullable = false)
    private String datasetType; // e.g., "HEATMAP" or "CLUSTERS"

    @Column(name = "data_type", nullable = false)
    private String dataType; // e.g., "SQL", "JSON", "RDF"

    public UserDataset() {}

    public UserDataset(String username, String datasetName, String datasetType, String dataType) {
        this.username = username;
        this.datasetName = datasetName;
        this.datasetType = datasetType;
        this.dataType = dataType;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getDatasetName() { return datasetName; }
    public void setDatasetName(String datasetName) { this.datasetName = datasetName; }
    public String getDatasetType() { return datasetType; }
    public void setDatasetType(String datasetType) { this.datasetType = datasetType; }
    public String getDataType() { return dataType; }
    public void setDataType(String dataType) { this.dataType = dataType; }
}
