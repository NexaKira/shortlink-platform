package com.sc.shortlinkcore.config;

import com.sc.shortlinkcore.util.WorkerIdManager;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ShutdownConfig {

    @Autowired
    private WorkerIdManager workerIdManager;

    @PreDestroy
    public void onShutdown() {
        workerIdManager.release();
    }

}
