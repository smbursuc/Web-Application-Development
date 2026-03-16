package com.soloproductions.wade.repository;

import com.soloproductions.wade.entity.UserAction;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserActionRepository extends JpaRepository<UserAction, Long> {
    List<UserAction> findByUserIdOrderByTimestampDesc(Long userId, Pageable pageable);
    List<UserAction> findByUserIdOrderByTimestampDesc(Long userId);
    long countByUserId(Long userId);
    long countByUserIdAndActionType(Long userId, String actionType);
}
