package com.carepath.security;

import com.carepath.foundation.*;
import com.carepath.identity.AuthProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import org.springframework.web.filter.OncePerRequestFilter;

// Installed only inside the Spring Security chain (not a servlet component).
public class AuthBoundaryFilter extends OncePerRequestFilter {
    private final AuthProperties properties; private final AuthRateLimiter limits; private final ObjectMapper mapper;
    public AuthBoundaryFilter(AuthProperties properties, AuthRateLimiter limits, ObjectMapper mapper) { this.properties=properties;this.limits=limits;this.mapper=mapper; }
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain) throws ServletException,IOException {
        if (request.getRequestURI().startsWith("/api/v1/public/share") || request.getRequestURI().startsWith("/api/v1/shares")) {
            response.setHeader("Cache-Control","no-store"); response.setHeader("Pragma","no-cache");
            response.setHeader("X-Robots-Tag","noindex, nofollow, noarchive");
            if ("POST".equals(request.getMethod())) {
                var bounded=new BoundedAuthRequest(request);
                if(bounded.tooLarge()){ApiErrors.write(response,mapper,413,"REQUEST_TOO_LARGE","The request is too large.");return;}
                request=bounded;
            }
            if(request.getRequestURI().startsWith("/api/v1/public/share")) {
                try {limits.share(request.getRemoteAddr());}
                catch(ApiFailure e){if(e.status()==429)response.setHeader("Retry-After",String.valueOf(properties.rateWindowSeconds()));ApiErrors.write(response,mapper,e.status(),"SHARE_UNAVAILABLE","This shared Visit Pack is unavailable. Please try later.");return;}
            }
        }
        if (request.getRequestURI().startsWith("/api/v1/auth/")) {
            response.setHeader("Cache-Control","no-store"); response.setHeader("Pragma","no-cache");
            if ("POST".equals(request.getMethod())) {
                // Strict origin + non-simple custom header protect refresh/logout AND login CSRF.
                if (!properties.frontendOrigin().equals(request.getHeader("Origin")) || !"web".equals(request.getHeader("X-CarePath-Client"))) {
                    ApiErrors.write(response,mapper,403,"ORIGIN_REJECTED","The request origin could not be verified."); return;
                }
                var bounded=new BoundedAuthRequest(request);
                if (bounded.tooLarge()) { ApiErrors.write(response,mapper,413,"REQUEST_TOO_LARGE","The request is too large."); return; }
                request=bounded;
                try { limits.ip(request.getRemoteAddr()); }
                catch(ApiFailure e) {
                    if(e.status()==429) response.setHeader("Retry-After",String.valueOf(properties.rateWindowSeconds()));
                    ApiErrors.write(response,mapper,e.status(),e.code(),e.getMessage()); return;
                }
            }
        }
        if((request.getRequestURI().equals("/api/v1/search") || request.getRequestURI().startsWith("/api/v1/nearby-care/") || request.getRequestURI().startsWith("/api/v1/visit-packs") || request.getRequestURI().startsWith("/api/v1/care/") || request.getRequestURI().startsWith("/api/v1/review/") || request.getRequestURI().startsWith("/api/v1/assistant/")) && ("POST".equals(request.getMethod()) || "PUT".equals(request.getMethod()))) {
            var bounded=new BoundedAuthRequest(request);
            if(bounded.tooLarge()) {ApiErrors.write(response,mapper,413,"REQUEST_TOO_LARGE","The request is too large.");return;}
            request=bounded;
        }
        chain.doFilter(request,response);
    }
}
