package uteq.edu.ec.artisync.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Configuration
@EnableCaching
public class RedisConfig {

    @Value("${app.cache.catalogo.ttl-seconds:60}")
    private long catalogoTtlSeconds;

    /**
     * Plantilla Redis de claves y valores {@code String}, usada por la lista
     * negra de JTI ({@code JwtAuthenticationFilter}) y el rate limiter
     * ({@code AuthRateLimitFilter}). Serialización explícita con
     * {@link StringRedisSerializer} en vez del default JDK, para que las
     * claves sean legibles directamente en {@code redis-cli}.
     *
     * @param connectionFactory conexión Redis configurada por Spring Boot
     * @return un {@link RedisTemplate} de {@code String}/{@code String}
     */
    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }

    /**
     * ADR-004: caché del catálogo con TTL corto configurable externamente (sin
     * recompilar el backend), habilita el escenario "frío vs caliente" de las
     * mediciones de rendimiento (Bloque C.1).
     */
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration catalogoConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(catalogoTtlSeconds))
                .disableCachingNullValues();

        return RedisCacheManager.builder(connectionFactory)
                .withCacheConfiguration("catalogo", catalogoConfig)
                .build();
    }
}
