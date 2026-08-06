# stIndicator

Binance USDⓈ-M Futures One-way Mode 기준의 ATR 트레이딩 도구다. 주문 화면은 ATR 기반 주문 미리보기와 관리형 주문 생성, 관리중 포지션, 테스트 포지션, 호가창, 기술적 분석 차트를 제공한다. 거래소 TP/SL 주문이 아니라 서비스 자체 TP/SL 및 손절선 상승 전략을 가격 WebSocket으로 평가한다.

## 주요 기능

- ATR 기반 수량, 필요 증거금, 손절가, 익절가 계산
- 서비스 관리형 LIMIT 주문과 테스트 주문
- 관리중 포지션, 미관리 포지션, 대기 주문, 포지션 기록
- 손절선 상승 전략과 손절선 변경 기록
- Binance market/public multiplex WebSocket 기반 실시간 가격, kline, ticker, depth 수신
- 주문 탭 차트: 캔들, 거래량, EMA, SMA, 볼린저 밴드, VWAP
- cursor 기반 과거/이후 차트 구간 조회
- 차트 좌우 이동, 드래그, 시간축 확대/축소
- Binance kline WebSocket 기반 최신 캔들 및 지표 실시간 갱신
- TimesFM 예측 보조 분석 서버 연동

## 실행 환경

| 항목 | 값 |
| --- | --- |
| Backend | Java 21, Spring Boot 4.0.3, Gradle |
| Frontend | React 19, TypeScript, Vite, npm |
| DB | MySQL |
| Chart API | Spring REST + `/mp` WebSocket |
| Binance WS | `wss://fstream.binance.com/market/stream`, `wss://fstream.binance.com/public/stream` |

## 환경 변수

`.env.example`을 기준으로 값을 준비한다. 실제 API key, secret, password는 저장소에 기록하지 않는다.

| 변수 | 설명 |
| --- | --- |
| `MYSQL_DB_URL` | Spring datasource JDBC URL |
| `MYSQL_DB_USER` | DB 사용자 |
| `MYSQL_DB_PASSWORD` | DB 비밀번호 |
| `JWT_SECRET` | 세션/토큰 서명용 secret |
| `BINANCE_API_KEY` | Binance Futures API key |
| `BINANCE_API_SECRET` | Binance Futures API secret |
| `MAIL_USERNAME` | 메일 인증 발송 계정 |
| `MAIL_PASSWORD` | 메일 앱 비밀번호 |
| `MAIL_HOST` | SMTP host |
| `MAIL_PORT` | SMTP port |
| `MAIL_DEBUG_DEV` | 개발 환경 메일 debug |
| `MAIL_DEBUG_PROD` | 운영 환경 메일 debug |

## 실행 방법

Backend:

```bash
./gradlew bootRun
```

Frontend:

```bash
cd frontend
npm install
npm run dev
```

Docker Compose:

```bash
docker compose up --build
```

기본 compose 포트:

| 서비스 | 호스트 포트 | 컨테이너 포트 |
| --- | ---: | ---: |
| `st-back` | 8090 | 8080 |
| `st-front` | 3000 | 80 |
| `st-forecast` | 8100 | 8090 |

## 주문 탭 차트

### 기능

- 최신 캔들 REST 조회 후 WebSocket 실시간 구독을 시작한다.
- 과거 구간은 `before`, 이후 구간은 `after` cursor로 필요한 만큼만 조회한다.
- 캔들 식별자는 `openTime`이다.
- EMA/SMA는 `type + period + time`, 볼린저와 VWAP는 `time` 기준으로 병합한다.
- 차트 위 좌우 휠/트랙패드와 좌우 드래그는 시간축 이동이다.
- 차트 위 위아래 휠은 시간축 확대/축소다.
- 위아래 드래그는 차트 동작으로 사용하지 않는다.
- 과거를 보고 있을 때 실시간 새 캔들이 들어와도 최신 위치로 강제 이동하지 않는다.
- 최신 구간을 보고 있을 때 새 캔들이 들어오면 확대 비율을 유지한 채 최신 위치를 따라간다.

### 지표 계산 위치

EMA, SMA, 볼린저 밴드, VWAP, RSI, MACD, ADX/DMI는 백엔드에서 계산한다. 프론트는 설정 입력, 요청, 패널 렌더링, 색상, 두께, 기준선, 패널 높이, 구간 병합, 실시간 point 적용만 담당한다. 색상, 선 두께, 기준선, 패널 높이는 계산 요청과 WebSocket 구독 설정에 포함하지 않는다.

### 지표 계산 정책

| 지표 | 정책 |
| --- | --- |
| EMA | 종가 기준. 첫 `period`개 종가의 SMA를 seed로 사용. `alpha = 2 / (period + 1)` |
| SMA | 종가 기준 rolling average. 초기 캔들이 부족하면 point를 만들지 않는다. |
| Bollinger Bands | 종가 기준. 중심선은 SMA. 표준편차는 모집단 표준편차. `upper/lower = middle ± stddev × deviation` |
| VWAP | `(high + low + close) / 3` typical price와 volume 기준. UTC 일일 세션 단위로 누적값을 리셋한다. volume이 0이면 point를 만들지 않는다. |
| RSI | 종가 기준 Wilder RSI. 첫 `period`개 변화량으로 초기 averageGain/averageLoss를 만들고 이후 Wilder smoothing을 적용한다. gain/loss가 모두 0이면 50, loss가 0이면 100, gain이 0이면 0이다. |
| MACD | 종가 기준. Fast EMA와 Slow EMA는 기존 EMA 정책을 사용하고 `MACD = Fast EMA - Slow EMA`다. Signal은 MACD line의 EMA이며 초기 seed는 MACD 값의 SMA다. Signal 생성 전 `signal`, `histogram`은 `null`이다. |
| ADX/DMI | True Range, +DM, -DM을 계산한 뒤 Wilder smoothing으로 +DI/-DI/DX/ADX를 산출한다. +DM과 -DM은 같은 캔들에서 동시에 양수가 될 수 없다. 초기 ADX 전에는 +DI/-DI만 존재하고 `adx`는 `null`일 수 있다. |

RSI, MACD, ADX/DMI는 표시 구간보다 앞쪽의 준비 캔들을 내부적으로 함께 조회해 계산한다. 응답에는 화면 표시 구간의 캔들과 지표 point만 포함하며, 준비 캔들은 반환하지 않는다.

### MACD 시그널 판정 정책

MACD 시그널 강조는 백엔드 판정 결과를 그대로 사용한다. 프론트는 크로스 여부를 다시 계산하지 않고, 서버가 반환한 상태와 이벤트만 렌더링한다.

| 항목 | 정책 |
| --- | --- |
| 골든크로스 | 이전 `MACD - Signal <= 0`이고 현재 `MACD - Signal > 0`일 때 후보가 된다. |
| 데드크로스 | 이전 `MACD - Signal >= 0`이고 현재 `MACD - Signal < 0`일 때 후보가 된다. |
| 동일값 구간 | `abs(MACD - Signal) <= 0.00000001`이면 동일 상태로 본다. 동일값 구간 자체에서는 이벤트를 만들지 않고, 실제 상향/하향 이탈 시점에만 후보를 만든다. |
| 첫 유효 point | 이전 유효 MACD/Signal 관계가 없으면 방향 상태만 판정하고 크로스 이벤트는 만들지 않는다. |
| Histogram 정합성 | `histogram`은 `MACD - Signal`과 허용 오차 안에서 일치해야 한다. 불일치하면 해당 point는 NEUTRAL로 처리하고 크로스 이벤트를 만들지 않는다. |
| Histogram 노이즈 | `abs(histogram) / close * 10000 < minimumHistogramBps`이면 NEUTRAL이며 크로스 후보가 아니다. 기준값과 같거나 크면 유효 후보가 될 수 있다. |
| 0선 필터 | `requireZeroLineConfirmation=true`이면 골든은 `MACD > 0`, 데드는 `MACD < 0` 조건을 후보 생성부터 확정까지 유지해야 한다. `MACD == 0`은 충족으로 보지 않는다. |
| 강도 판정 | LONG/SHORT 상태에서 Histogram 절대값이 이전보다 커지고 `strongRequiresZeroLine` 조건을 만족하면 STRONG, 아니면 WEAK다. 이 설정은 크로스 이벤트 허용 여부와 별개다. |
| 확인 봉 수 | `crossConfirmationBars`는 교차가 발생한 확정 캔들을 1로 포함한다. 값이 2이면 발생 캔들과 다음 확정 캔들까지 관계, 노이즈, 0선 조건이 유지되어야 확정 이벤트가 된다. |
| 후보 취소 | 확인 중 반대 관계로 바뀌거나 Histogram 기준 미달, 0선 조건 이탈이 발생하면 후보를 취소한다. 한 설정 조합과 시각에는 하나의 MACD 후보만 유효하다. |
| 이벤트 병합 | 이벤트 키는 `MACD + type + originTime + signalConfigVersion`이다. 같은 originTime의 provisional/confirmed 마커가 중복으로 남지 않도록 최신 판정으로 교체한다. |

## Chart REST API

### 차트 구간 조회

`GET /api/v1/chart`

최신, 과거, 이후 캔들 구간과 요청한 지표를 계산해 반환한다. `before`와 `after`는 동시에 사용할 수 없다. 둘 다 없으면 최신 구간을 조회한다.

#### Query Parameters

| 필드 | 타입 | 필수 | 기본값 | 제한 | 설명 |
| --- | --- | --- | --- | --- | --- |
| `symbol` | string | Y | - | `[A-Z0-9]{2,20}USDT` | Binance USDT 선물 심볼 |
| `interval` | string | N | `1h` | `1m`, `5m`, `15m`, `1h`, `4h`, `1d` | 캔들 주기 |
| `limit` | number | N | `200` | 최대 `500` | 화면에 반환할 캔들 수 |
| `before` | number | N | - | 0 이상 | 해당 openTime 이전 캔들 조회 cursor |
| `after` | number | N | - | 0 이상 | 해당 openTime 이후 캔들 조회 cursor |
| `endTime` | number | N | - | 0 이상 | `before`와 같은 의미의 호환 필드 |
| `emaPeriods` | string | N | - | 최대 20개, 각 1~1000 | 예: `20,60,120` |
| `smaPeriods` | string | N | - | 최대 20개, 각 1~1000 | 예: `20,50,200` |
| `bollingerPeriod` | number | N | - | 1~1000 | 값이 있으면 볼린저 밴드를 계산한다. |
| `bollingerDeviation` | number | N | `2` | 0.1~10 | 볼린저 표준편차 배수 |
| `vwap` | boolean | N | `false` | `true`, `false` | VWAP 계산 여부 |
| `rsiPeriod` | number | N | - | 1~1000 | 값이 있으면 RSI를 계산한다. 기본 UI 값은 14다. |
| `macdFastPeriod` | number | N | - | 1~1000, slow보다 작아야 함 | 값이 있으면 MACD를 계산한다. 일부 MACD 필드만 보내면 누락값은 12/26/9 기본값을 사용한다. |
| `macdSlowPeriod` | number | N | - | 1~1000 | MACD slow EMA 기간 |
| `macdSignalPeriod` | number | N | - | 1~1000 | MACD signal EMA 기간 |
| `adxDiPeriod` | number | N | - | 1~1000 | 값이 있으면 ADX/DMI를 계산한다. 일부 ADX 필드만 보내면 누락값은 14/14 기본값을 사용한다. |
| `adxSmoothingPeriod` | number | N | - | 1~1000 | ADX smoothing 기간 |

#### 최신 구간 요청

```http
GET /api/v1/chart?symbol=BTCUSDT&interval=1h&limit=200&emaPeriods=20,60&smaPeriods=50&bollingerPeriod=20&bollingerDeviation=2&vwap=true&rsiPeriod=14&macdFastPeriod=12&macdSlowPeriod=26&macdSignalPeriod=9&adxDiPeriod=14&adxSmoothingPeriod=14
```

#### 이전 구간 요청

```http
GET /api/v1/chart?symbol=BTCUSDT&interval=1h&limit=200&before=1710000000000&emaPeriods=20
```

#### 이후 구간 요청

```http
GET /api/v1/chart?symbol=BTCUSDT&interval=1h&limit=200&after=1710000000000&emaPeriods=20
```

#### 성공 응답 예시

```json
{
  "symbol": "BTCUSDT",
  "interval": "1h",
  "candles": [
    {
      "openTime": 1710000000000,
      "open": 65000.0,
      "high": 65200.0,
      "low": 64900.0,
      "close": 65150.0,
      "volume": 123.45,
      "closeTime": 1710003599999
    }
  ],
  "indicators": {
    "ema": [
      {
        "period": 20,
        "points": [
          { "time": 1710000000000, "value": 65080.12 }
        ]
      }
    ],
    "sma": [],
    "bollingerBands": {
      "period": 20,
      "deviation": 2,
      "points": [
        {
          "time": 1710000000000,
          "middle": 65000.0,
          "upper": 65600.0,
          "lower": 64400.0
        }
      ]
    },
    "vwap": {
      "points": [
        { "time": 1710000000000, "value": 65120.0 }
      ]
    },
    "rsi": {
      "period": 14,
      "points": [
        { "time": 1710000000000, "value": 58.31 }
      ]
    },
    "macd": {
      "fastPeriod": 12,
      "slowPeriod": 26,
      "signalPeriod": 9,
      "points": [
        {
          "time": 1710000000000,
          "macd": 125.1,
          "signal": 119.42,
          "histogram": 5.68
        }
      ]
    },
    "adxDmi": {
      "diPeriod": 14,
      "adxSmoothingPeriod": 14,
      "points": [
        {
          "time": 1710000000000,
          "adx": 24.12,
          "plusDi": 31.52,
          "minusDi": 18.14
        }
      ]
    }
  },
  "hasMore": true,
  "hasOlder": true,
  "hasNewer": false,
  "nextBefore": 1710000000000,
  "nextAfter": 1710003599999,
  "direction": "LATEST",
  "returnedCount": 200
}
```

#### 응답 필드

| 필드 | 설명 |
| --- | --- |
| `candles` | openTime 오름차순 캔들 목록 |
| `indicators` | 요청한 지표만 포함하는 지표 객체 |
| `hasOlder` | `before=nextBefore`로 과거 구간 조회 가능 여부 |
| `hasNewer` | `after=nextAfter`로 이후 구간 조회 가능 여부 |
| `nextBefore` | 다음 과거 조회 cursor |
| `nextAfter` | 다음 이후 조회 cursor |
| `direction` | `LATEST`, `OLDER`, `NEWER` |
| `returnedCount` | 응답에 포함된 캔들 수 |

#### 지표 응답 필드

| 필드 | 설명 |
| --- | --- |
| `indicators.rsi.period` | RSI 계산 기간 |
| `indicators.rsi.points[].value` | 0~100 범위의 RSI 값 |
| `indicators.macd.fastPeriod/slowPeriod/signalPeriod` | MACD 계산 기간 설정 |
| `indicators.macd.points[].macd` | MACD line 값 |
| `indicators.macd.points[].signal` | Signal line 값. 초기 구간에서는 `null`일 수 있다. |
| `indicators.macd.points[].histogram` | MACD - Signal 값. Signal이 없으면 `null`일 수 있다. |
| `indicators.adxDmi.diPeriod/adxSmoothingPeriod` | DMI/ADX 계산 기간 설정 |
| `indicators.adxDmi.points[].adx` | ADX 값. 초기 구간에서는 `null`일 수 있다. |
| `indicators.adxDmi.points[].plusDi` | +DI 값 |
| `indicators.adxDmi.points[].minusDi` | -DI 값 |

#### 오류 상황

| 상황 | 메시지 |
| --- | --- |
| `symbol` 누락 | `symbol은 필수입니다.` |
| 지원하지 않는 symbol 형식 | `지원하지 않는 USDT 선물 심볼 형식입니다.` |
| 지원하지 않는 interval | `지원 interval은 1m, 5m, 15m, 1h, 4h, 1d입니다.` |
| `before`와 `after` 동시 사용 | `before와 after는 동시에 사용할 수 없습니다.` |
| 잘못된 지표 기간 | `...는 1 이상 1000 이하의 정수여야 합니다.` |

## Chart WebSocket

### 연결 URL

```text
ws://{host}/mp
wss://{host}/mp
```

브라우저가 HTTPS로 동작하면 `wss://`를 사용한다. 프론트는 현재 origin을 기준으로 `wsUrl('/mp')`를 생성한다.

### 차트 구독

```json
{
  "type": "SUBSCRIBE_CHART",
  "symbols": ["BTCUSDT"],
  "interval": "1m",
  "subscriptionId": "chart-BTCUSDT-1m-1710000000000",
  "configVersion": 1710000000000,
  "emaPeriods": [20, 60],
  "smaPeriods": [50],
  "bollingerPeriod": 20,
  "bollingerDeviation": 2,
  "vwap": true,
  "rsiPeriod": 14,
  "macdFastPeriod": 12,
  "macdSlowPeriod": 26,
  "macdSignalPeriod": 9,
  "adxDiPeriod": 14,
  "adxSmoothingPeriod": 14
}
```

| 필드 | 설명 |
| --- | --- |
| `type` | `SUBSCRIBE_CHART` |
| `symbols` | 현재 구현은 첫 번째 심볼을 차트 구독 대상으로 사용한다. |
| `interval` | Binance kline interval |
| `subscriptionId` | 프론트가 생성하는 구독 식별자 |
| `configVersion` | 지표 설정 버전. 이전 버전 이벤트 폐기에 사용한다. |
| `emaPeriods` | 계산할 EMA 기간 목록 |
| `smaPeriods` | 계산할 SMA 기간 목록 |
| `bollingerPeriod` | 볼린저 밴드 기간. 없으면 계산하지 않는다. |
| `bollingerDeviation` | 볼린저 표준편차 배수 |
| `vwap` | VWAP 계산 여부 |
| `rsiPeriod` | RSI 계산 기간. 없으면 RSI를 계산하지 않는다. |
| `macdFastPeriod` | MACD fast EMA 기간. MACD 필드 중 하나라도 있으면 누락값은 12/26/9를 사용한다. |
| `macdSlowPeriod` | MACD slow EMA 기간 |
| `macdSignalPeriod` | MACD signal EMA 기간 |
| `adxDiPeriod` | ADX/DMI DI 기간. ADX/DMI 필드 중 하나라도 있으면 누락값은 14/14를 사용한다. |
| `adxSmoothingPeriod` | ADX smoothing 기간 |

### 차트 구독 해제

```json
{
  "type": "UNSUBSCRIBE_CHART",
  "symbols": ["BTCUSDT"],
  "interval": "1m",
  "subscriptionId": "chart-BTCUSDT-1m-1710000000000",
  "configVersion": 1710000000000
}
```

### 구독 확인 이벤트

```json
{
  "eventType": "CHART_SUBSCRIBED",
  "type": "CHART_SUBSCRIBED",
  "subscriptionId": "chart-BTCUSDT-1m-1710000000000",
  "symbol": "BTCUSDT",
  "interval": "1m",
  "configVersion": 1710000000000,
  "lastOpenTime": 1710000000000
}
```

### 실시간 업데이트 이벤트

```json
{
  "eventType": "CHART_REALTIME_UPDATE",
  "type": "CHART_REALTIME_UPDATE",
  "subscriptionId": "chart-BTCUSDT-1m-1710000000000",
  "symbol": "BTCUSDT",
  "interval": "1m",
  "configVersion": 1710000000000,
  "sequence": 1842,
  "candle": {
    "openTime": 1710000000000,
    "closeTime": 1710000059999,
    "open": "65000.0",
    "high": "65200.0",
    "low": "64900.0",
    "close": "65150.0",
    "volume": "123.45",
    "closed": false
  },
  "indicators": {
    "ema": [
      { "period": 20, "time": 1710000000000, "value": "65080.12" }
    ],
    "sma": [
      { "period": 50, "time": 1710000000000, "value": "64890.31" }
    ],
    "bollingerBands": {
      "period": 20,
      "deviation": "2",
      "time": 1710000000000,
      "middle": "65000.0",
      "upper": "65600.0",
      "lower": "64400.0"
    },
    "vwap": {
      "time": 1710000000000,
      "value": "65120.0"
    },
    "rsi": {
      "period": 14,
      "time": 1710000000000,
      "value": "58.31"
    },
    "macd": {
      "fastPeriod": 12,
      "slowPeriod": 26,
      "signalPeriod": 9,
      "time": 1710000000000,
      "macd": "125.10",
      "signal": "119.42",
      "histogram": "5.68"
    },
    "adxDmi": {
      "diPeriod": 14,
      "adxSmoothingPeriod": 14,
      "time": 1710000000000,
      "adx": "24.12",
      "plusDi": "31.52",
      "minusDi": "18.14"
    }
  }
}
```

요청하지 않은 지표는 `indicators`에 포함하지 않는다. 지표 값이 아직 생성되지 않는 초기 구간은 0으로 대체하지 않고 point를 보내지 않는다. MACD의 `signal/histogram`, ADX/DMI의 `adx`는 초기화 전 `null` 또는 미포함일 수 있다.

### 실시간 처리 규칙

- 같은 `openTime` candle은 교체한다.
- 더 큰 `openTime` candle은 뒤에 추가한다.
- `closed=true` 이후 같은 `openTime`의 `closed=false` 이벤트는 폐기한다.
- `sequence`가 이전 값보다 작거나 같으면 프론트에서 폐기한다.
- `subscriptionId`, `symbol`, `interval`, `configVersion`이 현재 구독과 다르면 프론트에서 폐기한다.
- 누락된 openTime 구간이 감지되면 서버가 Binance REST로 해당 범위를 한 번 보정한다.

## Binance WebSocket 구조

서버는 Binance market stream 연결을 하나의 상위 연결로 유지한다.

```text
Binance
  ↓
Single Market Multiplex WebSocket
  ↓
Internal stream router
  ↓
market data / chart realtime / position services
  ↓
Frontend /mp WebSocket
```

`symbol + interval`이 같은 여러 차트 구독은 같은 Binance kline stream을 공유한다. 마지막 프론트 구독이 사라지면 upstream 구독을 해제한다.

## 테스트 및 검증

Backend chart tests:

```bash
./gradlew test --tests st.indicator.stindicator.application.service.ChartServiceIndicatorTest
./gradlew test --tests st.indicator.stindicator.application.service.ChartRealtimeServiceTest
```

Backend compile:

```bash
./gradlew compileJava
```

Frontend build:

```bash
cd frontend
npm run build
```

프론트 프로젝트에는 별도 test runner가 없다. 브라우저 수동 검증은 주문 탭에서 다음 항목을 확인한다.

- 초기 캔들 및 거래량 표시
- EMA/SMA/볼린저/VWAP ON/OFF
- 좌우 휠, 좌우 드래그, 위아래 휠 확대/축소
- 과거/이후 구간 자동 로딩
- 실시간 candle 교체와 새 candle 추가
- 과거 탐색 중 최신 강제 이동 없음
- 심볼/interval 변경 시 이전 실시간 이벤트 미반영

## 운영 확인 항목

- HTTPS 환경에서는 WebSocket이 `wss://`로 연결되는지 확인한다.
- Nginx 또는 프록시는 WebSocket upgrade header를 전달해야 한다.
- 차트 REST 실패는 차트 영역 오류로 표시되며 주문 API를 중단하지 않는다.
- 차트 실시간 연결 실패 상태에서도 기존 캔들은 유지된다.
- Binance API key와 secret은 서버 환경 변수로만 제공한다.
