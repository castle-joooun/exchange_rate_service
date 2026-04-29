# 스위치원 백엔드 사전과제 — 실시간 환율 기반 외환 주문 시스템

한국수출입은행 환율 API를 1분 단위로 수집·이력 관리하고, 4개 통화(USD/JPY/CNY/EUR)에 대해 사용자가 KRW↔외화 매수/매도 주문을 실행할 수 있는 백엔드.

---

## 1. 실행 방법

### 사전 요구사항
- Java 17 (Gradle wrapper가 17 toolchain으로 컴파일하지만, **Gradle 자체 실행 JVM도 17 권장**. Java 26+ 환경에선 Gradle 8.10.2가 미지원)

```bash
# 17이 아닌 환경이면 명시
export JAVA_HOME=$(/usr/libexec/java_home -v 17)

# 빌드 + 테스트
./gradlew clean build

# 실행
./gradlew :api:bootRun
```

### 접속 포인트
| 용도 | URL |
|---|---|
| Swagger UI | <http://localhost:8080/swagger-ui/index.html> |
| H2 콘솔 | <http://localhost:8080/h2-console> |
| API 시작 | <http://localhost:8080/exchange-rate/latest> |

H2 콘솔 접속 정보:
- JDBC URL: `jdbc:h2:mem:switchwon;DB_CLOSE_DELAY=-1;MODE=MySQL`
- User: `sa`, Password: (빈 값)

---

## 2. 시스템 설계 추정치

### 가정
- 국내 외환 서비스 중소 규모
- MAU 100,000명 / DAU 10,000명 (10% 활성)
- 1인 평균 일 5회 환율 조회, 0.3회 주문

### QPS / TPS
| 항목 | 일 호출수 | 평균 | 피크 (×10) |
|---|---|---|---|
| 환율 조회 | 50,000 | 0.6 RPS | **6 RPS** |
| 주문 | 3,000 | 0.035 TPS | **0.35 TPS** |
| 환율 수집 (스케줄러) | 5,760 write/day | 분당 4건 | 분당 4건 |

### Storage (1년 기준)
- 환율 이력: 4통화 × 1440분 × 365일 = **210만 row × ~80B = 170MB/년**
- 주문 내역: 3000 × 365 = 110만 row × ~120B = **130MB/년**
- 합산 **300MB/년** → 단일 RDB로 충분

### 의사결정 근거
- 캐시는 **Local Caffeine으로 충분** (분산 캐시 불필요)
- DB 샤딩 불필요, 단일 RDB + 인덱스 최적화로 대응
- 환율 조회 피크 6 RPS → 캐시 hit 시 1ms 미만 응답 가능

---

## 3. 모듈 구조 + Layered Architecture

```
switch-won/
├── api/        # REST 진입점 + 비즈니스 로직 + 도메인
│   ├── controller/    # REST Controller
│   ├── service/       # Service Layer (수집/조회/주문)
│   ├── repository/    # Spring Data JPA Repository
│   ├── domain/        # Entity (Order, ExchangeRateHistory) + Enum
│   ├── dto/           # Request / Response
│   ├── exception/     # ErrorCode + GlobalExceptionHandler
│   ├── scheduler/     # 환율 수집 스케줄러
│   ├── seeder/        # 부팅 시 기준 환율 시드
│   └── config/        # CacheConfig, ClockConfig, SwaggerConfig
│
├── common/     # 공통 라이브러리
│   ├── response/      # ApiResponse (공통 응답 래퍼)
│   ├── exception/     # BaseException, ErrorCode interface, CommonErrorCode
│   ├── util/          # MoneyCalculator (정밀도 단일 진입점)
│   ├── log/           # MdcFilter (traceId), LoggingPatterns
│   └── config/        # ObjectMapperConfig (LocalDateTime 포맷)
│
└── external/   # 외부 API 호출
    ├── service/       # ExchangeRateClient + KoreaeximExchangeRateClient
    ├── dto/           # 외부 응답 record + DailyExchangeRate
    ├── handler/       # ResponseErrorHandler (HTTP 에러 변환)
    ├── exception/     # ExternalErrorCode
    └── config/        # KoreaeximProperties, RestClientConfig
```

**의존성 방향**: `api → external → common`, `api → common` (단방향, 역방향 import 없음)

---

## 4. API 명세

상세는 Swagger UI에서 확인. 핵심 4개:

| 메서드 | 경로 | 설명 |
|---|---|---|
| GET | `/exchange-rate/latest` | 4통화 최신 환율 일괄 조회 (캐시 60s) |
| GET | `/exchange-rate/latest/{currency}` | 단일 통화 최신 환율 |
| POST | `/order` | 외화 매수/매도 주문 |
| GET | `/order/list` | 주문 내역 조회 (id 오름차순) |

**공통 응답 포맷**:
```json
{ "code": "OK", "message": "SUCCESS", "returnObject": {...} }
```

**에러 응답 코드 체계** (prefix 기반):

| Prefix | 영역 |
|---|---|
| `COMMON_xxx` | 공통 검증/시스템 오류 |
| `DOMAIN_xxx` | 도메인 규칙 위반 (통화 조합 등) |
| `RATE_xxx` | 환율 조회 |
| `ORDER_xxx` | 주문 |
| `EXTERNAL_xxx` | 외부 API |

---

## 5. 도메인 핵심 규칙

### 환산 정밀도 — `common/util/MoneyCalculator`
모든 BigDecimal 산술은 이 클래스만 경유한다 (직접 계산 금지).

| 규칙 | 동작 |
|---|---|
| 환율 반올림 | `RoundingMode.HALF_UP`, scale 2 |
| KRW 절사 | `RoundingMode.FLOOR`, scale 0 |
| buyRate | 매매기준율 × 1.05 → 둘째자리 반올림 |
| sellRate | 매매기준율 × 0.95 → 둘째자리 반올림 |
| JPY 환산 | 100엔 단위 환율 사용 (forexAmount × rate / 100, floor) |

### 환율 수집 정책 — `api/service/RateCollectionPolicy`
스케줄러는 매분 발화하지만, 외부 API 호출은 정해진 시각에만:

```
if (평일 AND 분=0 AND 시각 ∈ {12, 15, 18, 21})
    → CALL_API   (평일 4회/일)
else
    → MOCK_FROM_LATEST   (DB 최신 row 기반 ±0.3% 랜덤 변동)
```

→ 외부 API 호출량 최소화 (쿼터/비용 의식) + 분 단위 시세 변동 요구사항 충족.

### 부팅 시 환율 시드 — `api/seeder/InitialRateSeeder`
앱 시작 시 DB 비어있으면:
1. 외부 API 호출 시도
2. 정상 응답: 4통화 외부값 사용
3. falsy(null/0)인 통화: 하드코딩 fallback (USD 1477.78 / JPY 925.43 / CNY 216.10 / EUR 1728.21)
4. 호출 실패: 4통화 모두 하드코딩

---

## 6. 어필 포인트

### 6-1. 멀티 모듈 의존성 통제
- `api → external → common` 단방향. 역방향 import 없음
- `external`은 도메인 통화를 모르고, `Currency.fromExternalCode()`는 `api`에서 매핑

### 6-2. MoneyCalculator 단일 책임
- 모든 정밀도/환산 규칙이 한 클래스에 응집
- 한글 테스트로 비즈니스 규칙 자체가 문서화

### 6-3. Resilience4j Circuit Breaker
- 외부 API 호출 실패 → 자동 fallback (`LatestRateFallbackProvider`)
- 의존성 역전: external은 `ExchangeRateFallbackProvider` 인터페이스만 정의, 구현(DB+random)은 api에서

### 6-4. Caffeine 캐시 (TTL 60s)
- 스케줄러 주기와 일치 → 명시적 evict 불필요
- `recordStats()` 활성화 → 모니터링 친화

### 6-5. 통화별 독립 트랜잭션 (부분 실패 허용)
- `ExchangeRateCollector`는 `@Transactional`을 메서드에 걸지 않음
- `JpaRepository.save()` 자체 트랜잭션으로 통화별 격리
- 한 통화 실패해도 나머지는 commit

### 6-6. 환경별 SQL 로그 분리
- `logback-spring.xml` Spring Profile 분기
- `local/dev/test/default`: SQL DEBUG + 파라미터 TRACE
- `prod`: 모두 OFF (성능 + 민감정보 보호)

### 6-7. traceId 자동 추적
- `MdcFilter`가 요청마다 traceId 발급 → MDC + 응답 헤더 `X-Trace-Id`
- 모든 로그에 자동 포함 (`[traceId=xxx]` 패턴)
- 응답 본문에는 노출하지 않음 (헤더로만 추적)

### 6-8. ErrorCode 거버넌스
- enum 가드 테스트로 prefix 중복, HttpStatus 매핑 자동 검증
- 외부 클라이언트가 `code` 문자열로 분기 시 깨지지 않는 불변 계약

### 6-9. 한글 테스트 + 한글 메서드명
- `@DisplayName` + 메서드명 모두 한글
- 평가자가 영어 → 한글 번역 부담 없이 비즈니스 의도 즉시 파악

### 6-10. Swagger 자동 문서화
- 모든 Controller `@Operation`, DTO `@Schema(description, example)`
- `/swagger-ui` 한 곳에서 전체 시연 가능

---

## 7. 테스트

```bash
./gradlew test
```

총 **200건 이상** 테스트 통과:
- common: MoneyCalculator 정밀도 규칙 검증
- external: RestClient mock + ResponseErrorHandler 분기
- api: 도메인/Repository/Service/Controller + 통합 테스트

테스트 종류:
- **단위 테스트** (Mockito) — Service/Repository 단위
- **슬라이스 테스트** (`@DataJpaTest`, `@WebMvcTest`) — JPA/Web 계층 격리
- **통합 테스트** (`@SpringBootTest`) — end-to-end 흐름 (`integration/` 패키지)

---

## 8. 구현 이슈 순서

| # | Issue | 핵심 산출물 |
|---|---|---|
| 1 | 프로젝트 초기 셋업 | 멀티 모듈 Gradle, .gitignore, CLAUDE.md |
| 2 | common 모듈 | ApiResponse, BaseException, MoneyCalculator |
| 3 | 도메인 + Repository | Currency, ExchangeRateHistory, Order |
| 4 | external 모듈 | RestClient + Resilience4j + Fallback 인터페이스 |
| 5 | 환율 수집 스케줄러 | RateCollectionPolicy, MockRateGenerator, InitialRateSeeder |
| 6 | 환율 조회 API | Caffeine 캐시 (60s TTL) |
| 7 | 주문 API (매수/매도) | OrderService, JPY 100엔 단위 환산 |
| 8 | 주문 조회 API | id 오름차순 정렬 |
| 9 | 글로벌 예외 처리 + 로깅 | GlobalExceptionHandler, logback Profile 분기 |
| 10 | Swagger 문서화 | OpenAPI Bean + Controller/DTO 어노테이션 |
| 11 | 통합 테스트 + README | end-to-end 검증 |

---

## 9. 개선 여지 (한계 인정)

이번 과제 범위에서 의도적으로 단순화한 부분과 운영 환경에서 필요한 추가 작업:

| 영역 | 현재 | 운영 필요시 |
|---|---|---|
| 주문 조회 | 단순 List 반환 | Spring Data Pageable, 필터(통화/기간) |
| 캐시 | Local Caffeine | 다중 인스턴스 환경 → Redis |
| 잔고 검증 | 없음 (과제 범위 밖) | 사용자 잔고 + 환전 한도 |
| 인증 | 없음 (과제 명시) | JWT/OAuth |
| 모니터링 | 콘솔 로그 + Actuator | Prometheus + Grafana, Sentry/Datadog |
| 로그 영속 | 콘솔만 | RollingFileAppender + 중앙 로그 수집 |
| 외부 API 호출 빈도 | 평일 4회/일 | 실시간 시세 제공처면 주기 단축 + 캐시 정책 변경 |

---

## 10. 디렉토리 구조 (전체)

```
switch-won/
├── api/                                 # 진입점 + 비즈니스
│   └── src/main/java/com/switchwon/api/
│       ├── SwitchWonApplication.java
│       ├── controller/
│       │   ├── ExchangeRateController.java
│       │   └── OrderController.java
│       ├── service/
│       │   ├── ExchangeRateService.java
│       │   ├── ExchangeRateCollector.java
│       │   ├── OrderService.java
│       │   ├── RateCollectionPolicy.java
│       │   ├── MockRateGenerator.java
│       │   ├── LatestRateFallbackProvider.java
│       │   └── CollectionDecision.java
│       ├── repository/
│       │   ├── ExchangeRateHistoryRepository.java
│       │   └── OrderRepository.java
│       ├── domain/
│       │   ├── Currency.java
│       │   ├── ExchangeRateHistory.java
│       │   ├── Order.java
│       │   └── OrderType.java
│       ├── dto/
│       │   ├── ExchangeRateResponse.java
│       │   ├── ExchangeRateListResponse.java
│       │   ├── OrderRequest.java
│       │   ├── OrderCreatedResponse.java
│       │   ├── OrderListItemResponse.java
│       │   └── OrderListResponse.java
│       ├── exception/
│       │   ├── GlobalExceptionHandler.java
│       │   ├── DomainErrorCode.java
│       │   ├── ExchangeRateErrorCode.java
│       │   └── OrderErrorCode.java
│       ├── scheduler/
│       │   └── ExchangeRateScheduler.java
│       ├── seeder/
│       │   └── InitialRateSeeder.java
│       └── config/
│           ├── CacheConfig.java
│           ├── ClockConfig.java
│           └── SwaggerConfig.java
│
├── common/                              # 공통 라이브러리
│   └── src/main/java/com/switchwon/common/
│       ├── response/ApiResponse.java
│       ├── exception/{BaseException, BusinessException, ErrorCode, CommonErrorCode}.java
│       ├── util/MoneyCalculator.java
│       ├── log/{MdcFilter, LoggingPatterns}.java
│       └── config/ObjectMapperConfig.java
│
└── external/                            # 외부 API
    └── src/main/java/com/switchwon/external/
        ├── service/
        │   ├── ExchangeRateClient.java                # 인터페이스
        │   ├── ExchangeRateFallbackProvider.java      # 인터페이스 (api 모듈이 구현)
        │   └── KoreaeximExchangeRateClient.java
        ├── dto/{KoreaeximRateResponse, DailyExchangeRate}.java
        ├── handler/KoreaeximResponseErrorHandler.java
        ├── exception/ExternalErrorCode.java
        └── config/{KoreaeximProperties, RestClientConfig}.java
```
