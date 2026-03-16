package com.soloproductions.wade.entity;

import com.soloproductions.wade.dataset.DataEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "heatmap", uniqueConstraints = {@UniqueConstraint(columnNames = {"username", "dataset_name", "object_1", "object_2"})})
public class HeatmapEntity implements DataEntity
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String username;

    @Column(name = "dataset_name", nullable = false)
    private String datasetName;

    @Column(name = "object_1", nullable = false)
    private String object1;

    @Column(name = "object_2", nullable = false)
    private String object2;

    @Column(name = "similarity", nullable = false)
    private Double similarity;

    public HeatmapEntity() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getDatasetName() { return datasetName; }
    public void setDatasetName(String datasetName) { this.datasetName = datasetName; }

    public String getObject1() { return object1; }
    public void setObject1(String object1) { this.object1 = object1; }

    public String getObject2() { return object2; }
    public void setObject2(String object2) { this.object2 = object2; }

    public Double getSimilarity() { return similarity; }
    public void setSimilarity(Double similarity) { this.similarity = similarity; }
}
