package com.xhy.tron.easytransaction.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SendParamDTO {
    private String fromAddress;
    private String fromPrivateKey;
    private String toAddress;
    private BigDecimal value;
    private BigDecimal gasLimit;
}
