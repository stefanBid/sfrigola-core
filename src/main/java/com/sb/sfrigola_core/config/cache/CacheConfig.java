package com.sb.sfrigola_core.config.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager caffeineCacheManager() {

        CaffeineCache languageCacheManager = new CaffeineCache("languages", Caffeine.newBuilder()
                .maximumSize(100)
                .expireAfterWrite(1, TimeUnit.DAYS)
                .build());

        CaffeineCache roleCacheManager = new CaffeineCache("roles", Caffeine.newBuilder()
                .maximumSize(10)
                .expireAfterWrite(1, TimeUnit.DAYS)
                .build());
        CaffeineCache categoryCacheManager = new CaffeineCache("categories", Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(1, TimeUnit.DAYS)
                .build());

        SimpleCacheManager manager = new SimpleCacheManager();
        manager.setCaches(Arrays.asList(languageCacheManager, roleCacheManager, categoryCacheManager));
        return manager;
    }
}
