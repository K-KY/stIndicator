package st.indicator.stindicator.presentation.ws.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import st.indicator.stindicator.domain.entity.Order;

import java.math.BigDecimal;

@Schema(description = "주문 실행 결과 응답 DTO")
public class OrderExecutionResponseDto {
    @Schema(description = "주문 ID")
    private String orderId;
    @Schema(description = "심볼")
    private String symbol;
    @Schema(description = "주문 방향")
    private String side;
    @Schema(description = "주문 유형")
    private String type;
    @Schema(description = "주문 상태")
    private String status;
    @Schema(description = "주문 가격")
    private BigDecimal price;
    @Schema(description = "원주문 수량")
    private BigDecimal origQty;
    @Schema(description = "실제 체결 수량")
    private BigDecimal executedQty;

    public static OrderExecutionResponseDto from(Order order) {
        OrderExecutionResponseDto dto = new OrderExecutionResponseDto();
        dto.orderId = order.getOrderId();
        dto.symbol = order.getSymbol();
        dto.side = order.getSide();
        dto.type = order.getType();
        dto.status = order.getStatus();
        dto.price = order.getPrice();
        dto.origQty = order.getOrigQty();
        dto.executedQty = order.getExecutedQty();
        return dto;
    }

    public String getOrderId() {
        return orderId;
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

    public String getStatus() {
        return status;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public BigDecimal getOrigQty() {
        return origQty;
    }

    public BigDecimal getExecutedQty() {
        return executedQty;
    }
}
