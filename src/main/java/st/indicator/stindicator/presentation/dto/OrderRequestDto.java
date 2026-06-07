package st.indicator.stindicator.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import st.indicator.stindicator.application.dto.OrderCommand;

@Schema(description = "일반 주문 요청 DTO")
public class OrderRequestDto {
    @Schema(description = "주문을 넣을 거래 심볼", example = "BTCUSDT")
    String symbol;
    @Schema(description = "주문 방향", example = "BUY")
    String side;
    @Schema(description = "주문 유형", example = "LIMIT")
    String type;
    @Schema(description = "LIMIT 주문의 유효 시간 정책", example = "GTC")
    String timeInForce;
    @Schema(description = "실제로 거래소에 전송할 주문 수량", example = "0.01")
    String quantity;
    @Schema(description = "LIMIT 주문에서 사용할 지정 가격", example = "60000")
    String price;

    public OrderRequestDto(String symbol, String side, String type, String timeInForce, String quantity, String price) {
        this.symbol = symbol;
        this.side = side;
        this.type = type;
        this.timeInForce = timeInForce;
        this.quantity = quantity;
        this.price = price;
    }

    public OrderRequestDto() {
    }

    public String getSymbol() {
        return symbol;
    }

    public String getSide() {
        return side;
    }

    public String getType() {
        return type;
    }

    public String getTimeInForce() {
        return timeInForce;
    }

    public String getQuantity() {
        return quantity;
    }

    public String getPrice() {
        return price;
    }

    public OrderCommand toCommand() {
        return new OrderCommand(symbol, side, type, timeInForce, quantity, price);
    }
}
