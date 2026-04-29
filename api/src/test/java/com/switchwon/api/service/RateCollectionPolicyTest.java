package com.switchwon.api.service;

import com.switchwon.external.config.KoreaeximProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RateCollectionPolicyTest {

    private final KoreaeximProperties properties = new KoreaeximProperties(
            "https://example.com", "k", 3000, 5000, List.of(12, 15, 18, 21));

    private final RateCollectionPolicy policy = new RateCollectionPolicy(Clock.systemDefaultZone(), properties);

    @Test
    @DisplayName("평일_12시_0분은_CALL_API다")
    void 평일_12시_0분은_CALL_API다() {
        LocalDateTime tuesday12 = LocalDateTime.of(2026, 4, 28, 12, 0);

        assertThat(policy.decideAt(tuesday12)).isEqualTo(CollectionDecision.CALL_API);
    }

    @Test
    @DisplayName("평일_15시_18시_21시_0분도_모두_CALL_API다")
    void 평일_15시_18시_21시_0분도_모두_CALL_API다() {
        LocalDateTime tuesday = LocalDateTime.of(2026, 4, 28, 15, 0);
        assertThat(policy.decideAt(tuesday)).isEqualTo(CollectionDecision.CALL_API);
        assertThat(policy.decideAt(tuesday.withHour(18))).isEqualTo(CollectionDecision.CALL_API);
        assertThat(policy.decideAt(tuesday.withHour(21))).isEqualTo(CollectionDecision.CALL_API);
    }

    @Test
    @DisplayName("평일_호출시각이_아니면_MOCK_FROM_LATEST다")
    void 평일_호출시각이_아니면_MOCK_FROM_LATEST다() {
        LocalDateTime tuesday11 = LocalDateTime.of(2026, 4, 28, 11, 0);
        LocalDateTime tuesday14 = LocalDateTime.of(2026, 4, 28, 14, 0);
        LocalDateTime tuesday22 = LocalDateTime.of(2026, 4, 28, 22, 0);

        assertThat(policy.decideAt(tuesday11)).isEqualTo(CollectionDecision.MOCK_FROM_LATEST);
        assertThat(policy.decideAt(tuesday14)).isEqualTo(CollectionDecision.MOCK_FROM_LATEST);
        assertThat(policy.decideAt(tuesday22)).isEqualTo(CollectionDecision.MOCK_FROM_LATEST);
    }

    @Test
    @DisplayName("평일_호출시각이라도_분이_0이_아니면_MOCK_FROM_LATEST다")
    void 평일_호출시각이라도_분이_0이_아니면_MOCK_FROM_LATEST다() {
        LocalDateTime tuesday12_01 = LocalDateTime.of(2026, 4, 28, 12, 1);
        LocalDateTime tuesday12_30 = LocalDateTime.of(2026, 4, 28, 12, 30);
        LocalDateTime tuesday12_59 = LocalDateTime.of(2026, 4, 28, 12, 59);

        assertThat(policy.decideAt(tuesday12_01)).isEqualTo(CollectionDecision.MOCK_FROM_LATEST);
        assertThat(policy.decideAt(tuesday12_30)).isEqualTo(CollectionDecision.MOCK_FROM_LATEST);
        assertThat(policy.decideAt(tuesday12_59)).isEqualTo(CollectionDecision.MOCK_FROM_LATEST);
    }

    @Test
    @DisplayName("토요일은_호출시각_0분이라도_MOCK_FROM_LATEST다")
    void 토요일은_호출시각_0분이라도_MOCK_FROM_LATEST다() {
        // 2026-04-25는 토요일
        LocalDateTime saturday12 = LocalDateTime.of(2026, 4, 25, 12, 0);
        LocalDateTime saturday15 = LocalDateTime.of(2026, 4, 25, 15, 0);

        assertThat(policy.decideAt(saturday12)).isEqualTo(CollectionDecision.MOCK_FROM_LATEST);
        assertThat(policy.decideAt(saturday15)).isEqualTo(CollectionDecision.MOCK_FROM_LATEST);
    }

    @Test
    @DisplayName("일요일은_호출시각_0분이라도_MOCK_FROM_LATEST다")
    void 일요일은_호출시각_0분이라도_MOCK_FROM_LATEST다() {
        // 2026-04-26은 일요일
        LocalDateTime sunday12 = LocalDateTime.of(2026, 4, 26, 12, 0);

        assertThat(policy.decideAt(sunday12)).isEqualTo(CollectionDecision.MOCK_FROM_LATEST);
    }

    @Test
    @DisplayName("callHours가_비어있으면_평일_호출시각이라도_MOCK_FROM_LATEST다")
    void callHours가_비어있으면_평일_호출시각이라도_MOCK_FROM_LATEST다() {
        KoreaeximProperties empty = new KoreaeximProperties("u", "k", 1, 1, List.of());
        RateCollectionPolicy degraded = new RateCollectionPolicy(Clock.systemDefaultZone(), empty);

        LocalDateTime tuesday12 = LocalDateTime.of(2026, 4, 28, 12, 0);

        assertThat(degraded.decideAt(tuesday12)).isEqualTo(CollectionDecision.MOCK_FROM_LATEST);
    }

    @Test
    @DisplayName("callHours가_null이어도_안전하게_MOCK_FROM_LATEST다")
    void callHours가_null이어도_안전하게_MOCK_FROM_LATEST다() {
        KoreaeximProperties nullHours = new KoreaeximProperties("u", "k", 1, 1, null);
        RateCollectionPolicy degraded = new RateCollectionPolicy(Clock.systemDefaultZone(), nullHours);

        LocalDateTime tuesday12 = LocalDateTime.of(2026, 4, 28, 12, 0);

        assertThat(degraded.decideAt(tuesday12)).isEqualTo(CollectionDecision.MOCK_FROM_LATEST);
    }

    @Test
    @DisplayName("decide는_주입된_Clock으로부터_현재_시각을_읽는다")
    void decide는_주입된_Clock으로부터_현재_시각을_읽는다() {
        // 2026-04-28 화요일 12:00 KST 고정 Clock
        Clock fixed = Clock.fixed(
                LocalDateTime.of(2026, 4, 28, 12, 0)
                        .atZone(java.time.ZoneId.systemDefault()).toInstant(),
                java.time.ZoneId.systemDefault());
        RateCollectionPolicy fixedPolicy = new RateCollectionPolicy(fixed, properties);

        assertThat(fixedPolicy.decide()).isEqualTo(CollectionDecision.CALL_API);
    }
}
