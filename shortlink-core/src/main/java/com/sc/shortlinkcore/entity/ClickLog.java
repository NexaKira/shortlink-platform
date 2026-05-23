package com.sc.shortlinkcore.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Entity
@Data
@NoArgsConstructor
@Table(name = "click_log")
public class ClickLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "short_code", length = 16, nullable = false)
    private String shortCode;

    @Column(name = "visitor_ip", length = 45)
    private String visitorIp;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(name = "referer", length = 2048)
    private String referer;

    @Column(name = "click_time", nullable = false)
    private LocalDateTime clickTime;

    public ClickLog(String shortCode, String visitorIp, String userAgent, String referer) {
        this.shortCode = shortCode;
        this.visitorIp = visitorIp;
        this.userAgent = userAgent;
        this.referer = referer;
        this.clickTime = LocalDateTime.now();
    }
}
