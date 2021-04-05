package com.xhy.tron.easytransaction.dto;

import lombok.Data;
import lombok.ToString;

import java.math.BigDecimal;

@ToString
@Data
public class SendParamDTO {
    private String fromAddress;
    private String fromPrivateKey;
    private String toAddress;
    private BigDecimal value;
    private BigDecimal gasLimit;

    private String contract;
}
