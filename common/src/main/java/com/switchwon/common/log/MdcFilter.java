package com.switchwon.common.log;

import com.switchwon.common.response.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MdcFilter extends OncePerRequestFilter {

    public static final String TRACE_ID_HEADER =  "X-Trace-Id";
    public static final int TRACE_ID_LENGTH = 12;
    public static final Set<String> SKIP_LOG_PATTERNS = Set.of(
            "/h2-console", "/favicon.ico", "/actuator"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String traceId = resolveTraceId(request);
        MDC.put(ApiResponse.MDC_TRACE_ID, traceId);
        response.setHeader(TRACE_ID_HEADER, traceId);

        boolean skip = isSkipLogging(request.getRequestURI());
        long start = System.currentTimeMillis();
        try {
            if (!skip) {
                log.info("{} 요청 진입 method={} uri={} remote={}",
                        LoggingPatterns.BIZ_EVENT,
                        request.getMethod(),
                        request.getRequestURI(),
                        request.getRemoteAddr());
            }
            filterChain.doFilter(request, response);
        } finally {
            if (!skip) {
                long elapsed = System.currentTimeMillis() - start;
                log.info("{} 요청 종료 status={} elapsedMs={}",
                        LoggingPatterns.BIZ_EVENT,
                        response.getStatus(),
                        elapsed);
            }
            MDC.clear();
        }
    }

    private boolean isSkipLogging(String url) {
        return  SKIP_LOG_PATTERNS.stream().anyMatch(url::startsWith);
    }

    private String resolveTraceId(HttpServletRequest request) {
        String header = request.getHeader(TRACE_ID_HEADER);
        if (header != null && !header.isBlank()) {
            return header;
        }

        return UUID.randomUUID().toString().replace("-", "").substring(0, TRACE_ID_LENGTH);
    }
}
