package com.switchwon.common.log;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class MdcFilterTest {

    private final MdcFilter filter = new MdcFilter();

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    @DisplayName("traceId가_없으면_12자리_UUID를_자동_발급한다")
    void traceId_없으면_12자리_UUID를_자동_발급한다() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> {
            String traceId = MDC.get("traceId");
            assertThat(traceId).isNotNull().hasSize(12);
        };

        filter.doFilterInternal(request, response, chain);
    }

    @Test
    @DisplayName("X-Trace-Id_헤더가_있으면_그_값을_traceId로_사용한다")
    void X_Trace_Id_헤더가_있으면_그_값을_traceId로_사용한다() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(MdcFilter.TRACE_ID_HEADER, "custom-trace-01");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) ->
                assertThat(MDC.get("traceId")).isEqualTo("custom-trace-01");

        filter.doFilterInternal(request, response, chain);
    }

    @Test
    @DisplayName("응답_헤더에_traceId가_포함된다")
    void 응답_헤더에_traceId가_포함된다() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, mock(FilterChain.class));

        assertThat(response.getHeader(MdcFilter.TRACE_ID_HEADER)).isNotNull();
    }

    @Test
    @DisplayName("요청_처리_후_MDC가_정리된다")
    void 요청_처리_후_MDC가_정리된다() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, mock(FilterChain.class));

        assertThat(MDC.get("traceId")).isNull();
    }

    @Test
    @DisplayName("필터_우선순위가_HIGHEST_PRECEDENCE이다")
    void 필터_우선순위가_HIGHEST_PRECEDENCE이다() {
        Order order = MdcFilter.class.getAnnotation(Order.class);

        assertThat(order).isNotNull();
        assertThat(order.value()).isEqualTo(Ordered.HIGHEST_PRECEDENCE);
    }
}
