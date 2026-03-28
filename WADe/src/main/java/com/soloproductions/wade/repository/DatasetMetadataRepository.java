package com.soloproductions.wade.repository;

import com.soloproductions.wade.entity.DatasetMetadataEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DatasetMetadataRepository extends JpaRepository<DatasetMetadataEntity, Long>
{
    Optional<DatasetMetadataEntity> findByUsernameAndDatasetName(String username, String datasetName);
}
