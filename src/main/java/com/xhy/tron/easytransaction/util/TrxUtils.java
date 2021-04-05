package com.xhy.tron.easytransaction.util;


import com.alibaba.fastjson.JSONObject;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.spongycastle.util.encoders.Hex;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.tron.common.crypto.ECKey;
import org.tron.common.crypto.Sha256Sm3Hash;
import org.tron.common.utils.Base58;
import org.tron.common.utils.ByteArray;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.contract.BalanceContract.TransferContract;
import org.tron.protos.contract.SmartContractOuterClass;

import javax.servlet.http.HttpUtils;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;


@Slf4j
public class TrxUtils {



    protected static final int DEFAULT_UNIT = 6;

    private static final String methodId = "0xa9059cbb";

    protected static String url = "https://api.trongrid.io";
    protected static byte addressPreFixByte = 65; //默认主网

    /**
     * tron api 访问的key
     */
    private static String tronApiKey = "ebf83559-5b1a-47f1-a7b8-2313aed777d9";




    /**
     * trx离线签名交易 - 转trx
     *
     * @param fromAddress 要发送的地址
     * @param privateKey  发送地址私钥
     * @param toAddress   接收地址
     * @param amount      发送数量
     * @param gas         手续费
     * @return
     * @throws Exception
     */
    public static String signTransaction(String fromAddress, String privateKey, String toAddress, BigDecimal amount,
                                         BigDecimal gas) throws Exception {
        byte[] privateBytes = ByteArray.fromHexString(privateKey);
        byte[] from = decodeFromBase58Check(fromAddress);
        byte[] to = decodeFromBase58Check(toAddress);
        long snumber = amount.multiply(BigDecimal.TEN.pow(DEFAULT_UNIT)).longValue();
        Transaction transaction = createTransaction(from, to, snumber, gas);
        byte[] transactionBytes = transaction.toByteArray();

        ECKey ecKey = ECKey.fromPrivate(privateBytes);
        Transaction transaction1 = null;
        try {
            transaction1 = Transaction.parseFrom(transactionBytes);
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }
        byte[] rawdata = transaction1.getRawData().toByteArray();
        byte[] hashrawdata = Sha256Sm3Hash.hash(rawdata);

        // String hash = ByteArray.toHexString(hashrawdata);
        byte[] sign = ecKey.sign(hashrawdata).toByteArray();
        // 签名后的交易数据
        String signedData = ByteArray
                .toHexString(transaction1.toBuilder().addSignature(ByteString.copyFrom(sign)).build().toByteArray());
        JSONObject requestBody = new JSONObject();
        requestBody.put("transaction", signedData);
        Map<String, String> headers = getApiKeyHeader();
        HttpResponse<String> response = HttpclientUtil.send(HttpclientUtil.HttpMethod.POST, url + "/wallet/broadcasthex", headers, requestBody.toJSONString());
        if (response.statusCode() != HttpStatus.OK.value()){
            throw new RuntimeException("广播请求异常");
        }
        String s = response.body();
        JSONObject entries = JSONObject.parseObject(s);
        Boolean result = entries.getBoolean("result");
        String code = entries.getString("code");
        String hash = null;
        if (result && code.equals("SUCCESS")) {
            hash = entries.getString("txid");
        } else {
            log.error("trx转账返回失败, entries={}", entries);
        }
        return hash;
    }

    /**
     * 创建合约交易
     * @param contractAddr
     * @param fromAddress
     * @param privateKey
     * @param toAddress
     * @param amount
     * @param gas
     * @return
     * @throws Exception
     */
    public static String signContractAddrTransaction(String contractAddr, String fromAddress, String privateKey, String toAddress, BigDecimal amount,
                                         BigDecimal gas) throws Exception {
        byte[] privateBytes = ByteArray.fromHexString(privateKey);
        byte[] from = decodeFromBase58Check(fromAddress);
        byte[] contract = decodeFromBase58Check(contractAddr);
        BigDecimal compareTo = amount.multiply(BigDecimal.TEN.pow(DEFAULT_UNIT));
        String snumber = Long.toHexString(compareTo.longValue());
        String data = methodId + fill_zero(base58checkToHexString(toAddress)) + TrxUtils.fill_zero(snumber);
        Protocol.Transaction transaction = createContractTransaction(from, contract, data, gas, DEFAULT_UNIT);
        byte[] transactionBytes = transaction.toByteArray();

        ECKey ecKey = ECKey.fromPrivate(privateBytes);
        Protocol.Transaction transaction1 = null;
        try {
            transaction1 = Protocol.Transaction.parseFrom(transactionBytes);
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }
        byte[] rawdata = transaction1.getRawData().toByteArray();
        byte[] hashrawdata = Sha256Sm3Hash.hash(rawdata);

        // String hash = ByteArray.toHexString(hashrawdata);
        byte[] sign = ecKey.sign(hashrawdata).toByteArray();
        // 签名后的交易数据
        String signedData = ByteArray
                .toHexString(transaction1.toBuilder().addSignature(ByteString.copyFrom(sign)).build().toByteArray());
        JSONObject requestBody = new JSONObject();
        requestBody.put("transaction", signedData);
        Map<String, String> headers = getApiKeyHeader();
        headers.put("content-type", "application/json");
        HttpResponse<String> response = HttpclientUtil.send(HttpclientUtil.HttpMethod.POST, url + "/wallet/broadcasthex", headers, requestBody.toJSONString());
        if (response.statusCode() != HttpStatus.OK.value()){
            throw new RuntimeException("广播TRC20交易请求异常");
        }
        String s = response.body();
        JSONObject entries = JSONObject.parseObject(s);
        Boolean result = entries.getBoolean("result");
        String code = entries.getString("code");
        String hash = null;
        if (!result || !code.equals("SUCCESS")) {
            throw new RuntimeException("Send transaction error, resultCode : " + code);
        }
        return entries.getString("txid");
    }

    private static Transaction createTransaction(byte[] from, byte[] to, long amount, BigDecimal gas) throws Exception {
        Transaction.Builder transactionBuilder = Transaction.newBuilder();
        JSONObject newestBlock = blockStr();

        Transaction.Contract.Builder contractBuilder = Transaction.Contract.newBuilder();
        TransferContract.Builder transferContractBuilder = TransferContract.newBuilder();
        transferContractBuilder.setAmount(amount);
        ByteString bsTo = ByteString.copyFrom(to);
        ByteString bsOwner = ByteString.copyFrom(from);
        transferContractBuilder.setToAddress(bsTo);
        transferContractBuilder.setOwnerAddress(bsOwner);
        try {
            Any any = Any.pack(transferContractBuilder.build());
            contractBuilder.setParameter(any);
        } catch (Exception e) {
            return null;
        }
        contractBuilder.setType(Transaction.Contract.ContractType.TransferContract);
        transactionBuilder.getRawDataBuilder().addContract(contractBuilder).setTimestamp(System.currentTimeMillis())
                .setExpiration(newestBlock.getLong("timestamp") + 10 * 60 * 60 * 1000)
                .setFeeLimit(gas.multiply(BigDecimal.TEN.pow(DEFAULT_UNIT)).longValue());
        Transaction transaction = transactionBuilder.build();
        Transaction refTransaction = setReference(transaction, newestBlock);
        return refTransaction;
    }

    /**
     * 创建合约交易对象
     * @param from
     * @param to
     * @param data
     * @param gas
     * @param precision
     * @return
     */
    public static Transaction createContractTransaction(byte[] from, byte[] to, String data, BigDecimal gas, int precision) throws IOException, InterruptedException {
        Transaction.Builder transactionBuilder = Protocol.Transaction.newBuilder();
        Transaction.Contract.Builder contractBuilder = Protocol.Transaction.Contract.newBuilder();
        SmartContractOuterClass.TriggerSmartContract.Builder triggerSmartContract = SmartContractOuterClass.TriggerSmartContract.newBuilder();
        ByteString bsTo = ByteString.copyFrom(to);
        ByteString bsOwner = ByteString.copyFrom(from);
        triggerSmartContract.setData(ByteString.copyFrom(ByteArray.fromHexString(data)));
        triggerSmartContract.setOwnerAddress(bsOwner);
        triggerSmartContract.setContractAddress(bsTo);
        try {
            Any any = Any.pack(triggerSmartContract.build());
            contractBuilder.setParameter(any);
        } catch (Exception e) {
            return null;
        }
        contractBuilder.setType(Protocol.Transaction.Contract.ContractType.TriggerSmartContract);
        JSONObject newestBlock = blockStr();
        transactionBuilder.getRawDataBuilder().addContract(contractBuilder).setTimestamp(System.currentTimeMillis())
                .setExpiration(newestBlock.getLong("timestamp") + 10 * 60 * 60 * 1000)
                .setFeeLimit(gas.multiply(BigDecimal.TEN.pow(DEFAULT_UNIT)).longValue());
        Protocol.Transaction transaction = transactionBuilder.build();
        Protocol.Transaction refTransaction = TrxUtils.setReference(transaction, newestBlock);
        return refTransaction;

    }




    protected static Transaction setReference(Transaction transaction, JSONObject blockStr) {
        Long blockHeight = blockStr.getLong("number");
        byte[] blockHash = Hex.decode(blockStr.getString("hash"));
        byte[] refBlockNum = ByteArray.fromLong(blockHeight);
        Transaction.raw rawData = transaction.getRawData().toBuilder()
                .setRefBlockHash(ByteString.copyFrom(ByteArray.subArray(blockHash, 8, 16)))
                .setRefBlockBytes(ByteString.copyFrom(ByteArray.subArray(refBlockNum, 6, 8)))
                .build();
        return transaction.toBuilder().setRawData(rawData).build();
    }


    public static JSONObject blockStr() throws IOException, InterruptedException {
        HttpResponse<String> response = HttpclientUtil.send(HttpclientUtil.HttpMethod.GET,
                "https://apilist.tronscan.org" + "/api/block/latest", getApiKeyHeader(), "");
        String blockStr = response.body();
        JSONObject blockInfo = JSONObject.parseObject(blockStr);
        return blockInfo;
    }


    public static boolean addressValid(byte[] address) {
        if (address == null || address.length == 0) {
            System.out.println("Warning: Address is empty !!");
            return false;
        } else if (address.length != 21) {
            System.out.println("Warning: Address length need 21 but " + address.length + " !!");
            return false;
        } else {
            byte preFixbyte = address[0];
            if (preFixbyte != addressPreFixByte) {
                System.out.println("Warning: Address need prefix with " + addressPreFixByte + " but " + preFixbyte + " !!");
                return false;
            } else {
                return true;
            }
        }
    }

    public static byte[] decodeFromBase58Check(String addr) {
        if (StringUtils.isEmpty(addr)) {
            System.out.println("Warning: Address is empty !!");
            return null;
        } else {
            byte[] address = null;
            byte[] decodeCheck = Base58.decode(addr);
            if (decodeCheck.length <= 4) {
                return null;
            } else {
                byte[] decodeData = new byte[decodeCheck.length - 4];
                System.arraycopy(decodeCheck, 0, decodeData, 0, decodeData.length);
                byte[] hash0 = Sha256Sm3Hash.hash(decodeData);
                byte[] hash1 = Sha256Sm3Hash.hash(hash0);
                address = hash1[0] == decodeCheck[decodeData.length] && hash1[1] == decodeCheck[decodeData.length + 1] && hash1[2] == decodeCheck[decodeData.length + 2] && hash1[3] == decodeCheck[decodeData.length + 3] ? decodeData : null;
            }
            return !addressValid(address) ? null : address;
        }
    }

    public static String fill_zero(String input) {
        int strLen = input.length();
        StringBuffer sb = null;
        while (strLen < 64) {
            sb = new StringBuffer();
            sb.append("0").append(input);// 左补0
            input = sb.toString();
            strLen = input.length();
        }
        return input;
    }

    private static Map<String,String> getApiKeyHeader(){
        var header = new HashMap();
        header.put("TRON-PRO-API-KEY", tronApiKey);
        return header;
    }

    public static String base58checkToHexString(String addr) {
        byte[] bytes = decodeFromBase58Check(addr);
        return bytes == null ? "" : Hex.toHexString(bytes);
    }


}