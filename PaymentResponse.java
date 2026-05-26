// com.webs.furniturewebs.dto/PaymentResponse.java
package com.webs.furniturewebs.dto;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class PaymentResponse {
    private Integer paymentId;
    private Integer orderId;
    private BigDecimal amount;
    private String status;
    private String paymentMethod;
    private LocalDateTime transactionDate;
    private String cardLast4;
}