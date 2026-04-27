package com.panto.wms.common.logging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * 请求日志过滤器测试。
 */
class RequestLoggingFilterTest {

    private final RequestLoggingFilter filter = new RequestLoggingFilter();

    @Test
    void shouldGenerateTraceIdWhenRequestDoesNotProvideOne() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/products");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (servletRequest, servletResponse) -> {
        };

        filter.doFilter(request, response, chain);

        String traceId = response.getHeader(RequestLoggingFilter.TRACE_ID_HEADER);
        assertNotNull(traceId);
        assertTrue(traceId.length() > 0);
        assertNull(MDC.get(RequestLoggingFilter.TRACE_ID_MDC_KEY));
    }

    @Test
    void shouldReuseIncomingTraceId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/products");
        request.addHeader(RequestLoggingFilter.TRACE_ID_HEADER, "trace-from-nginx");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (servletRequest, servletResponse) -> {
        };

        filter.doFilter(request, response, chain);

        assertEquals("trace-from-nginx", response.getHeader(RequestLoggingFilter.TRACE_ID_HEADER));
        assertNull(MDC.get(RequestLoggingFilter.TRACE_ID_MDC_KEY));
    }
}
