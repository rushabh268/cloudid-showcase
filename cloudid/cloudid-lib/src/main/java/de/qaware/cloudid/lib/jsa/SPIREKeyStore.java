package de.qaware.cloudid.lib.jsa;

import de.qaware.cloudid.lib.spire.CloudId;
import de.qaware.cloudid.lib.spire.CloudIdManager;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.io.OutputStream;
import java.security.Key;
import java.security.KeyStoreSpi;
import java.security.cert.Certificate;
import java.time.Instant;
import java.util.Date;
import java.util.Enumeration;
import java.util.Objects;

import static java.util.Collections.enumeration;
import static java.util.Collections.singleton;

/**
 * SPIRE key store.
 * <p>
 * Uses a fixed alias.
 */
@Slf4j
public class SPIREKeyStore extends KeyStoreSpi {

    private final CloudIdManager cloudIdManager;

    /**
     * Constructor
     */
    public SPIREKeyStore() {
        cloudIdManager = CloudId.getManager();
    }

    @Override
    public Key engineGetKey(String alias, char[] password) {
        LOGGER.trace("engineGetKey({}, ...)", alias);
        return cloudIdManager.getPreferredBundle().getKeyPair().getPrivate();
    }

    @Override
    public Certificate[] engineGetCertificateChain(String alias) {
        LOGGER.trace("engineGetCertificateChain({})", alias);

        return cloudIdManager.getPreferredBundle().getCaCertChainArray();
    }

    @Override
    public Certificate engineGetCertificate(String alias) {
        LOGGER.trace("engineGetCertificate({})", alias);
        return cloudIdManager.getPreferredBundle().getCertificate();
    }

    @Override
    public Date engineGetCreationDate(String alias) {
        LOGGER.trace("engineGetCreationDate({})", alias);
        return Date.from(Instant.EPOCH);
    }

    @Override
    public Enumeration<String> engineAliases() {
        LOGGER.trace("engineAliases()");
        return enumeration(singleton(SPIREProvider.ALIAS));
    }

    @Override
    public boolean engineContainsAlias(String alias) {
        LOGGER.trace("engineContainsAlias({})", alias);
        return Objects.equals(alias, SPIREProvider.ALIAS);
    }

    @Override
    public int engineSize() {
        LOGGER.trace("engineSize()");
        return 1;
    }

    @Override
    public boolean engineIsKeyEntry(String alias) {
        LOGGER.trace("engineIsKeyEntry({})", alias);
        return Objects.equals(alias, SPIREProvider.ALIAS);
    }

    @Override
    public boolean engineIsCertificateEntry(String alias) {
        LOGGER.trace("engineIsCertificateEntry({})", alias);
        return Objects.equals(alias, SPIREProvider.ALIAS);
    }

    @Override
    public String engineGetCertificateAlias(Certificate cert) {
        LOGGER.trace("engineGetCertificateAlias({})", cert);
        return SPIREProvider.ALIAS;
    }

    @Override
    public void engineLoad(InputStream stream, char[] password) {
        LOGGER.trace("engineLoad({}, ...)", stream);
    }

    @Override
    public void engineSetKeyEntry(String alias, Key key, char[] password, Certificate[] chain) {
        LOGGER.warn("engineSetKeyEntry({}, {}, ..., {})", alias, key, chain);
    }

    @Override
    public void engineSetKeyEntry(String alias, byte[] key, Certificate[] chain) {
        LOGGER.warn("engineSetKeyEntry({}, {}, {})", alias, key, chain);
    }

    @Override
    public void engineSetCertificateEntry(String alias, Certificate cert) {
        LOGGER.warn("engineSetCertificateEntry({}, {})", alias, cert);
    }

    @Override
    public void engineDeleteEntry(String alias) {
        LOGGER.warn("engineDeleteEntry({}, {})", alias);
    }

    @Override
    public void engineStore(OutputStream stream, char[] password) {
        LOGGER.warn("engineStore({}, ...)", stream);
    }
}
