package com.switchwon.api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * springdoc-openapi 설정. /swagger-ui 와 /api-docs 엔드포인트가 노출된다.
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI switchWonOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("스위치원 외환 주문 시스템 API")
                        .version("v1.0")
                        .description("""
                                실시간 환율 기반 외화 매수/매도 주문 API.

                                - 환율 조회: USD/JPY/CNY/EUR 4개 통화의 최신 환율
                                - 주문: KRW ↔ 외화 매수/매도, 주문 시점 환율 스냅샷 보존
                                - 환율 수집: 매분 스케줄러, 평일 12/15/18/21시에 외부 API 호출 (그 외엔 ±0.3% 랜덤 변동)
                                """)
                        .contact(new Contact()
                                .name("스위치원 백엔드 사전과제")
                                .email("tjdwns0309@gmail.com")));
    }
}
