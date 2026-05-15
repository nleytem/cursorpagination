package com.nmleytem.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Filter that generates a unique Request ID for each incoming HTTP request
 * and adds it to the SLF4J Mapped Diagnostic Context (MDC).
 * This allows all logs associated with a single request to be traced using the same ID.
 */
@Component
public class LoggingFilter extends OncePerRequestFilter {

    private static final String REQUEST_ID_KEY = "requestId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        long startTime = System.currentTimeMillis();
        String requestId = UUID.randomUUID().toString();
        
        // Use X-Request-ID from client if provided, otherwise use generated UUID
        String clientRequestId = request.getHeader("X-Request-ID");
        if (clientRequestId != null && !clientRequestId.isBlank()) {
            requestId = clientRequestId;
        }

        MDC.put(REQUEST_ID_KEY, requestId);
        
        try {
            response.addHeader("X-Request-ID", requestId);
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            logger.info(String.format("Request completed: method=%s uri=%s status=%d duration=%dms",
                    request.getMethod(), request.getRequestURI(), response.getStatus(), duration));
            MDC.remove(REQUEST_ID_KEY);
        }
    }
}
