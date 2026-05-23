package com.sc.shortlinkcore.service;

import com.sc.shortlinkcore.entity.ClickLog;
import com.sc.shortlinkcore.repository.ClickLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class ClickLogService {

    @Autowired
    private ClickLogRepository clickLogRepository;

    // 异步保存日志
    @Async
    public void logClick(String shortCode, String visitorIp, String userAgent, String referer) {
        ClickLog log = new ClickLog(shortCode, visitorIp, userAgent, referer);
        clickLogRepository.save(log);
    }

    public long getClickCount(String shortCode) {
        return clickLogRepository.countByShortCode(shortCode);
    }

}
