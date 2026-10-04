package com.carepath.security;
import java.util.List;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
@Component
public class RedisRateLimitStore implements RateLimitStore {
    private final StringRedisTemplate redis;
    private static final DefaultRedisScript<Long> SCRIPT=new DefaultRedisScript<>(
        "local n=redis.call('INCR',KEYS[1]); if n==1 then redis.call('EXPIRE',KEYS[1],ARGV[1]); end; return n",Long.class);
    public RedisRateLimitStore(StringRedisTemplate redis) { this.redis=redis; }
    @Override public boolean allow(String key, int limit, int windowSeconds) {
        Long n=redis.execute(SCRIPT,List.of(key),String.valueOf(windowSeconds));
        if (n==null) throw new IllegalStateException("Rate limiter unavailable");
        return n<=limit;
    }
}
