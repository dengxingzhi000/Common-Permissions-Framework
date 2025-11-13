package com.frog.gateway.client;

import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.digest.HmacAlgorithm;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 *
 *
 * @author Deng
 * createData 2025/11/11 14:35
 * @version 1.0
 */
public class SignatureClient {

    public static Map<String, String> generateHeaders(String appId, String secretKey,
                                                      String url, Map<String, String> params,
                                                      String body) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String nonce = UUID.randomUUID().toString().replace("-", "");

        String sortedParams = params.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("&"));

        String bodyHash = DigestUtils.sha256Hex(StringUtils.defaultString(body));
        String signContent = timestamp + nonce + appId + url + sortedParams + bodyHash;

        String signature = SecureUtil.hmac(HmacAlgorithm.HmacSHA256, secretKey)
                .digestHex(signContent);

        return Map.of(
                "X-Timestamp", timestamp,
                "X-Nonce", nonce,
                "X-Signature", signature,
                "X-App-Id", appId,
                "X-Sign-Version", "HMAC-SHA256-V2"
        );
    }
}