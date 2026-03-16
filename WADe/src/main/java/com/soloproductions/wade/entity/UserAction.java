package com.soloproductions.wade.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_action")
public class UserAction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String actionType; // e.g., "VIEW", "CREATE", "UPDATE", "DELETE"

    @Column(nullable = false)
    private String datasetId;

    @Column(nullable = false)
    private String datasetType; // "heatmap" or "cluster"

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(length = 1000)
    private String description;

    public UserAction() {}

    public UserAction(User user, String actionType, String datasetId, String datasetType, String description) {
        this.user = user;
        this.actionType = actionType;
        this.datasetId = datasetId;
        this.datasetType = datasetType;
        this.timestamp = LocalDateTime.now();
        this.description = description;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }
    public String getDatasetId() { return datasetId; }
    public void setDatasetId(String datasetId) { this.datasetId = datasetId; }
    public String getDatasetType() { return datasetType; }
    public void setDatasetType(String datasetType) { this.datasetType = datasetType; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
