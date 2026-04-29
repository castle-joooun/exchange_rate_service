package com.switchwon.external.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class KoreaeximRateResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("result가_1이면_isSuccess는_true다")
    void result가_1이면_isSuccess는_true다() {
        KoreaeximRateResponse response = new KoreaeximRateResponse(
                1, "USD", "미국 달러", "1477.45", "1474.47", "1480.43");

        assertThat(response.isSuccess()).isTrue();
    }

    @Test
    @DisplayName("result가_1이_아니면_isSuccess는_false다")
    void result가_1이_아니면_isSuccess는_false다() {
        assertThat(new KoreaeximRateResponse(2, "USD", null, "1", "1", "1").isSuccess()).isFalse();
        assertThat(new KoreaeximRateResponse(3, "USD", null, "1", "1", "1").isSuccess()).isFalse();
        assertThat(new KoreaeximRateResponse(4, "USD", null, "1", "1", "1").isSuccess()).isFalse();
    }

    @Test
    @DisplayName("result가_null이면_isSuccess는_false다")
    void result가_null이면_isSuccess는_false다() {
        KoreaeximRateResponse response = new KoreaeximRateResponse(
                null, "USD", null, "1", "1", "1");

        assertThat(response.isSuccess()).isFalse();
    }

    @Test
    @DisplayName("snake_case_JSON이_record_필드로_정확히_매핑된다")
    void snake_case_JSON이_record_필드로_정확히_매핑된다() throws Exception {
        String json = """
                {
                  "result": 1,
                  "cur_unit": "USD",
                  "cur_nm": "미국 달러",
                  "ttb": "1474.47",
                  "tts": "1480.43",
                  "deal_bas_r": "1477.45"
                }
                """;

        KoreaeximRateResponse response = objectMapper.readValue(json, KoreaeximRateResponse.class);

        assertThat(response.result()).isEqualTo(1);
        assertThat(response.curUnit()).isEqualTo("USD");
        assertThat(response.curName()).isEqualTo("미국 달러");
        assertThat(response.ttb()).isEqualTo("1474.47");
        assertThat(response.tts()).isEqualTo("1480.43");
        assertThat(response.dealBasRate()).isEqualTo("1477.45");
    }

    @Test
    @DisplayName("JPY_100_표기도_cur_unit에_그대로_들어온다")
    void JPY_100_표기도_cur_unit에_그대로_들어온다() throws Exception {
        String json = """
                {"result":1,"cur_unit":"JPY(100)","cur_nm":"일본 옌","ttb":"905.80","tts":"915.20","deal_bas_r":"910.50"}
                """;

        KoreaeximRateResponse response = objectMapper.readValue(json, KoreaeximRateResponse.class);

        assertThat(response.curUnit()).isEqualTo("JPY(100)");
        assertThat(response.dealBasRate()).isEqualTo("910.50");
    }

    @Test
    @DisplayName("응답에_정의되지_않은_필드가_있어도_무시되고_매핑된다")
    void 응답에_정의되지_않은_필드가_있어도_무시되고_매핑된다() throws Exception {
        String json = """
                {
                  "result": 1,
                  "cur_unit": "USD",
                  "cur_nm": "미국 달러",
                  "deal_bas_r": "1477.45",
                  "ttb": "1474.47",
                  "tts": "1480.43",
                  "future_field": "ignore-me",
                  "another_unknown": 99
                }
                """;

        KoreaeximRateResponse response = objectMapper.readValue(json, KoreaeximRateResponse.class);

        assertThat(response.curUnit()).isEqualTo("USD");
        assertThat(response.dealBasRate()).isEqualTo("1477.45");
    }

    @Test
    @DisplayName("배열_응답도_List로_정상_매핑된다")
    void 배열_응답도_List로_정상_매핑된다() throws Exception {
        String json = """
                [
                  {"result":1,"cur_unit":"USD","cur_nm":"미국 달러","ttb":"1474.47","tts":"1480.43","deal_bas_r":"1477.45"},
                  {"result":1,"cur_unit":"JPY(100)","cur_nm":"일본 옌","ttb":"905.80","tts":"915.20","deal_bas_r":"910.50"}
                ]
                """;

        List<KoreaeximRateResponse> responses = objectMapper.readValue(
                json, objectMapper.getTypeFactory().constructCollectionType(List.class, KoreaeximRateResponse.class));

        assertThat(responses).hasSize(2);
        assertThat(responses).extracting(KoreaeximRateResponse::curUnit)
                .containsExactly("USD", "JPY(100)");
    }
}
