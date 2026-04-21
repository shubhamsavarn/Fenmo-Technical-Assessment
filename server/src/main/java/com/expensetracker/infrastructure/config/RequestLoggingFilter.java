package com.expensetracker.infrastructure.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

/**
 * Servlet filter that assigns a unique correlation ID (traceId) to every request
 * and logs request/response lifecycle events.
 *
 * <p>The traceId is stored in SLF4J's MDC (Mapped Diagnostic Context), which means
 * every log line emitted during this request will automatically include the traceId.
 * This enables tracing a single request through all layers and services.</p>
 */
@Component
@Order(1)
public class RequestLoggingFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);
    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String MDC_TRACE_ID = "traceId";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // Generate or reuse trace ID
        String traceId = httpRequest.getHeader(TRACE_ID_HEADER);
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString().substring(0, 8); // Short ID for readability
        }

        MDC.put(MDC_TRACE_ID, traceId);
        httpResponse.setHeader(TRACE_ID_HEADER, traceId);

        long startTime = System.currentTimeMillis();
        String method = httpRequest.getMethod();
        String uri = httpRequest.getRequestURI();
        String query = httpRequest.getQueryString();

        log.info("→ {} {} {}", method, uri, query != null ? "?" + query : "");

        try {
            chain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            log.info("← {} {} {} {}ms", method, uri, httpResponse.getStatus(), duration);
            MDC.remove(MDC_TRACE_ID);
        }
    }
}
