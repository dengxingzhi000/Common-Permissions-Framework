package com.frog.gateway.config;

import com.frog.gateway.properties.MtlsProperties;
import feign.Client;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.ConnectionPool;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.Scheduled;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Enumeration;
import java.util.concurrent.TimeUnit;

/**
 * Feign 双向 TLS (mTLS) 配置（OkHttp）
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "security.mtls", name = "enabled", havingValue = "true", matchIfMissing = true)
public class FeignMtlsConfig {
    private final MtlsProperties mtlsProperties;

    private volatile SSLContext cachedSslContext;
    private volatile X509TrustManager cachedTrustManager;

    @Bean
    public Client feignClient() throws Exception {
        okhttp3.OkHttpClient okHttpClient = new okhttp3.OkHttpClient.Builder()
                .sslSocketFactory(getSslContext().getSocketFactory(), getTrustManager())
                .connectionPool(new ConnectionPool(
                        mtlsProperties.getMaxIdleConnections(),
                        mtlsProperties.getKeepAliveMinutes(),
                        TimeUnit.MINUTES
                ))
                .connectTimeout(mtlsProperties.getConnectTimeoutMs(), TimeUnit.MILLISECONDS)
                .readTimeout(mtlsProperties.getReadTimeoutMs(), TimeUnit.MILLISECONDS)
                .writeTimeout(mtlsProperties.getWriteTimeoutMs(), TimeUnit.MILLISECONDS)
                .build();

        return new feign.okhttp.OkHttpClient(okHttpClient);
    }

    private synchronized SSLContext getSslContext() throws Exception {
        if (cachedSslContext == null) {
            buildSslArtifacts();
        }
        return cachedSslContext;
    }

    private synchronized X509TrustManager getTrustManager() throws Exception {
        if (cachedTrustManager == null) {
            buildSslArtifacts();
        }
        return cachedTrustManager;
    }

    private void buildSslArtifacts() throws Exception {
        KeyStore keyStore = loadKeyStore(mtlsProperties.getKeystorePath(), mtlsProperties.getKeystorePassword());
        KeyStore trustStore = loadKeyStore(mtlsProperties.getTruststorePath(), mtlsProperties.getTruststorePassword());

        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, mtlsProperties.getKeystorePassword().toCharArray());

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trustStore);

        X509TrustManager x509Tm = null;
        for (TrustManager tm : tmf.getTrustManagers()) {
            if (tm instanceof X509TrustManager) {
                x509Tm = (X509TrustManager) tm;
                break;
            }
        }
        if (x509Tm == null) {
            throw new IllegalStateException("No X509TrustManager found");
        }

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(kmf.getKeyManagers(), new TrustManager[]{x509Tm}, new SecureRandom());

        cachedSslContext = sslContext;
        cachedTrustManager = x509Tm;
        log.info("mTLS SSLContext (OkHttp) initialized.");
    }

    private KeyStore loadKeyStore(Resource resource, String password) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (InputStream is = resource.getInputStream()) {
            keyStore.load(is, password.toCharArray());
        }
        return keyStore;
    }

    @Scheduled(cron = "0 0 2 * * ?")
    public void checkCertificateExpiry() {
        try {
            KeyStore keyStore = loadKeyStore(mtlsProperties.getKeystorePath(), mtlsProperties.getKeystorePassword());
            Enumeration<String> aliases = keyStore.aliases();
            if (!aliases.hasMoreElements()) {
                log.warn("KeyStore 中未找到任何证书条目");
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
                log.warn("客户端证书即将过期(剩余 {} 天, 到期日期: {})", daysUntilExpiry, notAfter);
                // TODO: 发送通知（Email / Webhook / 报警平台）
            } else {
                log.debug("证书 [{}] 状态正常，有效期至 {}", alias, notAfter);
            }

        } catch (Exception e) {
            log.error("检查证书到期失败: {}", e.getMessage(), e);
        }
    }
}