package com.sc.shortlinkcore.repository;

import com.sc.shortlinkcore.entity.ShortLink;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ShortLinkRepository extends JpaRepository<ShortLink, Long> {
    // 根据短码查询，Optional 表示可能找不到
    Optional<ShortLink> findByShortCode(String shortCode);

    // 判断某个短码是否已经存在
    boolean existsByShortCode(String shortCode);
}