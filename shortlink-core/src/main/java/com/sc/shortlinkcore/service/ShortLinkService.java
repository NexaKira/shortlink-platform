package com.sc.shortlinkcore.service;

import com.sc.shortlinkcore.entity.ShortLink;
import com.sc.shortlinkcore.repository.ShortLinkRepository;
import com.sc.shortlinkcore.util.ShortCodeGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service   // 标记这是一个 Service 组件，Spring 会自动管理它
public class ShortLinkService {

    @Autowired   // 让 Spring 自动把 repository 的实例注入进来
    private ShortLinkRepository repository;

    // 生成短链的方法
    public String createShortLink(String longUrl) {
        String shortCode;
        int maxRetry = 3;   // 最多重试3次
        do {
            shortCode = ShortCodeGenerator.generate();  // 生成随机短码
        } while (repository.existsByShortCode(shortCode) && --maxRetry > 0);
        // 如果短码已经存在，就重新生成，直到重试次数用完

        // 如果重试用完了还是重复，报错
        if (repository.existsByShortCode(shortCode)) {
            throw new RuntimeException("短码生成失败，请重试");
        }

        // 创建实体对象，并保存到数据库
        ShortLink link = new ShortLink(shortCode, longUrl);
        repository.save(link);

        // 返回完整的短链接地址（本地测试用）
        return "http://localhost:8080/" + shortCode;
    }

    // 根据短码获取原始长链接（用于跳转）
    public String getLongUrl(String shortCode) {
        // Optional 像一个盒子，里面可能有值，也可能为空
        java.util.Optional<ShortLink> optional = repository.findByShortCode(shortCode);
        if (optional.isPresent()) {
            return optional.get().getLongUrl();  // 有值，取出长链接
        } else {
            throw new RuntimeException("短链不存在: " + shortCode);
        }
    }
}