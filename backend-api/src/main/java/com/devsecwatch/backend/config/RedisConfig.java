package com.devsecwatch.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        return template;
    }

    @Bean
    public io.github.bucket4j.distributed.proxy.ProxyManager<byte[]> proxyManager(RedisConnectionFactory connectionFactory) {
        if (connectionFactory instanceof org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory) {
            org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory lettuceFactory = (org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory) connectionFactory;
            io.lettuce.core.RedisClient client = (io.lettuce.core.RedisClient) lettuceFactory.getNativeClient();
            io.lettuce.core.api.StatefulRedisConnection<byte[], byte[]> connection = client
                    .connect(io.lettuce.core.codec.ByteArrayCodec.INSTANCE);
            return io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager.builderFor(connection)
                    .build();
        }
        throw new IllegalStateException("Unsupported Redis connection factory type: " + connectionFactory.getClass().getName());
    }
}
