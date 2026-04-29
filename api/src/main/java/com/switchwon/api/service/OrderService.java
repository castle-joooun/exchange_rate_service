package com.switchwon.api.service;

import com.switchwon.api.domain.Currency;
import com.switchwon.api.domain.ExchangeRateHistory;
import com.switchwon.api.domain.Order;
import com.switchwon.api.domain.OrderType;
import com.switchwon.api.dto.OrderCreatedResponse;
import com.switchwon.api.dto.OrderListItemResponse;
import com.switchwon.api.dto.OrderListResponse;
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
import java.util.List;

/**
 * 주문 처리 + 조회 서비스.
 *
 * <p><b>placeOrder (POST /order)</b>: 매수/매도 주문</p>
 * <ol>
 *   <li>주문 통화쌍 결정 (KRW ↔ 외화 외 조합은 OrderType.resolve 가 거부)</li>
 *   <li>외화의 최신 환율 조회 (없으면 EXCHANGE_RATE_NOT_FOUND)</li>
 *   <li>매수면 buyRate, 매도면 sellRate 적용</li>
 *   <li>외화 → KRW 환산 (JPY 는 100엔 단위 환율을 사용해 별도 환산)</li>
 *   <li>Order 엔티티 저장 후 응답 변환</li>
 * </ol>
 *
 * <p><b>getOrderList (GET /order/list)</b>: 주문 내역 최신순 조회</p>
 *
 * <p>트랜잭션: 클래스 기본 readOnly. 쓰기 메서드는 placeOrder 만 @Transactional 로 오버라이드.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    /**
     * JPY 환율은 100엔 단위로 저장되므로 100엔 미만 입력은 환산 시 0 KRW 가 된다.
     * 매수/매도 양방향 모두 차단한다 (1엔 매도도 환산 결과가 0이라 의미 없음).
     */
    private static final BigDecimal JPY_MIN_AMOUNT = new BigDecimal("100");

    private final ExchangeRateHistoryRepository exchangeRateRepository;
    private final OrderRepository orderRepository;

    @Transactional  // 클래스 기본 readOnly 를 override
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

    /**
     * 전체 주문 내역 조회. id 오름차순 정렬 (생성 순서와 동일).
     *
     * <p>운영 환경에서는 페이지네이션이 필수지만, 과제 스펙은 단순 List 반환을 명시한다.
     * 트랜잭션은 클래스 기본값(readOnly)을 그대로 사용한다.</p>
     */
    public OrderListResponse getOrderList() {
        log.info("{} 주문 내역 조회 시작", LoggingPatterns.BIZ_EVENT);

        List<OrderListItemResponse> items = orderRepository.findAllByOrderByIdAsc()
                .stream()
                .map(OrderListItemResponse::from)
                .toList();

        log.info("{} 주문 내역 조회 완료 count={}", LoggingPatterns.BIZ_EVENT, items.size());
        return OrderListResponse.of(items);
    }
}
