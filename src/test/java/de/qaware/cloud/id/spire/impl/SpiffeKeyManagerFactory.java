package de.qaware.cloud.id.spire.impl;

import de.qaware.cloud.id.TestResources;
import de.qaware.cloud.id.spire.SVIDBundle;
import de.qaware.cloud.id.util.ExponentialBackoffSupplier;
import lombok.extern.slf4j.Slf4j;
import spire.api.workload.WorkloadOuterClass.WorkloadEntry;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactorySpi;
import javax.net.ssl.ManagerFactoryParameters;
import java.security.KeyStore;
import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;

@Slf4j
public class SpiffeKeyManagerFactory extends KeyManagerFactorySpi {

    private KeyManager keyManager;

    @Override
    protected void engineInit(KeyStore keyStore, char[] chars) {
        LOGGER.error("Delegating to a keystore backed key manager is not supported yet");
    }

    @Override
    protected void engineInit(ManagerFactoryParameters managerFactoryParameters) {
        LOGGER.error("Delegating to a keystore backed key manager is not supported yet");
    }

    private void init() {
        // TODO: Make this work and remove the mock below
        // SocketChannelFactory channelFactory = new SocketChannelFactory("/tmp/test.sock");
        // WorkloadEntriesSupplier workloadEntriesSupplier = new WorkloadEntriesSupplier(channelFactory);

        Supplier<List<WorkloadEntry>> workloadEntriesSupplier = () -> TestResources.getTestBundles().getBundlesList();

        Supplier<List<SVIDBundle>> bundlesSupplier = new ExponentialBackoffSupplier<>(
                new BundlesSupplier(workloadEntriesSupplier),
                2_000,
                60_000,
                1.5);

        BundleSupplier bundleSupplier = new BundleSupplier(
                bundlesSupplier,
                Duration.ofMinutes(15),
                Duration.ofMinutes(1));

        bundleSupplier.start();

        keyManager = new SpiffeKeyManager(bundleSupplier);
    }

    @Override
    protected KeyManager[] engineGetKeyManagers() {
        if (keyManager == null) {
            init();
        }

        return new KeyManager[]{keyManager};
    }

}
