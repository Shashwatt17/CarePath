package com.carepath.security;
public interface RateLimitStore { boolean allow(String key, int limit, int windowSeconds); }
