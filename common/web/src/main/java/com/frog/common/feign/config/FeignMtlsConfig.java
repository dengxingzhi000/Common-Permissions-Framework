package com.frog.common.feign.config;

import feign.Client;
import feign.httpclient.ApacheHttpClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
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
 * 兼容性配置：仅当应用中不存在 Feign Client Bean 时生效，避免与 gateway 的统一配置冲突。
 */
@Slf4j
@Configuration
@ConditionalOnMissingBean(Client.class)
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
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (InputStream is = new ClassPathResource(keystorePath).getInputStream()) {
            keyStore.load(is, keystorePassword.toCharArray());
        }

        KeyStore trustStore = KeyStore.getInstance("PKCS12");
        try (InputStream is = new ClassPathResource(truststorePath).getInputStream()) {
            trustStore.load(is, truststorePassword.toCharArray());
        }

        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, keystorePassword.toCharArray());

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trustStore);

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), new SecureRandom());

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
        }
    }
}