package com.sc.shortlinkcore.service;

import com.sc.shortlinkcore.common.BloomFilterService;
import com.sc.shortlinkcommon.BusinessException;
import com.sc.shortlinkcore.entity.ShortLink;
import com.sc.shortlinkcore.repository.ShortLinkRepository;
import com.sc.shortlinkcommon.util.Base62Encoder;
import com.sc.shortlinkcore.util.SnowflakeIdGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;


@Service   // 标记这是一个 Service 组件，Spring 会自动管理它
public class ShortLinkService {

    @Value("${shortlink.base-url}")
    private String baseUrl;

    @Autowired   // 让 Spring 自动把 repository 的实例注入进来
    private ShortLinkRepository repository;

    @Autowired  // 让 Spring 自动把 bloomFilter 的实例注入进来
    private BloomFilterService bloomFilter;

    @Autowired
    private SnowflakeIdGenerator snowflakeIdGenerator;

    @Autowired
    private ClickLogService clickLogService;

    // 生成短链的方法
    public String createShortLink(String longUrl) {
        long snowflakeId = snowflakeIdGenerator.nextId();
        String shortCode = Base62Encoder.encode(snowflakeId);  // 生成随机短码

        // 创建实体对象，并保存到数据库
        ShortLink link = new ShortLink(shortCode, longUrl);
        repository.save(link);
        // 将新创建的短码加载进布隆过滤器
        bloomFilter.put(shortCode);

        // 返回完整的短链接地址（本地测试用）
        return baseUrl + shortCode;
    }

    // 根据短码获取原始长链接（用于跳转）
    @Cacheable(value = "shortlink", key = "#shortCode")
    public String getLongUrl(String shortCode) {
        // 布隆过滤器过滤一遍
        if (!bloomFilter.mightContain(shortCode)) {
            throw new BusinessException(404, "短链不存在: " + shortCode);
        }
        // Optional 像一个盒子，里面可能有值，也可能为空
        java.util.Optional<ShortLink> optional = repository.findByShortCode(shortCode);
        if (optional.isPresent()) {
            return optional.get().getLongUrl();  // 有值，取出长链接
        } else {
            throw new BusinessException(404, "短链不存在: " + shortCode);
        }
    }

    public void recordClick(String shortCode, String visitorIp, String userAgent, String referer) {
        clickLogService.logClick(shortCode, visitorIp, userAgent, referer);
    }

}