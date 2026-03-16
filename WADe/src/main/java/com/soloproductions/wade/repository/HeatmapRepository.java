package com.soloproductions.wade.repository;

import com.soloproductions.wade.entity.HeatmapEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface HeatmapRepository extends JpaRepository<HeatmapEntity, Long>
{
    List<HeatmapEntity> findByUsernameAndDatasetName(String username, String datasetName);
    List<HeatmapEntity> findByUsernameAndDatasetName(String username, String datasetName, Pageable pageable);
    List<HeatmapEntity> findByUsernameAndDatasetNameAndObject1AndObject2(String username, String datasetName, String object1, String object2);
    List<HeatmapEntity> findByUsernameAndDatasetNameAndObject1AndObject2AndSimilarity(String username, String datasetName, String object1, String object2, Double similarity);
    List<HeatmapEntity> findByDatasetName(String datasetName);
    List<HeatmapEntity> findByDatasetName(String datasetName, Pageable pageable);
    List<HeatmapEntity> findByDatasetNameAndObject1AndObject2(String datasetName, String object1, String object2);
    long countByUsernameAndDatasetName(String username, String datasetName);
}
