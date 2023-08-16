package com.qa.core.context;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@Lazy
public class CustomCacheManager {

    private final Cache<String, Object> cacheManager =
            CacheBuilder.newBuilder().maximumSize(1000).expireAfterWrite(15, TimeUnit.MINUTES).build();

    public CustomCacheManager() {}

    public void storeInCache(String key, Object value) {
        cacheManager.put(key, value);
    }

    public Object getFromCache(String key) {
        return cacheManager.asMap().get(key);
    }

    public boolean isCached(String key) {
        return cacheManager.asMap().containsKey(key);
    }
}
