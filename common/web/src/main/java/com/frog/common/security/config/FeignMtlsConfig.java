package com.frog.common.security.config;

import feign.Client;
import feign.httpclient.ApacheHttpClient;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.net.ssl.SSLContext;
import java.io.File;
import java.security.KeyStore;

/**
 *
 *
 * @author Deng
 * createData 2025/10/24 16:48
 * @version 1.0
 */
@Configuration
public class FeignMtlsConfig {

    @Value("${security.mtls.keystore-path}")
    private String keystorePath;

    @Value("${security.mtls.keystore-password}")
    private String keystorePassword;

    @Value("${security.mtls.truststore-path}")
    private String truststorePath;

    @Value("${security.mtls.truststore-password}")
    private String truststorePassword;

    @Bean
    public Client feignClient() throws Exception {
        // 加载客户端证书
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(new File(keystorePath).toURI().toURL().openStream(),
                keystorePassword.toCharArray());

        // 加载信任证书
        KeyStore trustStore = KeyStore.getInstance("PKCS12");
        trustStore.load(new File(truststorePath).toURI().toURL().openStream(),
                truststorePassword.toCharArray());

        // 构建SSLContext
        SSLContext sslContext = SSLContextBuilder.create()
                .loadKeyMaterial(keyStore, keystorePassword.toCharArray())
                .loadTrustMaterial(trustStore, null)
                .build();

        // 配置SSL
        SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(
                sslContext,
                new String[]{"TLSv1.2", "TLSv1.3"},
                null,
                NoopHostnameVerifier.INSTANCE
        );

        CloseableHttpClient httpClient = HttpClients.custom()
                .setSSLSocketFactory(socketFactory)
                .build();

        return new ApacheHttpClient(httpClient);
    }
}
