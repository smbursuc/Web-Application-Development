package com.soloproductions.wade.repository;

import com.soloproductions.wade.entity.UserAction;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface UserActionRepository extends JpaRepository<UserAction, Long> {
    List<UserAction> findByUserIdOrderByTimestampDesc(Long userId, Pageable pageable);
    List<UserAction> findByUserIdOrderByTimestampDesc(Long userId);
    long countByUserId(Long userId);
    long countByUserIdAndActionType(Long userId, String actionType);

    // Delete operations change database state and require an active transaction.
    // Ensuring this method is transactional also allows proper rollback behavior
    // if something fails during deletion.
    @Transactional
    void deleteByUserId(Long userId);
}
