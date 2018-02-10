package de.unibi.cebitec.bibigrid.googlecloud;

import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.MachineType;
import com.google.api.services.compute.model.MachineTypesScopedList;
import de.unibi.cebitec.bibigrid.core.CommandLineValidator;
import de.unibi.cebitec.bibigrid.core.intents.CreateCluster;
import de.unibi.cebitec.bibigrid.core.intents.ListIntent;
import de.unibi.cebitec.bibigrid.core.intents.TerminateIntent;
import de.unibi.cebitec.bibigrid.core.intents.ValidateIntent;
import de.unibi.cebitec.bibigrid.core.model.Configuration;
import de.unibi.cebitec.bibigrid.core.model.InstanceType;
import de.unibi.cebitec.bibigrid.core.model.IntentMode;
import de.unibi.cebitec.bibigrid.core.model.ProviderModule;
import de.unibi.cebitec.bibigrid.core.util.DefaultPropertiesFile;
import org.apache.commons.cli.CommandLine;

import java.util.HashMap;

/**
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
@SuppressWarnings("unused")
public class ProviderModuleGoogleCloud extends ProviderModule {
    @Override
    public String getName() {
        return "googlecloud";
    }

    @Override
    public CommandLineValidator getCommandLineValidator(final CommandLine commandLine,
                                                        final DefaultPropertiesFile defaultPropertiesFile,
                                                        final IntentMode intentMode) {
        return new CommandLineValidatorGoogleCloud(commandLine, defaultPropertiesFile, intentMode, this);
    }

    @Override
    public ListIntent getListIntent(Configuration config) {
        return new ListIntentGoogleCloud((ConfigurationGoogleCloud) config);
    }

    @Override
    public TerminateIntent getTerminateIntent(Configuration config) {
        return new TerminateIntentGoogleCloud((ConfigurationGoogleCloud) config);
    }

    @Override
    public CreateCluster getCreateIntent(Configuration config) {
        return new CreateClusterGoogleCloud((ConfigurationGoogleCloud) config, this);
    }

    @Override
    public ValidateIntent getValidateIntent(Configuration config) {
        return new ValidateIntentGoogleCloud((ConfigurationGoogleCloud) config);
    }

    @Override
    public String getBlockDeviceBase() {
        return "/dev/sd";
    }

    @Override
    public HashMap<String, InstanceType> getInstanceTypeMap(Configuration config) {
        Compute compute = GoogleCloudUtils.getComputeService((ConfigurationGoogleCloud) config);
        if (compute == null) {
            return null;
        }
        String projectId = ((ConfigurationGoogleCloud) config).getGoogleProjectId();
        String zone = config.getAvailabilityZone();
        HashMap<String, InstanceType> instanceTypes = new HashMap<>();
        try {
            if (zone == null) {
                for (MachineTypesScopedList scopedList :
                        compute.machineTypes().aggregatedList(projectId).execute().getItems().values()) {
                    for (MachineType f : scopedList.getMachineTypes()) {
                        instanceTypes.put(f.getName(), new InstanceTypeGoogleCloud(f));
                    }
                }
            } else {
                for (MachineType f : compute.machineTypes().list(projectId, zone).execute().getItems()) {
                    instanceTypes.put(f.getName(), new InstanceTypeGoogleCloud(f));
                }
            }
        } catch (Exception ignored) {
        }
        return instanceTypes;
    }
}
