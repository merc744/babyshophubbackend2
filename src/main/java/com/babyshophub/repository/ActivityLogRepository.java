package com.babyshophub.repository;

import com.babyshophub.entity.ActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {
    List<ActivityLog> findAllByOrderByCreatedAtDesc();

    List<ActivityLog> findAllByUserEmailOrderByCreatedAtDesc(String email);
}