package com.soloproductions.wade.repository;

import com.soloproductions.wade.entity.UserDataset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface UserDatasetRepository extends JpaRepository<UserDataset, Long>
{
    List<UserDataset> findByUsername(String username);
    
    Optional<UserDataset> findByUsernameAndDatasetNameAndDatasetTypeAndDataType(String username, String datasetName, String datasetType, String dataType);

    @Query("SELECT DISTINCT u.datasetName FROM UserDataset u WHERE u.username = :username")
    List<String> findDistinctDatasetNamesByUsername(String username);

    boolean existsByUsernameAndDatasetName(String username, String datasetName);

    @Query("SELECT DISTINCT u.datasetType FROM UserDataset u WHERE u.username = :username AND u.datasetName = :datasetName")
    List<String> findDatasetTypesByUsernameAndDatasetName(String username, String datasetName);

    @Query("SELECT DISTINCT u.dataType FROM UserDataset u WHERE u.username = :username AND u.datasetName = :datasetName AND u.datasetType = :datasetType")
    List<String> findDataTypesByUsernameAndDatasetNameAndDatasetType(String username, String datasetName, String datasetType);
}
