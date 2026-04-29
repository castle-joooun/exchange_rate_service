package com.switchwon.external.handler;

import com.switchwon.common.exception.BusinessException;
import com.switchwon.external.exception.ExternalErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.client.MockClientHttpResponse;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KoreaeximResponseErrorHandlerTest {

    private final KoreaeximResponseErrorHandler handler = new KoreaeximResponseErrorHandler();
    private final URI url = URI.create("https://oapi.koreaexim.go.kr/site/program/financial/exchangeJSON?authkey=secret&searchdate=20260428&data=AP01");

    @Test
    @DisplayName("hasError는_4xx와_5xx에서만_true를_반환한다")
    void hasError는_4xx와_5xx에서만_true를_반환한다() throws Exception {
        assertThat(handler.hasError(new MockClientHttpResponse(new byte[0], HttpStatus.OK))).isFalse();
        assertThat(handler.hasError(new MockClientHttpResponse(new byte[0], HttpStatus.BAD_REQUEST))).isTrue();
        assertThat(handler.hasError(new MockClientHttpResponse(new byte[0], HttpStatus.INTERNAL_SERVER_ERROR))).isTrue();
    }

    @Test
    @DisplayName("HTTP_401은_EXTERNAL_API_AUTH_FAILED_BusinessException으로_변환된다")
    void HTTP_401은_EXTERNAL_API_AUTH_FAILED_BusinessException으로_변환된다() {
        MockClientHttpResponse response = new MockClientHttpResponse("unauthorized".getBytes(), HttpStatus.UNAUTHORIZED);

        assertThatThrownBy(() -> handler.handleError(url, HttpMethod.GET, response))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ExternalErrorCode.EXTERNAL_API_AUTH_FAILED);
    }

    @Test
    @DisplayName("HTTP_403도_EXTERNAL_API_AUTH_FAILED로_변환된다")
    void HTTP_403도_EXTERNAL_API_AUTH_FAILED로_변환된다() {
        MockClientHttpResponse response = new MockClientHttpResponse(new byte[0], HttpStatus.FORBIDDEN);

        assertThatThrownBy(() -> handler.handleError(url, HttpMethod.GET, response))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ExternalErrorCode.EXTERNAL_API_AUTH_FAILED);
    }

    @Test
    @DisplayName("HTTP_400은_EXTERNAL_API_UNAVAILABLE로_변환된다")
    void HTTP_400은_EXTERNAL_API_UNAVAILABLE로_변환된다() {
        MockClientHttpResponse response = new MockClientHttpResponse(new byte[0], HttpStatus.BAD_REQUEST);

        assertThatThrownBy(() -> handler.handleError(url, HttpMethod.GET, response))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ExternalErrorCode.EXTERNAL_API_UNAVAILABLE);
    }

    @Test
    @DisplayName("HTTP_500은_EXTERNAL_API_UNAVAILABLE로_변환된다")
    void HTTP_500은_EXTERNAL_API_UNAVAILABLE로_변환된다() {
        MockClientHttpResponse response = new MockClientHttpResponse("server boom".getBytes(), HttpStatus.INTERNAL_SERVER_ERROR);

        assertThatThrownBy(() -> handler.handleError(url, HttpMethod.GET, response))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ExternalErrorCode.EXTERNAL_API_UNAVAILABLE);
    }

    @Test
    @DisplayName("HTTP_503도_EXTERNAL_API_UNAVAILABLE로_변환된다")
    void HTTP_503도_EXTERNAL_API_UNAVAILABLE로_변환된다() {
        MockClientHttpResponse response = new MockClientHttpResponse(new byte[0], HttpStatus.SERVICE_UNAVAILABLE);

        assertThatThrownBy(() -> handler.handleError(url, HttpMethod.GET, response))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ExternalErrorCode.EXTERNAL_API_UNAVAILABLE);
    }

    @Test
    @DisplayName("HTTP_2xx는_hasError가_false를_반환한다")
    void HTTP_2xx는_hasError가_false를_반환한다() throws Exception {
        assertThat(handler.hasError(new MockClientHttpResponse(new byte[0], HttpStatus.OK))).isFalse();
        assertThat(handler.hasError(new MockClientHttpResponse(new byte[0], HttpStatus.CREATED))).isFalse();
        assertThat(handler.hasError(new MockClientHttpResponse(new byte[0], HttpStatus.NO_CONTENT))).isFalse();
    }

    @Test
    @DisplayName("HTTP_3xx는_hasError가_false를_반환한다")
    void HTTP_3xx는_hasError가_false를_반환한다() throws Exception {
        assertThat(handler.hasError(new MockClientHttpResponse(new byte[0], HttpStatus.MOVED_PERMANENTLY))).isFalse();
        assertThat(handler.hasError(new MockClientHttpResponse(new byte[0], HttpStatus.FOUND))).isFalse();
    }

    @Test
    @DisplayName("body_읽기에_실패해도_핸들러는_BusinessException으로_정상_변환한다")
    void body_읽기에_실패해도_핸들러는_BusinessException으로_정상_변환한다() {
        // body 스트림이 깨진 응답
        MockClientHttpResponse response = new MockClientHttpResponse(failingInputStream(), HttpStatus.INTERNAL_SERVER_ERROR);

        assertThatThrownBy(() -> handler.handleError(url, HttpMethod.GET, response))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ExternalErrorCode.EXTERNAL_API_UNAVAILABLE);
    }

    @Test
    @DisplayName("URL이_null이어도_NPE_없이_BusinessException으로_변환한다")
    void URL이_null이어도_NPE_없이_BusinessException으로_변환한다() {
        MockClientHttpResponse response = new MockClientHttpResponse(new byte[0], HttpStatus.INTERNAL_SERVER_ERROR);

        assertThatThrownBy(() -> handler.handleError(null, null, response))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ExternalErrorCode.EXTERNAL_API_UNAVAILABLE);
    }

    @Test
    @DisplayName("구버전_시그니처_handleError_response_단일_파라미터도_정상_동작한다")
    void 구버전_시그니처_handleError도_정상_동작한다() {
        MockClientHttpResponse response = new MockClientHttpResponse(new byte[0], HttpStatus.UNAUTHORIZED);

        assertThatThrownBy(() -> handler.handleError(response))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ExternalErrorCode.EXTERNAL_API_AUTH_FAILED);
    }

    @Test
    @DisplayName("hasError_검사_자체는_예외를_던지지_않는다")
    void hasError_검사_자체는_예외를_던지지_않는다() {
        MockClientHttpResponse response = new MockClientHttpResponse(new byte[0], HttpStatus.NOT_FOUND);

        assertThatCode(() -> handler.hasError(response)).doesNotThrowAnyException();
    }

    private static InputStream failingInputStream() {
        return new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("simulated read failure");
            }

            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                throw new IOException("simulated read failure");
            }
        };
    }
}
