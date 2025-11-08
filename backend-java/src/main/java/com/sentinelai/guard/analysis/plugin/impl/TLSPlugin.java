package com.sentinelai.guard.analysis.plugin.impl;

import com.sentinelai.guard.analysis.plugin.AnalysisPlugin;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.net.ssl.*;
import java.io.IOException;
import java.net.URL;
import java.security.cert.Certificate;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Slf4j
@Component
public class TLSPlugin implements AnalysisPlugin {

    @Value("${sentinel.plugins.tls.enabled:true}")
    private boolean enabled;
    
    @Value("${sentinel.plugins.tls.timeout-ms:5000}")
    private int timeoutMs;
    
    @Value("${sentinel.plugins.tls.expiration-threshold-days:30}")
    private int expirationThresholdDays;
    
    @Override
    public String getId() {
        return "tls-analyzer";
    }

    @Override
    public String getName() {
        return "TLS Certificate Analyzer";
    }

    @Override
    public String getDescription() {
        return "Analyzes SSL/TLS certificate validity and security";
    }

    @Override
    public double getWeight() {
        return 0.2; // 20% weight in final score
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public PluginResult analyze(String url) {
        if (!enabled) {
            return new PluginResult(0.0, "TLS check skipped (plugin disabled)");
        }

        // Skip if not HTTPS
        if (!url.toLowerCase().startsWith("https://")) {
            return new PluginResult(0.3, "Not using HTTPS");
        }

        try {
            URL parsedUrl = new URL(url);
            String hostname = parsedUrl.getHost();
            int port = parsedUrl.getPort() == -1 ? 443 : parsedUrl.getPort();

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{new X509TrustManager() {
                public void checkClientTrusted(X509Certificate[] chain, String authType) {}
                public void checkServerTrusted(X509Certificate[] chain, String authType) {}
                public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
            }}, null);

            // FIX: Declare factory outside the try-with-resources block
            SSLSocketFactory factory = sslContext.getSocketFactory();
            
            // The SSLSocket *is* AutoCloseable and is correctly used here
            try (SSLSocket socket = (SSLSocket) factory.createSocket(hostname, port)) {
                
                socket.setSoTimeout(timeoutMs);
                socket.startHandshake();
                
                Certificate[] certs = socket.getSession().getPeerCertificates();
                if (certs == null || certs.length == 0) {
                    return new PluginResult(0.8, "No SSL/TLS certificate found");
                }
                
                X509Certificate cert = (X509Certificate) certs[0];
                
                try {
                    cert.checkValidity();
                    
                    Date now = new Date();
                    Date notAfter = cert.getNotAfter();
                    long daysUntilExpiry = Duration.between(
                        now.toInstant(), 
                        notAfter.toInstant()
                    ).toDays();
                    
                    if (daysUntilExpiry < 0) {
                        return new PluginResult(1.0, "Certificate has expired");
                    } else if (daysUntilExpiry < expirationThresholdDays) {
                        double risk = 0.3 + (0.7 * (1 - (daysUntilExpiry / (double)expirationThresholdDays)));
                        return new PluginResult(risk, 
                            String.format("Certificate expires in %d days", daysUntilExpiry));
                    }
                    
                    String sigAlg = cert.getSigAlgName().toUpperCase();
                    if (sigAlg.contains("SHA1") || sigAlg.contains("MD5") || sigAlg.contains("RSA < 2048")) {
                        return new PluginResult(0.6, "Weak certificate signature algorithm: " + sigAlg);
                    }
                    
                    return new PluginResult(0.0, "Valid certificate with strong configuration");
                    
                } catch (CertificateExpiredException e) {
                    return new PluginResult(1.0, "Certificate has expired");
                } catch (CertificateNotYetValidException e) {
                    return new PluginResult(0.8, "Certificate is not yet valid");
                }
            }
            
        } catch (SSLHandshakeException e) {
            log.warn("SSL handshake failed for {}: {}", url, e.getMessage());
            return new PluginResult(0.9, "SSL handshake failed: " + e.getMessage());
        } catch (Exception e) {
            log.warn("Error checking TLS for {}: {}", url, e.getMessage());
            return new PluginResult(0.5, "TLS check failed: " + e.getMessage());
        }
    }
}