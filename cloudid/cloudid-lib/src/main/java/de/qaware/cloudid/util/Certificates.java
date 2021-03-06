package de.qaware.cloudid.util;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import javax.security.auth.x500.X500Principal;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.*;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.StringUtils.startsWith;

/**
 * Utilities for certificates.
 */
@Slf4j
@UtilityClass
public class Certificates {

    /**
     * Id of an URI subject alternative name.
     */
    private static final int SAN_URI_OID = 6;
    /**
     * Index of the object Id field.
     */
    private static final int SAN_OID_I = 0;
    /**
     * Index of the value field.
     */
    private static final int SAN_VALUE_I = 1;

    private static final String SPIFFE_URI_PREFIX = "spiffe://";

    private static final CertificateFactory CERTIFICATE_FACTORY = getX509CertificateFactory();
    private static final CertPathValidator CERT_PATH_VALIDATOR = getPKIXCertPathValidator();

    private static PKIXParameters toPkixParameters(Set<X509Certificate> trustedCerts) {
        try {
            PKIXParameters pkixParameters = new PKIXParameters(trustedCerts.stream()
                    .map(c -> new TrustAnchor(c, null))
                    .collect(toSet()));
            pkixParameters.setRevocationEnabled(false);
            return pkixParameters;
        } catch (InvalidAlgorithmParameterException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Validate a certificate chain against a set of trusted certificates.
     *
     * @param chain        certificate chain
     * @param trustedCerts trusted certificates
     * @throws CertificateException if the validation fails
     */
    public void validate(X509Certificate[] chain, Set<X509Certificate> trustedCerts) throws CertificateException {
        PKIXParameters pkixParameters = toPkixParameters(trustedCerts);
        CertPath certPath = CERTIFICATE_FACTORY.generateCertPath(truncateChain(chain, trustedCerts));

        try {
            CERT_PATH_VALIDATOR.validate(certPath, pkixParameters);
        } catch (CertPathValidatorException | InvalidAlgorithmParameterException e) {
            throw new CertificateException(e);
        }
    }

    /**
     * Get the SPIFFE Id from a SPIFFE certificate.
     *
     * @param certificate certificate
     * @return optional containing the SPIFFE Id
     * @throws CertificateParsingException if parsing the certificate fails
     */
    public static Optional<String> getSpiffeId(X509Certificate certificate) throws CertificateParsingException {
        return Optional.ofNullable(certificate.getSubjectAlternativeNames())
                .flatMap(sans -> sans.stream()
                        .filter(san -> ((Integer) san.get(SAN_OID_I) == SAN_URI_OID))
                        .map(san -> (String) san.get(SAN_VALUE_I))
                        .filter(uri -> startsWith(uri, SPIFFE_URI_PREFIX))
                        .findFirst());
    }

    private static List<X509Certificate> truncateChain(X509Certificate[] chain, Set<X509Certificate> trustedCerts) throws CertificateException {
        Set<X500Principal> trustedPrincipals = trustedCerts.stream()
                .map(X509Certificate::getSubjectX500Principal)
                .collect(toSet());

        for (int i = 0; i < chain.length; i++) {
            X509Certificate certificate = chain[i];
            if (trustedPrincipals.contains(certificate.getIssuerX500Principal())) {
                return asList(chain).subList(0, i);
            }
        }

        throw new CertificateException("Path does not chain with any of the trust anchors");
    }

    private static CertificateFactory getX509CertificateFactory() {
        try {
            return CertificateFactory.getInstance("X.509");
        } catch (CertificateException e) {
            throw new IllegalStateException(e);
        }
    }

    private static CertPathValidator getPKIXCertPathValidator() {
        try {
            return CertPathValidator.getInstance("PKIX");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

}
