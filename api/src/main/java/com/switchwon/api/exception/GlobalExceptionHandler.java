package com.switchwon.api.exception;

import com.switchwon.common.exception.BusinessException;
import com.switchwon.common.exception.CommonErrorCode;
import com.switchwon.common.exception.ErrorCode;
import com.switchwon.common.log.LoggingPatterns;
import com.switchwon.common.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * 모든 컨트롤러의 예외를 가로채서 ApiResponse 형식으로 변환한다.
 *
 * <p>분기 정책:</p>
 * <ul>
 *   <li>BusinessException — ErrorCode 의 HttpStatus 그대로 사용</li>
 *   <li>Validation 실패 (@Valid) — 400, 첫 번째 violation 메시지</li>
 *   <li>PathVariable enum 변환 실패 — 400</li>
 *   <li>Request body 파싱 실패 — 400</li>
 *   <li>그 외 — 500 INTERNAL_ERROR</li>
 * </ul>
 *
 * <p>응답에 traceId 가 자동 포함된다 (MdcFilter 가 MDC 에 주입).</p>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        ErrorCode errorCode = e.getErrorCode();
        log.warn("{} 도메인 예외 code={} message={}",
                LoggingPatterns.BIZ_FAIL, errorCode.getCode(), e.getMessage());

        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ApiResponse.error(errorCode, e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException e) {
        FieldError firstError = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .orElse(null);
        String message = (firstError != null)
                ? String.format("%s: %s", firstError.getField(), firstError.getDefaultMessage())
                : "요청 값이 올바르지 않습니다";

        log.warn("{} 입력 값 검증 실패 message={}", LoggingPatterns.BIZ_FAIL, message);

        return ResponseEntity
                .status(CommonErrorCode.INVALID_DATA.getHttpStatus())
                .body(ApiResponse.error(CommonErrorCode.INVALID_DATA, message));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        String message = String.format("'%s' 값이 올바르지 않습니다: %s",
                e.getName(), e.getValue());

        log.warn("{} PathVariable/RequestParam 타입 변환 실패 param={} value={}",
                LoggingPatterns.BIZ_FAIL, e.getName(), e.getValue());

        return ResponseEntity
                .status(CommonErrorCode.INVALID_DATA.getHttpStatus())
                .body(ApiResponse.error(CommonErrorCode.INVALID_DATA, message));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotReadable(HttpMessageNotReadableException e) {
        log.warn("{} 요청 본문 파싱 실패 reason={}",
                LoggingPatterns.BIZ_FAIL, e.getMostSpecificCause().getClass().getSimpleName());

        return ResponseEntity
                .status(CommonErrorCode.INVALID_DATA.getHttpStatus())
                .body(ApiResponse.error(CommonErrorCode.INVALID_DATA, "요청 본문 형식이 올바르지 않습니다"));
    }

    /**
     * 다른 핸들러에 매칭되지 않은 모든 예외 — fallback.
     * Spring 은 가장 구체적인 핸들러를 먼저 매칭하므로 BusinessException 등은 위 핸들러가 잡고,
     * 정말로 예상치 못한 경우만 여기로 떨어진다.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception e) {
        log.error("{} 예상치 못한 시스템 오류 type={}",
                LoggingPatterns.SYS_ERROR, e.getClass().getSimpleName(), e);

        return ResponseEntity
                .status(CommonErrorCode.INTERNAL_ERROR.getHttpStatus())
                .body(ApiResponse.error(CommonErrorCode.INTERNAL_ERROR));
    }
}
