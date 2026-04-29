package com.switchwon.api.service;

import com.switchwon.api.domain.Currency;
import com.switchwon.api.domain.ExchangeRateHistory;
import com.switchwon.api.domain.Order;
import com.switchwon.api.domain.OrderType;
import com.switchwon.api.dto.OrderCreatedResponse;
import com.switchwon.api.dto.OrderRequest;
import com.switchwon.api.exception.ExchangeRateErrorCode;
import com.switchwon.api.exception.OrderErrorCode;
import com.switchwon.api.repository.ExchangeRateHistoryRepository;
import com.switchwon.api.repository.OrderRepository;
import com.switchwon.common.exception.BusinessException;
import com.switchwon.common.log.LoggingPatterns;
import com.switchwon.common.util.MoneyCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * 외화 매수/매도 주문 처리 서비스.
 *
 * <ol>
 *   <li>주문 통화쌍 결정 (KRW ↔ 외화 외 조합은 OrderType.resolve 가 거부)</li>
 *   <li>외화의 최신 환율 조회 (없으면 EXCHANGE_RATE_NOT_FOUND)</li>
 *   <li>매수면 buyRate, 매도면 sellRate 적용</li>
 *   <li>외화 → KRW 환산 (JPY 는 100엔 단위 환율을 사용해 별도 환산)</li>
 *   <li>Order 엔티티 저장 후 응답 변환</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    /**
     * JPY 환율은 100엔 단위로 저장되므로 100엔 미만 입력은 환산 시 0 KRW 가 된다.
     * 매수/매도 양방향 모두 차단한다 (1엔 매도도 환산 결과가 0이라 의미 없음).
     */
    private static final BigDecimal JPY_MIN_AMOUNT = new BigDecimal("100");

    private final ExchangeRateHistoryRepository exchangeRateRepository;
    private final OrderRepository orderRepository;

    @Transactional
    public OrderCreatedResponse placeOrder(OrderRequest request) {
        log.info("{} 주문 처리 시작 forexAmount={} from={} to={}",
                LoggingPatterns.BIZ_EVENT,
                request.forexAmount(), request.fromCurrency(), request.toCurrency());

        // 1. 통화쌍 검증 (외화는 어느 쪽인가?)
        OrderType orderType = OrderType.resolve(request.fromCurrency(), request.toCurrency());
        Currency forexCurrency = (request.fromCurrency() == Currency.KRW)
                ? request.toCurrency()
                : request.fromCurrency();

        // 2. JPY 100엔 미만 차단 (매수/매도 양방향)
        if (forexCurrency == Currency.JPY && request.forexAmount().compareTo(JPY_MIN_AMOUNT) < 0) {
            log.warn("{} JPY 주문 100엔 미만 거부 forexAmount={}",
                    LoggingPatterns.BIZ_FAIL, request.forexAmount());
            throw new BusinessException(
                    OrderErrorCode.JPY_AMOUNT_BELOW_MIN_UNIT,
                    "JPY 주문 금액 부족: " + request.forexAmount());
        }

        // 3. 외화의 최신 환율 조회
        ExchangeRateHistory rate = exchangeRateRepository
                .findTopByCurrencyOrderByCollectedAtDesc(forexCurrency)
                .orElseThrow(() -> {
                    log.warn("{} 주문 처리 실패 환율_없음 currency={}",
                            LoggingPatterns.BIZ_FAIL, forexCurrency);
                    return new BusinessException(
                            ExchangeRateErrorCode.EXCHANGE_RATE_NOT_FOUND,
                            "환율 정보 없음: currency=" + forexCurrency);
                });

        // 4. 적용 환율 결정
        BigDecimal appliedRate = (orderType == OrderType.BUY) ? rate.getBuyRate() : rate.getSellRate();

        // 5. KRW 환산 (JPY 는 100엔 단위 환율이므로 별도 분기)
        BigDecimal krwAmount = (forexCurrency == Currency.JPY)
                ? MoneyCalculator.toKrwFromJpy(request.forexAmount(), appliedRate)
                : MoneyCalculator.toKrw(request.forexAmount(), appliedRate);

        // 6. fromAmount / toAmount 결정
        BigDecimal fromAmount;
        BigDecimal toAmount;
        if (orderType == OrderType.BUY) {
            // KRW → 외화. KRW 가 출금, 외화가 입금
            fromAmount = krwAmount;
            toAmount = request.forexAmount();
        } else {
            // 외화 → KRW. 외화가 출금, KRW 가 입금
            fromAmount = request.forexAmount();
            toAmount = krwAmount;
        }

        // 7. Order 저장
        Order order = Order.create(
                fromAmount, request.fromCurrency(),
                toAmount, request.toCurrency(),
                appliedRate);
        Order saved = orderRepository.save(order);

        log.info("{} 주문 처리 완료 orderId={} type={} forexAmount={}({}) from={}({}) to={}({}) rate={}",
                LoggingPatterns.BIZ_EVENT,
                saved.getId(), orderType,
                request.forexAmount(), forexCurrency,
                fromAmount, request.fromCurrency(),
                toAmount, request.toCurrency(),
                appliedRate);

        return OrderCreatedResponse.from(saved);
    }
}
