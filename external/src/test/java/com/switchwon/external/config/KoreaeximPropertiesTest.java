package com.switchwon.external.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class KoreaeximPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfig.class);

    @Test
    @DisplayName("application_프로퍼티의_값이_record_필드에_정확히_바인딩된다")
    void application_프로퍼티의_값이_record_필드에_정확히_바인딩된다() {
        contextRunner
                .withPropertyValues(
                        "external.koreaexim.base-url=https://test.example.com",
                        "external.koreaexim.auth-key=my-test-key",
                        "external.koreaexim.connect-timeout-ms=1500",
                        "external.koreaexim.read-timeout-ms=2500"
                )
                .run(context -> {
                    KoreaeximProperties props = context.getBean(KoreaeximProperties.class);
                    assertThat(props.baseUrl()).isEqualTo("https://test.example.com");
                    assertThat(props.authKey()).isEqualTo("my-test-key");
                    assertThat(props.connectTimeoutMs()).isEqualTo(1500);
                    assertThat(props.readTimeoutMs()).isEqualTo(2500);
                });
    }

    @Test
    @DisplayName("kebab_case_프로퍼티가_camelCase_record_필드로_매핑된다")
    void kebab_case_프로퍼티가_camelCase_record_필드로_매핑된다() {
        contextRunner
                .withPropertyValues(
                        "external.koreaexim.base-url=https://kebab.example.com",
                        "external.koreaexim.auth-key=kebab-key",
                        "external.koreaexim.connect-timeout-ms=3000",
                        "external.koreaexim.read-timeout-ms=5000"
                )
                .run(context -> {
                    KoreaeximProperties props = context.getBean(KoreaeximProperties.class);
                    assertThat(props.baseUrl()).isEqualTo("https://kebab.example.com");
                    assertThat(props.connectTimeoutMs()).isEqualTo(3000);
                });
    }

    @Test
    @DisplayName("환경변수_override가_yml_기본값보다_우선한다")
    void 환경변수_override가_yml_기본값보다_우선한다() {
        contextRunner
                .withPropertyValues(
                        "external.koreaexim.base-url=https://default.example.com",
                        "external.koreaexim.auth-key=DEFAULT_KEY",
                        "external.koreaexim.connect-timeout-ms=1000",
                        "external.koreaexim.read-timeout-ms=2000",
                        // 우선순위 더 높은 override
                        "external.koreaexim.auth-key=OVERRIDDEN_KEY"
                )
                .run(context -> {
                    KoreaeximProperties props = context.getBean(KoreaeximProperties.class);
                    assertThat(props.authKey()).isEqualTo("OVERRIDDEN_KEY");
                });
    }

    @EnableConfigurationProperties(KoreaeximProperties.class)
    static class TestConfig {
    }
}
