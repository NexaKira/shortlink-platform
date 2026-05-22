package com.sc.shortlinkcore.common;

import com.google.common.hash.Funnels;
import com.sc.shortlinkcore.entity.ShortLink;
import com.sc.shortlinkcore.repository.ShortLinkRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.google.common.hash.BloomFilter;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class BloomFilterService {

    private final BloomFilter<String> bloomFilter = BloomFilter.create(Funnels.stringFunnel(StandardCharsets.UTF_8),100000, 0.01);

    @Autowired
    private ShortLinkRepository shortLinkRepository;

    @PostConstruct
    public void init() {
        // 启动时扫描全表，把已有短码加载进布隆过滤器
        List<ShortLink> shortLinks = shortLinkRepository.findAll();
        for(ShortLink shortLink: shortLinks) {
            bloomFilter.put(shortLink.getShortCode());
        }
    }

    public void put(String shortCode) {
        // 将新创建的短码加载进布隆过滤器
        bloomFilter.put(shortCode);
    }

    public boolean mightContain(String shortCode) {
        return bloomFilter.mightContain(shortCode);
    }

}
