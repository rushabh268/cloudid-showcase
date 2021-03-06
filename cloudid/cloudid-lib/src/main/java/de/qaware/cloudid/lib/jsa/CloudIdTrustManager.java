package de.qaware.cloudid.lib.jsa;

import de.qaware.cloudid.lib.*;
import de.qaware.cloudid.util.Certificates;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Validate;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509ExtendedTrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.Socket;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Objects;
import java.util.Optional;

import static de.qaware.cloudid.util.Certificates.getSpiffeId;
import static java.text.MessageFormat.format;
import static java.util.Arrays.stream;

/**
 * X.509 trust manager backed by CloudId.
 */
@Slf4j
@RequiredArgsConstructor
public class CloudIdTrustManager extends X509ExtendedTrustManager {

    private final IdManager idManager;
    private final ACLManager aclManager;

    private final X509TrustManager delegate = getDefaultTrustManager();

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        LOGGER.trace("getAcceptedIssuers()");

        return idManager.getWorkloadId().getTrustedCAs().toArray(new X509Certificate[0]);
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        LOGGER.debug("Validating client {}, {}", chain, authType);

        Validate.notEmpty(chain);

        Optional<String> clientIdOpt = getSpiffeId(chain[0]);

        if (clientIdOpt.isPresent()) {
            String clientId = clientIdOpt.get();

            WorkloadId workloadId = idManager.getWorkloadId();
            ACL acl = aclManager.get();

            Certificates.validate(chain, workloadId.getTrustedCAs());

            if (!acl.isAllowed(clientId, workloadId.getSpiffeId()) && shouldCheckAcl()) {
                String message = format("{0} tried to access this service but is not allowed to", clientId);
                LOGGER.error(message);
                throw new CertificateException(message);
            }
        } else {
            LOGGER.debug("Client certificate is not a SPIFFE certificate. Delegating to {}", delegate.getClass().getName());
            delegate.checkClientTrusted(chain, authType);
        }
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket) throws CertificateException {
        checkClientTrusted(chain, authType);
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine sslEngine) throws CertificateException {
        checkClientTrusted(chain, authType);
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        LOGGER.debug("Validating server {}, {}", chain, authType);

        Validate.notEmpty(chain);

        Optional<String> clientIdOpt = getSpiffeId(chain[0]);
        if (clientIdOpt.isPresent()) {
            Certificates.validate(chain, idManager.getWorkloadId().getTrustedCAs());
        } else {
            LOGGER.debug("Server certificate is not a SPIFFE certificate. Delegating to {}", delegate.getClass().getName());
            delegate.checkServerTrusted(chain, authType);
        }
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket) throws CertificateException {
        checkServerTrusted(chain, authType);
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine sslEngine) throws CertificateException {
        checkServerTrusted(chain, authType);
    }

    private boolean shouldCheckAcl() {
        return !Config.ACL_DISABLED.get();
    }

    private static X509TrustManager getDefaultTrustManager() {
        return stream(Security.getProviders("TrustManagerFactory.PKIX"))
                .map(Provider::getName)
                .filter(name -> !Objects.equals(name, CloudId.PROVIDER_NAME))
                .map(CloudIdTrustManager::getX509TrustManager)
                .findFirst()
                .orElseThrow(IllegalStateException::new);
    }

    private static X509TrustManager getX509TrustManager(String name) {
        try {
            TrustManagerFactory factory = TrustManagerFactory.getInstance("PKIX", name);
            factory.init((KeyStore) null);
            return stream(factory.getTrustManagers())
                    .filter(tm -> tm instanceof X509TrustManager)
                    .map(tm -> (X509TrustManager) tm)
                    .findFirst()
                    .orElseThrow(IllegalStateException::new);
        } catch (NoSuchAlgorithmException | NoSuchProviderException | KeyStoreException e) {
            throw new IllegalStateException(e);
        }
    }

}
