package com.xhy.tron.easytransaction.controller;

import com.xhy.tron.easytransaction.dto.Result;
import com.xhy.tron.easytransaction.dto.SendParamDTO;
import com.xhy.tron.easytransaction.util.TrxUtils;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/tran")
public class TranController {

    @RequestMapping(method = RequestMethod.POST, value = "/v1.0/easySend")
    public Result easySend(@RequestBody SendParamDTO sendParamDTO){
        try {
            if (StringUtils.isEmpty(sendParamDTO.getFromAddress())
                    || StringUtils.isEmpty(sendParamDTO.getToAddress())
                    || StringUtils.isEmpty(sendParamDTO.getFromPrivateKey())
                    || sendParamDTO.getValue() == null || sendParamDTO.getValue().compareTo(BigDecimal.ZERO) <= 0
                    || sendParamDTO.getGasLimit() == null || sendParamDTO.getGasLimit().compareTo(BigDecimal.ZERO) <= 0){
                throw new IllegalArgumentException("参数不合法, param:" + sendParamDTO);
            }
            //交易成功hash
            String txid = null;
            //trx交易
            if (StringUtils.isEmpty(sendParamDTO.getContract())){
                txid = TrxUtils.signTransaction(sendParamDTO.getFromAddress(), sendParamDTO.getFromPrivateKey(),
                        sendParamDTO.getToAddress(), sendParamDTO.getValue(), sendParamDTO.getGasLimit());
            }else { //trc20交易
                txid = TrxUtils.signContractAddrTransaction(sendParamDTO.getContract(), sendParamDTO.getFromAddress(), sendParamDTO.getFromPrivateKey(),
                        sendParamDTO.getToAddress(), sendParamDTO.getValue(), sendParamDTO.getGasLimit());
            }
            if (!StringUtils.isEmpty(txid)){
                return Result.builder().code(0).message("success").data(txid).build();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Result.builder().code(-1).message(e.getMessage()).build();
        }
        return Result.builder().code(-1).build();
    }
}
