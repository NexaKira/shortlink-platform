package com.sc.shortlinkcore.repository;

import com.sc.shortlinkcore.entity.ClickLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClickLogRepository extends JpaRepository<ClickLog, Long> {
    // 根据短码查询
    long countByShortCode(String shortCode);

}
