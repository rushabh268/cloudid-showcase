package de.qaware.cloudid.lib.util;

import de.qaware.cloudid.lib.jsa.SPIRETrustManager;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.*;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

import static java.util.Collections.singleton;

/**
 * Utilities for certificates.
 */
@Slf4j
@UtilityClass
public class Certificates {

    private static final Pattern SPIFFE_ID_PATTERN = Pattern.compile("spiffe://.+");

    /**
     * Get the notAfter instant of a X.509 certificate.
     *
     * @param certificate certificate
     * @return notAfter instant or {@code Instant#MAX} if the field is unset
     */
    public static Instant getNotAfter(X509Certificate certificate) {
        Date date = certificate.getNotAfter();
        return date != null ? date.toInstant() : Instant.MAX;
    }

    /**
     * Get the notBefore instant of a X.509 certificate.
     *
     * @param certificate certificate
     * @return notBefore instant or {@code Instant#MIN} if the field is unset
     */
    public static Instant getNotBefore(X509Certificate certificate) {
        Date date = certificate.getNotBefore();
        return date != null ? date.toInstant() : Instant.MIN;
    }

    /**
     * Get a X.509 certificate factory
     *
     * @return {@code CertificateFactory.getInstance("X.509")}
     */
    public static CertificateFactory getX509CertFactory() {
        try {
            return CertificateFactory.getInstance("X.509");
        } catch (CertificateException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Get a PKIX certificate path validator.
     *
     * @return {@code CertPathValidator.getInstance("PKIX")}
     */
    public static CertPathValidator getCertPathValidator() {
        try {
            return CertPathValidator.getInstance("PKIX");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Get PKIX parameters.
     * <p>
     * Certificate revocation will be disabled.
     *
     * @param certificate trust anchor
     * @return PKIX parameters
     */
    public static PKIXParameters getPkixParameters(X509Certificate certificate) {
        try {
            PKIXParameters pkixParameters = new PKIXParameters(singleton(new TrustAnchor(certificate, null)));
            pkixParameters.setRevocationEnabled(false);
            return pkixParameters;
        } catch (InvalidAlgorithmParameterException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Tries to find the SPIFFE ID in the given Subject Alternative Name collection using the given pattern.
     *
     * @param alternativeNames the collection which contains the SPIFFE Id somewhere in its Lists, either
     *                         subject alternative names (SAN) or issuer alternative names
     * @return the SPIFFE ID or null if not found
     */
    @Nullable
    public static String getSpiffeId(@Nullable Collection<List<?>> alternativeNames) {
        if (alternativeNames == null) {
            // TODO Review if this check is a good idea
            LOGGER.info("alternativeNames in getSpiffeId was null, assuming that there is no SAN extension and therefore no SPIFFE ID");
            return null;
        }
        for (List<?> list : alternativeNames) {
            for (Object object : list) {
                if (object instanceof String) {
                    String spiffeId = (String) object;
                    if (SPIFFE_ID_PATTERN.matcher(spiffeId).matches()) {
                        return spiffeId;
                    }
                }
            }
        }

        return null;
    }
}
