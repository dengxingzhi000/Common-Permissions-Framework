package com.frog.common.feign.config;

import feign.Client;
import feign.httpclient.ApacheHttpClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.Scheduled;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Date;

/**
 *
 *
 * @author Deng
 * createData 2025/10/24 16:48
 * @version 1.0
 */
@Slf4j
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
        try (InputStream is = new ClassPathResource(keystorePath).getInputStream()) {
            keyStore.load(is, keystorePassword.toCharArray());
        }

        // 加载信任证书
        KeyStore trustStore = KeyStore.getInstance("PKCS12");
        try (InputStream is = new ClassPathResource(truststorePath).getInputStream()) {
            trustStore.load(is, truststorePassword.toCharArray());
        }

        // 构建SSLContext
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, keystorePassword.toCharArray());

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trustStore);

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), new SecureRandom());

        // 配置SSL
        SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(
                sslContext,
                new String[]{"TLSv1.3", "TLSv1.2"},
                null,
                SSLConnectionSocketFactory.getDefaultHostnameVerifier()
        );

        CloseableHttpClient httpClient = HttpClients.custom()
                .setSSLSocketFactory(socketFactory)
                .build();

        return new ApacheHttpClient(httpClient);
    }

    /**
     * 证书自动续期检查
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void checkCertificateExpiry() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (InputStream is = new ClassPathResource(keystorePath).getInputStream()) {
            keyStore.load(is, keystorePassword.toCharArray());
        }

        String alias = keyStore.aliases().nextElement();
        X509Certificate cert = (X509Certificate) keyStore.getCertificate(alias);

        Date notAfter = cert.getNotAfter();
        long daysUntilExpiry = (notAfter.getTime() - System.currentTimeMillis()) / (1000 * 60 * 60 * 24);

        if (daysUntilExpiry < 30) {
            log.warn("证书即将过期! 剩余天数: {}", daysUntilExpiry);
            // TODO: 发送告警通知
        }
    }
}