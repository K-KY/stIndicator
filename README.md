# stIndicator

ATR 기반 포지션 사이징과 Binance Futures 주문/조회 기능을 제공하는 Spring Boot 프로젝트입니다.

## 핵심 변경 사항

- ATR 기반 주문 미리보기와 실제 주문 실행 기능 추가
- `가용 자산 x 위험 비율(기본 1%)` 기준으로 리스크 금액 계산
- `수량 = 리스크 금액 / (ATR x ATR 배수)` 기준으로 주문 수량 계산
- 배율은 리스크 확대가 아니라 `필요 증거금 = (수량 x 진입가) / 배율` 계산에 반영
- 현재 보유 자산 목록 조회 추가
- 거래소 선물 심볼 목록 조회 추가
- 현재가 조회 추가
- 현재 진입 중인 포지션 목록 조회 추가
- 서비스 내부에서 포지션 시장가 청산 기능 추가
- 주문 저장 로직에서 `quantity`, `price` 순서가 뒤바뀌던 문제 수정

## ATR 주문 계산 규칙

ATR 주문은 아래 순서로 계산됩니다.

1. 가용 자산 조회
2. 최근 캔들 기준 ATR 계산
3. 리스크 금액 계산
4. ATR 거리 기준 주문 수량 계산
5. 배율 기준 필요 증거금 계산

공식은 아래와 같습니다.

```text
리스크 금액 = 가용 자산 x (위험 비율 / 100)
손절 거리 = ATR x ATR 배수
주문 수량 = 리스크 금액 / 손절 거리
주문 금액(Notional) = 주문 수량 x 진입가
필요 증거금 = 주문 금액 / 배율
```

예시:

```text
가용 자산 1000 USDT
위험 비율 1
ATR 4
ATR 배수 1
진입가 100
배율 10

리스크 금액 = 10
손절 거리 = 4
주문 수량 = 2.5
주문 금액 = 250
필요 증거금 = 25
```

## 환경 변수

`src/main/resources/application.properties` 기준으로 아래 값이 필요합니다.

- `BINANCE_API_KEY`
- `BINANCE_API_SECRET`
- `MYSQL_DB_URL`
- `MYSQL_DB_USER`
- `MYSQL_DB_PASSWORD`

## 실행

```bash
./gradlew bootRun
```

## 검증

ATR 계산/사이징 테스트:

```bash
./gradlew test --tests st.indicator.stindicator.application.service.AtrPositionSizingServiceTest
```

## 엔드포인트

기본 경로는 `/client` 입니다.

### 1. 캔들 조회

- Method: `GET`
- Path: `/client/candles`

Query Params:

- `symbol`: 예) `BTCUSDT`
- `interval`: 예) `1h`
- `limit`: 예) `150`

예시:

```text
GET /client/candles?symbol=BTCUSDT&interval=1h&limit=150
```

### 2. 총 지갑 잔고 조회

- Method: `GET`
- Path: `/client/balances`

예시:

```text
GET /client/balances
```

### 3. ATR 조회

- Method: `GET`
- Path: `/client/atrs`

Query Params:

- `symbol`
- `interval`
- `limit`

예시:

```text
GET /client/atrs?symbol=BTCUSDT&interval=1h&limit=150
```

### 4. 보유 자산 목록 조회

- Method: `GET`
- Path: `/client/assets`

설명:

- 지갑 내 자산 중 잔고/가용잔고/미실현손익이 0이 아닌 항목만 반환

예시:

```text
GET /client/assets
```

### 5. 거래소 코인 목록 조회

- Method: `GET`
- Path: `/client/symbols`

설명:

- Binance Futures 의 `TRADING` 상태 심볼만 반환

예시:

```text
GET /client/symbols
```

### 6. 현재가 조회

- Method: `GET`
- Path: `/client/price`

Query Params:

- `symbol`

예시:

```text
GET /client/price?symbol=BTCUSDT
```

### 7. 현재 포지션 목록 조회

- Method: `GET`
- Path: `/client/positions`

설명:

- 현재 수량이 0이 아닌 포지션만 반환

예시:

```text
GET /client/positions
```

### 8. 일반 주문 실행

- Method: `POST`
- Path: `/client/order`

Request Params:

- `symbol`
- `side`: `BUY` 또는 `SELL`
- `type`: `MARKET` 또는 `LIMIT`
- `timeInForce`: 지정가 주문일 때 사용 예) `GTC`
- `quantity`
- `price`: 지정가 주문일 때 사용

예시:

```text
POST /client/order?symbol=BTCUSDT&side=BUY&type=LIMIT&timeInForce=GTC&quantity=0.01&price=60000
```

### 9. 저장된 주문 목록 조회

- Method: `GET`
- Path: `/client/order`

Query Params:

- `symbol`

예시:

```text
GET /client/order?symbol=BTCUSDT
```

### 10. 주문 상세 조회

- Method: `GET`
- Path: `/client/order/details`

Query Params:

- `symbol`
- `orderId`

예시:

```text
GET /client/order/details?symbol=BTCUSDT&orderId=123456789
```

### 11. ATR 주문 미리보기

- Method: `GET`
- Path: `/client/atr/order/preview`

Query Params:

- `symbol`: 예) `BTCUSDT`
- `side`: `BUY` 또는 `SELL`
- `interval`: 기본값 `1h`
- `limit`: 기본값 `150`
- `atrPeriod`: 기본값 `14`
- `riskPercent`: 기본값 `1`
- `atrMultiplier`: 기본값 `1`
- `leverage`: 기본값 `1`
- `type`: 기본값 `MARKET`
- `timeInForce`: 필요 시 전달
- `entryPrice`: 생략 시 현재가 사용

예시:

```text
GET /client/atr/order/preview?symbol=BTCUSDT&side=BUY&interval=1h&limit=150&atrPeriod=14&riskPercent=1&atrMultiplier=1&leverage=10
```

응답에는 아래 값들이 포함됩니다.

- `availableBalance`
- `entryPrice`
- `atr`
- `atrMultiplier`
- `stopDistance`
- `riskPercent`
- `riskAmount`
- `leverage`
- `quantity`
- `notional`
- `requiredMargin`
- `stopPrice`
- `targetPrice`
- `possibleLoss`
- `possibleProfit`

### 12. ATR 기준 주문 실행

- Method: `POST`
- Path: `/client/atr/order`

Request Params:

- `symbol`
- `side`
- `interval`
- `limit`
- `atrPeriod`
- `riskPercent`
- `atrMultiplier`
- `leverage`
- `type`
- `timeInForce`
- `entryPrice`

동작:

- 먼저 ATR 기준 주문 수량을 계산
- 계산된 수량으로 실제 Binance 주문 실행
- 주문 결과를 사용자 주문 테이블에 저장

예시:

```text
POST /client/atr/order?symbol=BTCUSDT&side=BUY&interval=1h&limit=150&atrPeriod=14&riskPercent=1&atrMultiplier=1&leverage=10&type=MARKET
```

### 13. 서비스 내 포지션 청산

- Method: `POST`
- Path: `/client/positions/liquidate`

Query Params:

- `symbol`

동작:

- 현재 포지션 수량을 확인
- 롱이면 `SELL`, 숏이면 `BUY`
- `reduceOnly=true` 시장가 주문으로 청산

예시:

```text
POST /client/positions/liquidate?symbol=BTCUSDT
```

## 구현 파일 위치

- 컨트롤러: [src/main/java/st/indicator/stindicator/presentation/controller/ClientController.java](./src/main/java/st/indicator/stindicator/presentation/controller/ClientController.java)
- ATR 계산 서비스: [src/main/java/st/indicator/stindicator/application/service/AtrPositionSizingService.java](./src/main/java/st/indicator/stindicator/application/service/AtrPositionSizingService.java)
- 거래소 서비스: [src/main/java/st/indicator/stindicator/application/service/BinanceClient.java](./src/main/java/st/indicator/stindicator/application/service/BinanceClient.java)
- 거래소 커넥터: [src/main/java/st/indicator/stindicator/infra/connector/exchange/BinanceConnector.java](./src/main/java/st/indicator/stindicator/infra/connector/exchange/BinanceConnector.java)
- ATR 요청 DTO: [src/main/java/st/indicator/stindicator/presentation/dto/AtrOrderRequestDto.java](./src/main/java/st/indicator/stindicator/presentation/dto/AtrOrderRequestDto.java)
- ATR 응답 DTO: [src/main/java/st/indicator/stindicator/application/dto/AtrOrderPreview.java](./src/main/java/st/indicator/stindicator/application/dto/AtrOrderPreview.java)

## 주의 사항

- 현재 컨트롤러는 `@RequestBody` JSON 방식이 아니라 요청 파라미터 바인딩 기준입니다.
- `LIMIT` 주문 시 `price`, `timeInForce` 값을 함께 넣는 것을 권장합니다.
- `POST /client/atr/order` 에서 `entryPrice` 를 생략하면 계산은 현재가 기준으로 되지만 실제 주문은 `MARKET` 주문이 가장 자연스럽습니다.
- ATR 계산을 위해서는 `atrPeriod` 보다 충분히 큰 `limit` 값을 주는 것이 좋습니다.
