package org.acme.vehicle.logging;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Provider
public class RequestLoggingFilter implements ContainerRequestFilter, ContainerResponseFilter {
    private static final Logger LOG = Logger.getLogger(RequestLoggingFilter.class);
    private static final String START_TIME_KEY = "requestStartTime";
    private static final String REQUEST_ID_KEY = "X-Request-ID";

    @Inject
    ObjectMapper objectMapper;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String requestId = requestContext.getHeaderString(REQUEST_ID_KEY);
        if (requestId == null || requestId.trim().isEmpty()) {
            requestId = UUID.randomUUID().toString();
        }
        requestContext.setProperty(REQUEST_ID_KEY, requestId);
        requestContext.setProperty(START_TIME_KEY, System.currentTimeMillis());
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        String requestId = (String) requestContext.getProperty(REQUEST_ID_KEY);
        Long startTime = (Long) requestContext.getProperty(START_TIME_KEY);
        long latencyMs = startTime != null ? System.currentTimeMillis() - startTime : 0;

        if (requestId != null) {
            responseContext.getHeaders().putSingle(REQUEST_ID_KEY, requestId);
        }

        try {
            Map<String, Object> logEntry = new HashMap<>();
            logEntry.put("request_id", requestId);
            logEntry.put("method", requestContext.getMethod());
            logEntry.put("path", requestContext.getUriInfo().getPath());
            logEntry.put("status", responseContext.getStatus());
            logEntry.put("latency_ms", latencyMs);

            LOG.info(objectMapper.writeValueAsString(logEntry));
        } catch (Exception e) {
            LOG.warn("Failed to serialize request log JSON", e);
        }
    }
}

