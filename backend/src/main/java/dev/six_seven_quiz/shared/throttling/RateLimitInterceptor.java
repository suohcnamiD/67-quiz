package dev.six_seven_quiz.shared.throttling;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-IP token bucket for the hot public endpoints (login, register,
 * comment post, rating upsert). Anything trying to brute-force auth or
 * spam comments gets 429'd before it lands on the service layer.
 *
 * Cloudflare sits in front in prod so the true client IP arrives in
 * CF-Connecting-IP. We fall back to X-Forwarded-For (first hop) and
 * finally the socket peer for local dev. On the sockets path we don't
 * trust proxy headers because a malicious client can spoof them, but
 * behind CF the header is authoritative — CF strips inbound values
 * before setting its own.
 *
 * Buckets live in-process: fine for a single-instance deployment; move
 * to Redis if we ever scale horizontally.
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {
    private static final Logger log = LoggerFactory.getLogger(RateLimitInterceptor.class);

    /** Per-endpoint rules. Path prefixes; first match wins. */
    private static final Rule[] RULES = new Rule[]{
            // Auth endpoints — brute-force target. 10 requests / minute per IP.
            new Rule("/authentication/login", 10, 60_000),
            new Rule("/authentication/register", 5, 60_000),
            // Write endpoints that can be spammed.
            new Rule("/comments", 30, 60_000),
            new Rule("/quiz/", 60, 60_000), // covers /quiz/{id}/ratings, cover uploads, etc.
    };

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final boolean disabled;

    public RateLimitInterceptor(Environment environment) {
        // Buckets survive the whole process, so the e2e suite (which registers
        // many users per run) trips the register/login caps well before it
        // exercises the features under test. Disable the interceptor for the
        // local + test profiles; prod behaviour is unchanged.
        this.disabled = environment.matchesProfiles("local", "test");
        if (disabled) log.info("rate-limit interceptor disabled for local/test profile");
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (disabled) return true;
        Rule rule = matchRule(request);
        if (rule == null) return true;

        String ip = clientIp(request);
        String key = rule.pathPrefix() + "|" + ip;
        Bucket bucket = buckets.computeIfAbsent(key, k -> new Bucket(rule.capacity(), rule.windowMs()));
        long retryAfterMs = bucket.tryConsume();
        if (retryAfterMs < 0) return true;

        long retryAfterSeconds = Math.max(1, retryAfterMs / 1000);
        log.info("rate-limited ip={} path={} retryAfter={}s", ip, request.getRequestURI(), retryAfterSeconds);
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader("Retry-After", Long.toString(retryAfterSeconds));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"errors\":[{\"code\":\"RATE_LIMITED\",\"details\":{\"retryAfterSeconds\":" + retryAfterSeconds + "}}]}");
        return false;
    }

    private Rule matchRule(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path == null) return null;
        for (Rule rule : RULES) {
            if (path.startsWith(rule.pathPrefix())) return rule;
        }
        return null;
    }

    private String clientIp(HttpServletRequest request) {
        String cf = request.getHeader("CF-Connecting-IP");
        if (cf != null && !cf.isBlank()) return cf.trim();
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            int comma = xff.indexOf(',');
            return (comma < 0 ? xff : xff.substring(0, comma)).trim();
        }
        return request.getRemoteAddr();
    }

    private record Rule(String pathPrefix, int capacity, long windowMs) {}

    /**
     * Token bucket: {@code capacity} requests over a rolling
     * {@code windowMs} window. Tokens regenerate linearly. Returns -1
     * when the request is allowed, or the ms to wait until the next
     * token when the bucket is empty.
     */
    private static final class Bucket {
        private final int capacity;
        private final long windowMs;
        private double tokens;
        private long lastRefillMillis;

        Bucket(int capacity, long windowMs) {
            this.capacity = capacity;
            this.windowMs = windowMs;
            this.tokens = capacity;
            this.lastRefillMillis = System.currentTimeMillis();
        }

        synchronized long tryConsume() {
            refill();
            if (tokens >= 1) {
                tokens -= 1;
                return -1L;
            }
            double deficit = 1 - tokens;
            return (long) Math.ceil(deficit * (windowMs / (double) capacity));
        }

        private void refill() {
            long now = System.currentTimeMillis();
            long elapsed = now - lastRefillMillis;
            if (elapsed <= 0) return;
            double refillPerMs = capacity / (double) windowMs;
            tokens = Math.min(capacity, tokens + elapsed * refillPerMs);
            lastRefillMillis = now;
        }
    }
}
