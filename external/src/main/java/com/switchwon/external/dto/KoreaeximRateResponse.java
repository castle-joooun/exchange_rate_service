package com.switchwon.external.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 한국수출입은행 환율 API 응답 한 건.
 *
 * 응답 예시:
 * <pre>
 * {
 *   "result": 1,
 *   "cur_unit": "USD",
 *   "cur_nm": "미국 달러",
 *   "ttb": "1474.47",
 *   "tts": "1480.43",
 *   "deal_bas_r": "1477.45"
 * }
 * </pre>
 *
 * - result: 1=성공, 2=DATA코드 오류, 3=인증코드 오류, 4=일일제한 초과
 * - cur_unit: USD / JPY(100) / CNH / EUR
 * - deal_bas_r: 매매기준율
 * - ttb: 전신환 매입율 (외부 제공값, 우리는 사용하지 않고 매매기준율 × 0.95로 재계산)
 * - tts: 전신환 매도율 (동일하게 사용하지 않음)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record KoreaeximRateResponse(
        @JsonProperty("result") Integer result,
        @JsonProperty("cur_unit") String curUnit,
        @JsonProperty("cur_nm") String curName,
        @JsonProperty("deal_bas_r") String dealBasRate,
        @JsonProperty("ttb") String ttb,
        @JsonProperty("tts") String tts
) {

    public static final int RESULT_SUCCESS = 1;

    public boolean isSuccess() {
        return result != null && result == RESULT_SUCCESS;
    }
}
