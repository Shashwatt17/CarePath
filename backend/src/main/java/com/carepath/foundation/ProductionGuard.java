package com.carepath.foundation;

import java.net.URI;
import java.util.Arrays;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/** Explicit production startup gate; never prints configuration values or credentials. */
@Component
@Profile("production")
public class ProductionGuard {
    public ProductionGuard(Environment env) {
        if (Arrays.asList(env.getActiveProfiles()).contains("local") || Arrays.asList(env.getActiveProfiles()).contains("test"))
            throw new IllegalStateException("Production cannot run with local or test profiles");
        if (!env.getProperty("carepath.auth.cookie-secure", Boolean.class, true))
            throw new IllegalStateException("Production requires Secure cookies");
        String origin = env.getProperty("carepath.frontend-origin", "");
        if (!origin.equals(env.getProperty("carepath.auth.frontend-origin", "")))
            throw new IllegalStateException("Authentication and CORS origins must match");
        try {
            URI uri = URI.create(origin);
            String host = uri.getHost();
            if (!"https".equals(uri.getScheme()) || host == null || host.equals("localhost") || host.endsWith(".localhost")
                    || host.equals("127.0.0.1") || host.equals("[::1]") || uri.getUserInfo() != null
                    || uri.getQuery() != null || uri.getFragment() != null || !uri.getPath().isEmpty())
                throw new IllegalArgumentException();
        } catch (IllegalArgumentException e) { throw new IllegalStateException("Production requires one external HTTPS origin"); }
        if (!env.getProperty("spring.datasource.url", "").startsWith("jdbc:postgresql://"))
            throw new IllegalStateException("Production requires PostgreSQL");
        for (String name : new String[]{"spring.datasource.password", "spring.data.redis.password"}) {
            if (env.getProperty(name, "").isBlank()) throw new IllegalStateException("Production service credentials are required");
        }
        if (env.getProperty("springdoc.api-docs.enabled", Boolean.class, false)
                || env.getProperty("springdoc.swagger-ui.enabled", Boolean.class, false))
            throw new IllegalStateException("Public API documentation must be disabled in production");
    }
}
