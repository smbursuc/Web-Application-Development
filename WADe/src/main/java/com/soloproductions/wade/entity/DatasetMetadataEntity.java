package com.soloproductions.wade.entity;

import jakarta.persistence.*;

/**
 * Persists user-editable metadata for dynamically-created datasets.
 * One row per (username, datasetName).
 */
@Entity
@Table(name = "dataset_metadata_entry",
       uniqueConstraints = { @UniqueConstraint(columnNames = { "username", "dataset_name" }) })
public class DatasetMetadataEntity
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String username;

    @Column(name = "dataset_name", nullable = false)
    private String datasetName;

    /** Short summary / description shown in the info modal. */
    @Column(name = "summary", length = 2048)
    private String summary;

    /** URL pointing to the original data source. */
    @Column(name = "source")
    private String source;

    public DatasetMetadataEntity() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getDatasetName() { return datasetName; }
    public void setDatasetName(String datasetName) { this.datasetName = datasetName; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
}
