package com.yinyue.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class ApiSecurityFilter extends OncePerRequestFilter {

    private static final String TOKEN_HEADER = "X-API-Token";

    private final AppSecurityProperties securityProperties;
    private final RequestRateLimiter requestRateLimiter;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    private final List<String> protectedPaths = List.of(
            "/api/upload/**",
            "/api/music/**",
            "/api/ai/analyze",
            "/api/ai/generate-image",
            "/api/ai/netease",
            "/api/ai/netease/parse"
    );

    public ApiSecurityFilter(AppSecurityProperties securityProperties,
                             RequestRateLimiter requestRateLimiter) {
        this.securityProperties = securityProperties;
        this.requestRateLimiter = requestRateLimiter;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String path = request.getRequestURI();
        for (String protectedPath : protectedPaths) {
            if (pathMatcher.match(protectedPath, path)) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (securityProperties.isEnabled()) {
            String expectedToken = securityProperties.getApiToken();
            String actualToken = request.getHeader(TOKEN_HEADER);

            if (expectedToken == null || expectedToken.isBlank() || !expectedToken.equals(actualToken)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":401,\"message\":\"缺少或无效的 API Token\"}");
                return;
            }
        }

        if (!requestRateLimiter.allow(request.getRemoteAddr())) {
            response.setStatus(429);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":429,\"message\":\"请求过于频繁，请稍后再试\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
