package de.unibi.cebitec.bibigrid.openstack.intents;

import de.unibi.cebitec.bibigrid.core.intents.ValidateIntent;
import de.unibi.cebitec.bibigrid.core.model.Client;
import de.unibi.cebitec.bibigrid.core.model.Configuration;
import de.unibi.cebitec.bibigrid.core.model.InstanceType;
import de.unibi.cebitec.bibigrid.core.model.ProviderModule;
import de.unibi.cebitec.bibigrid.openstack.ClientOpenstack;
import org.openstack4j.api.OSClient;
import org.openstack4j.model.compute.AbsoluteLimit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class ValidateIntentOpenstack extends ValidateIntent {
    private static final Logger LOG = LoggerFactory.getLogger(PrepareIntentOpenstack.class);
    private final OSClient.OSClientV3 os;

    public ValidateIntentOpenstack(ProviderModule providerModule, Client client, Configuration config) {
        super(client, config);
        os = ((ClientOpenstack) providerModule.getClient()).getInternal();
    }

    /**
     * Checks, if quotas exceeded before creating instances.
     * Checked parameters are the openstack compute parameters instances, cores (VCPUs) and RAM
     * @return true, if quotas not exceeded -> Otherwise no creating instances allowed / possible
     */
    public boolean checkQuotasExceeded(Map<InstanceType, Integer> instanceTypes) {
        LOG.info("Checking quotas...");
        AbsoluteLimit quotaLimits = os.compute().quotaSets().limits().getAbsolute();
        int available_instances = quotaLimits.getMaxTotalInstances() - quotaLimits.getTotalInstancesUsed();
        LOG.info("Available instances: {}", available_instances);
        int available_cores = quotaLimits.getMaxTotalCores() - quotaLimits.getTotalCoresUsed();
        LOG.info("Available cores: {}", available_cores);
        int available_ram = quotaLimits.getMaxTotalRAMSize() - quotaLimits.getTotalRAMUsed();
        LOG.info("Available RAM: {}", available_ram);
        for (Map.Entry<InstanceType, Integer> instanceType : instanceTypes.entrySet()) {
            int instances_usage = instanceType.getValue();
            int cores_usage = instanceType.getKey().getCpuCores() * instances_usage;
            int ram_usage = instanceType.getKey().getMaxRam() * instances_usage;
            LOG.info("InstanceType {} uses {} additional instances with {} cores and {} RAM in total…", instanceType.getKey(), instances_usage, cores_usage, ram_usage);
            // Decreasing available quotas for each type...
            available_instances -= instances_usage;
            available_cores -= cores_usage;
            available_ram -= ram_usage;
        }
        if (available_instances <= 0) {
            int computed_total_usage = quotaLimits.getMaxTotalInstances() - available_instances;
            LOG.error("Too many instances would be launched. {} of {}.", computed_total_usage, quotaLimits.getMaxTotalInstances());
            return true;
        }
        if (available_cores <= 0) {
            int computed_total_usage = quotaLimits.getMaxTotalCores() - available_cores;
            LOG.error("Too many cores would be used. {} of {}.", computed_total_usage, quotaLimits.getMaxTotalCores());
            return true;
        }
        if (available_ram <= 0) {
            int computed_total_usage = quotaLimits.getMaxTotalRAMSize() - available_ram;
            LOG.error("Too many RAM would be used. {} of {}.", computed_total_usage, quotaLimits.getMaxTotalRAMSize());
            return true;
        }
        LOG.info("Quotas sufficient. Continuing ...");
        return false;
    }
}
