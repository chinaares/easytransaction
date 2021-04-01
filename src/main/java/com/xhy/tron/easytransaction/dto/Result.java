package com.xhy.tron.easytransaction.dto;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class Result<T> {
    private int code;
    private String message;
    private T data;
}
