package com.xhy.tron.easytransaction.controller;

import com.xhy.tron.easytransaction.dto.Result;
import com.xhy.tron.easytransaction.dto.SendParamDTO;
import com.xhy.tron.easytransaction.util.TrxUtils;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
@RequestMapping("/api/tran")
public class TranController {

    @RequestMapping(method = RequestMethod.POST, value = "/v1.0/easySend")
    public Result easySend(@RequestBody SendParamDTO sendParamDTO){
        try {
            String txid = TrxUtils.signTransaction(sendParamDTO.getFromAddress(), sendParamDTO.getFromPrivateKey(),
                    sendParamDTO.getToAddress(), sendParamDTO.getValue(), sendParamDTO.getGasLimit());
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
