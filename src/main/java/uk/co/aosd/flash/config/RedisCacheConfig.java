package uk.co.aosd.flash.config;

import java.time.Duration;

import org.springframework.boot.cache.autoconfigure.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;
import org.springframework.data.redis.serializer.RedisSerializer;

/**
 * Configure the Redis cache.
 */
@Configuration
@EnableCaching
public class RedisCacheConfig {

    /**
     * Configure the cache.
     */
    @Bean
    public RedisCacheConfiguration cacheConfiguration() {
        return RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(60)) // Set default expiration to 60 mins
            .disableCachingNullValues() // Don't cache null results
            .serializeValuesWith(SerializationPair.fromSerializer(RedisSerializer.json()));
    }

    /**
     * Customize the cache.
     */
    @Bean
    public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer() {
        return (builder) -> builder
            .withCacheConfiguration("products",
                RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofMinutes(1)))
            .withCacheConfiguration("activeSales",
                RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofMinutes(1)))
            .withCacheConfiguration("draftSales",
                RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofMinutes(1)))
            .withCacheConfiguration("flashSales",
                RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofMinutes(1)))
            .withCacheConfiguration("orders",
                RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofMinutes(1)))
            .withCacheConfiguration("orders:user",
                RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofMinutes(1)))
            .withCacheConfiguration("orders:all",
                RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofMinutes(1)))
            .withCacheConfiguration("users",
                RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofMinutes(5)))
            .withCacheConfiguration("analytics:sales",
                RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofMinutes(5)))
            .withCacheConfiguration("analytics:revenue",
                RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofMinutes(5)))
            .withCacheConfiguration("analytics:products",
                RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofMinutes(5)))
            .withCacheConfiguration("analytics:orders",
                RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofMinutes(5)));
    }
}
