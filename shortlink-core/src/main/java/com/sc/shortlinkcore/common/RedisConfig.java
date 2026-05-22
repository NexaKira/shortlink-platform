package com.sc.shortlinkcore.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Configuration
public class RedisConfig {

    @Value("${shortlink.cache.ttl}")
    private long cacheTtl; // 从配置文件读取TTL

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory factory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(cacheTtl)) // 设置过期时间
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair
                                .fromSerializer(new StringRedisSerializer())
                ); // value 用 String 序列化，Redis 里可读
        return RedisCacheManager.builder(factory)
                .cacheDefaults(config)
                .build();
    }

}
