package com.switchwon.external.handler;

import com.switchwon.common.exception.BusinessException;
import com.switchwon.common.log.LoggingPatterns;
import com.switchwon.external.exception.ExternalErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.ResponseErrorHandler;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * 한국수출입은행 API의 HTTP 4xx/5xx 응답을 도메인 예외로 변환한다.
 *
 * RestClient 가 응답 status 를 체크할 때 호출되며, 이 핸들러가 던진 예외는
 * Resilience4j 서킷브레이커가 실패로 카운트하여 차단 회로를 작동시킨다.
 */
@Slf4j
public class KoreaeximResponseErrorHandler implements ResponseErrorHandler {

    @Override
    public boolean hasError(ClientHttpResponse response) throws IOException {
        return response.getStatusCode().isError();
    }

    @Override
    public void handleError(ClientHttpResponse response) throws IOException {
        handleError(null, null, response);
    }

    @Override
    public void handleError(URI url, org.springframework.http.HttpMethod method, ClientHttpResponse response)
            throws IOException {
        HttpStatusCode status = response.getStatusCode();
        String body = readBodySafely(response);

        log.warn("{} 외부 API 호출 실패 method={} url={} status={} body={}",
                LoggingPatterns.SYS_ERROR,
                method,
                maskAuthKey(url),
                status.value(),
                truncate(body, 300));

        if (status.is4xxClientError()) {
            if (status.value() == 401 || status.value() == 403) {
                throw new BusinessException(ExternalErrorCode.EXTERNAL_API_AUTH_FAILED,
                        "한국수출입은행 API 인증 실패: HTTP " + status.value());
            }
            throw new BusinessException(ExternalErrorCode.EXTERNAL_API_UNAVAILABLE,
                    "한국수출입은행 API 4xx 오류: HTTP " + status.value());
        }
        // 5xx
        throw new BusinessException(ExternalErrorCode.EXTERNAL_API_UNAVAILABLE,
                "한국수출입은행 API 5xx 오류: HTTP " + status.value());
    }

    private String readBodySafely(ClientHttpResponse response) {
        try (var is = response.getBody()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "<body 읽기 실패: " + e.getClass().getSimpleName() + ">";
        }
    }

    private static String maskAuthKey(URI url) {
        return Optional.ofNullable(url)
                .map(URI::toString)
                .map(s -> s.replaceAll("authkey=[^&]+", "authkey=***"))
                .orElse("null");
    }

    private static String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max) + "...(truncated)";
    }
}
