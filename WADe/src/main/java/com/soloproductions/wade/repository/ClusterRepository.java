package com.soloproductions.wade.repository;

import com.soloproductions.wade.entity.ClusterEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ClusterRepository extends JpaRepository<ClusterEntity, Long>
{
    List<ClusterEntity> findByUsernameAndDatasetName(String username, String datasetName);
    List<ClusterEntity> findByUsernameAndDatasetName(String username, String datasetName, Pageable pageable);
    List<ClusterEntity> findByUsernameAndDatasetNameAndNodeNameAndParentName(String username, String datasetName, String nodeName, String parentName);
    List<ClusterEntity> findByUsernameAndDatasetNameAndNodeName(String username, String datasetName, String nodeName);
    List<ClusterEntity> findByUsernameAndDatasetNameAndParentName(String username, String datasetName, String parentName);
    List<ClusterEntity> findByDatasetName(String datasetName);
    List<ClusterEntity> findByDatasetName(String datasetName, Pageable pageable);
    long countByUsernameAndDatasetName(String username, String datasetName);
}
