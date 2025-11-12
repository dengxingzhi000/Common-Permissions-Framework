package com.frog.gateway.config;

import com.frog.gateway.properties.MtlsProperties;
import feign.Client;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.Scheduled;
import feign.httpclient.ApacheHttpClient;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Enumeration;

/**
 * Feign 双向 TLS (mTLS) 安全配置
 *
 * @author Deng
 * @since 2025/11/10
 */
@Slf4j
@Configuration
public class FeignMtlsConfig {
    private final MtlsProperties mtlsProperties;

    /** SSLContext 缓存（防止每次重建） */
    private volatile SSLContext cachedSslContext;

    @Bean
    public Client feignClient() throws Exception {
        CloseableHttpClient httpClient = HttpClients.custom()
                .setSSLSocketFactory(createSslSocketFactory())
                .build();
        return new ApacheHttpClient(httpClient);
    }

    private FeignMtlsConfig(MtlsProperties mtlsProperties) {
        this.mtlsProperties = mtlsProperties;
    }

    /**
     * 创建 SSL Socket 工厂（支持缓存）
     */
    private synchronized SSLConnectionSocketFactory createSslSocketFactory() throws Exception {
        if (cachedSslContext == null) {
            cachedSslContext = buildSslContext();
        }

        return new SSLConnectionSocketFactory(
                cachedSslContext,
                new String[]{"TLSv1.3", "TLSv1.2"},
                null,
                SSLConnectionSocketFactory.getDefaultHostnameVerifier()
        );
    }

    /**
     * 构建 SSL 上下文
     */
    private SSLContext buildSslContext() throws Exception {
        KeyStore keyStore = loadKeyStore(mtlsProperties.getKeystorePath(), mtlsProperties.getKeystorePassword());
        KeyStore trustStore = loadKeyStore(mtlsProperties.getTruststorePath(), mtlsProperties.getTruststorePassword());

        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, mtlsProperties.getKeystorePassword().toCharArray());

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trustStore);

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), new SecureRandom());

        log.info("mTLS SSLContext successfully initialized.");
        return sslContext;
    }

    /**
     * 通用 keystore 加载方法
     */
    private KeyStore loadKeyStore(Resource resource, String password) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (InputStream is = resource.getInputStream()) {
            keyStore.load(is, password.toCharArray());
        }
        return keyStore;
    }

    /**
     * 定时检测证书有效期（每天凌晨2点）
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void checkCertificateExpiry() {
        try {
            KeyStore keyStore = loadKeyStore(mtlsProperties.getKeystorePath(), mtlsProperties.getKeystorePassword());
            Enumeration<String> aliases = keyStore.aliases();
            if (!aliases.hasMoreElements()) {
                log.warn("KeyStore 中未找到任何证书条目。");
                return;
            }

            String alias = aliases.nextElement();
            X509Certificate cert = (X509Certificate) keyStore.getCertificate(alias);

            if (cert == null) {
                log.warn("无法获取证书: {}", alias);
                return;
            }

            Date notAfter = cert.getNotAfter();
            long daysUntilExpiry = Duration.between(Instant.now(), notAfter.toInstant()).toDays();

            if (daysUntilExpiry < 0) {
                log.error("客户端证书已过期: {} (过期日期: {})", alias, notAfter);
            } else if (daysUntilExpiry < 30) {
                log.warn("客户端证书即将过期 (剩余 {} 天, 到期日期: {})", daysUntilExpiry, notAfter);
                // TODO: 发送通知（Email / Webhook / 报警平台）
            } else {
                log.debug("证书 [{}] 状态正常，有效期至 {}", alias, notAfter);
            }

        } catch (Exception e) {
            log.error("检查证书到期失败: {}", e.getMessage(), e);
        }
    }
}
