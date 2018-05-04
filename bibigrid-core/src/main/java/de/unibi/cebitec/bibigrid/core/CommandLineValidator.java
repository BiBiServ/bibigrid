package de.unibi.cebitec.bibigrid.core;

import de.unibi.cebitec.bibigrid.core.model.*;

import static de.unibi.cebitec.bibigrid.core.util.VerboseOutputFilter.V;

import java.net.InetAddress;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import de.unibi.cebitec.bibigrid.core.model.exceptions.ConfigurationException;
import de.unibi.cebitec.bibigrid.core.model.exceptions.InstanceTypeNotFoundException;
import de.unibi.cebitec.bibigrid.core.util.DefaultPropertiesFile;
import de.unibi.cebitec.bibigrid.core.util.RuleBuilder;
import org.apache.commons.cli.CommandLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class CommandLineValidator {
    protected static final Logger LOG = LoggerFactory.getLogger(CommandLineValidator.class);
    protected final CommandLine cl;
    protected final List<String> req;
    protected final DefaultPropertiesFile defaultPropertiesFile;
    protected final IntentMode intentMode;
    private final ProviderModule providerModule;
    protected final Configuration config;
    private Configuration.SlaveInstanceConfiguration commandLineSlaveInstance;

    public CommandLineValidator(final CommandLine cl, final DefaultPropertiesFile defaultPropertiesFile,
                                final IntentMode intentMode, final ProviderModule providerModule)
            throws ConfigurationException {
        this.cl = cl;
        this.defaultPropertiesFile = defaultPropertiesFile;
        this.intentMode = intentMode;
        this.providerModule = providerModule;
        config = defaultPropertiesFile.loadConfiguration(getProviderConfigurationClass());
        if (config != null && defaultPropertiesFile.isAlternativeFilepath()) {
            config.setAlternativeConfigPath(defaultPropertiesFile.getPropertiesFilePath().toString());
        }
        req = getRequiredOptions();
    }

    /**
     * Create the {@link Configuration} model instance. Must be overridden by provider
     * implementations for specific configuration fields.
     */
    protected abstract Class<? extends Configuration> getProviderConfigurationClass();

    protected final Boolean parseBooleanParameter(RuleBuilder.RuleNames ruleName) {
        if (cl.hasOption(ruleName.getShortParam())) {
            final String value = cl.getOptionValue(ruleName.getShortParam());
            return isStringNullOrEmpty(value) || value.equalsIgnoreCase("yes");
        }
        return null;
    }

    protected final boolean checkRequiredParameter(String shortParam, String value) {
        if (req.contains(shortParam) && isStringNullOrEmpty(value)) {
            LOG.error("-" + shortParam + " option is required!");
            return false;
        }
        return true;
    }

    private boolean parseTerminateParameter() {
        // terminate (cluster-id)
        if (req.contains(IntentMode.TERMINATE.getShortParam())) {
            config.setClusterIds(cl.getOptionValue(IntentMode.TERMINATE.getShortParam()).trim());
        }
        return true;
    }

    private boolean parseCloud9Parameter() {
        // cloud9 (cluster-id)
        if (req.contains(IntentMode.CLOUD9.getShortParam())) {
            config.setClusterIds(cl.getOptionValue(IntentMode.CLOUD9.getShortParam()).trim());
        }
        return true;
    }

    private boolean parseUserNameParameter() {
        final String shortParam = RuleBuilder.RuleNames.USER.getShortParam();
        // Parse command line parameter
        if (cl.hasOption(shortParam)) {
            final String value = cl.getOptionValue(shortParam);
            if (!isStringNullOrEmpty(value)) {
                config.setUser(value);
            }
        }
        return checkRequiredParameter(shortParam, config.getUser());
    }

    private boolean parseSshUserNameParameter() {
        final String shortParam = RuleBuilder.RuleNames.SSH_USER.getShortParam();
        // Parse command line parameter
        if (cl.hasOption(shortParam)) {
            final String value = cl.getOptionValue(shortParam);
            if (!isStringNullOrEmpty(value)) {
                config.setSshUser(value);
            }
        }
        return checkRequiredParameter(shortParam, config.getSshUser());
    }

    private boolean parseNetworkParameter() {
        final String shortParam = RuleBuilder.RuleNames.NETWORK.getShortParam();
        // Parse command line parameter
        if (cl.hasOption(shortParam)) {
            final String value = cl.getOptionValue(shortParam);
            if (!isStringNullOrEmpty(value)) {
                config.setNetwork(value);
            }
        }
        return checkRequiredParameter(shortParam, config.getNetwork());
    }

    private boolean parseSubnetParameter() {
        final String shortParam = RuleBuilder.RuleNames.SUBNET.getShortParam();
        // Parse command line parameter
        if (cl.hasOption(shortParam)) {
            final String value = cl.getOptionValue(shortParam);
            if (!isStringNullOrEmpty(value)) {
                config.setSubnet(value);
            }
        }
        return checkRequiredParameter(shortParam, config.getSubnet());
    }

    private boolean parseCloud9WorkspaceParameter() {
        final String shortParam = RuleBuilder.RuleNames.CLOUD9_WORKSPACE.getShortParam();
        // Parse command line parameter
        if (cl.hasOption(shortParam)) {
            final String value = cl.getOptionValue(shortParam);
            if (!isStringNullOrEmpty(value)) {
                config.setCloud9Workspace(value);
            }
        }
        return checkRequiredParameter(shortParam, config.getCloud9Workspace());
    }

    private boolean parseSoftwareParameters() {
        Boolean parseResult = parseBooleanParameter(RuleBuilder.RuleNames.MESOS);
        if (parseResult != null) {
            config.setMesos(parseResult);
        }
        parseResult = parseBooleanParameter(RuleBuilder.RuleNames.OPEN_GRID_ENGINE);
        if (parseResult != null) {
            config.setOge(parseResult);
        }
        parseResult = parseBooleanParameter(RuleBuilder.RuleNames.NFS);
        if (parseResult != null) {
            config.setNfs(parseResult);
        }
        parseResult = parseBooleanParameter(RuleBuilder.RuleNames.CASSANDRA);
        if (parseResult != null) {
            config.setCassandra(parseResult);
        }
        parseResult = parseBooleanParameter(RuleBuilder.RuleNames.SPARK);
        if (parseResult != null) {
            config.setSpark(parseResult);
        }
        parseResult = parseBooleanParameter(RuleBuilder.RuleNames.HDFS);
        if (parseResult != null) {
            config.setHdfs(parseResult);
        }
        return true;
    }

    private boolean parseSshPublicKeyFileParameter() {
        final String shortParam = RuleBuilder.RuleNames.SSH_PUBLIC_KEY_FILE.getShortParam();
        // Parse command line parameter
        if (cl.hasOption(shortParam)) {
            final String value = cl.getOptionValue(shortParam);
            if (!isStringNullOrEmpty(value)) {
                config.setSshPublicKeyFile(Paths.get(value.trim()).toString());
            }
        }
        // Validate parameter if required
        if (req.contains(shortParam)) {
            if (isStringNullOrEmpty(config.getSshPublicKeyFile())) {
                LOG.error("-" + shortParam + " option is required! Please specify the absolute path to your " +
                        "SSH public key file.");
                return false;
            } else if (!Paths.get(config.getSshPublicKeyFile()).toFile().exists()) {
                LOG.error("SSH public key file '{}' does not exist!", config.getSshPublicKeyFile());
                return false;
            }
        }
        return true;
    }

    private boolean parseSshPrivateKeyFileParameter() {
        final String shortParam = RuleBuilder.RuleNames.SSH_PRIVATE_KEY_FILE.getShortParam();
        // Parse command line parameter
        if (cl.hasOption(shortParam)) {
            final String value = cl.getOptionValue(shortParam);
            if (!isStringNullOrEmpty(value)) {
                config.setSshPrivateKeyFile(Paths.get(value.trim()).toString());
            }
        }
        // Validate parameter if required
        if (req.contains(shortParam)) {
            if (isStringNullOrEmpty(config.getSshPrivateKeyFile())) {
                LOG.error("-" + shortParam + " option is required! Please specify the absolute path to your " +
                        "SSH private key file.");
                return false;
            } else if (!Paths.get(config.getSshPrivateKeyFile()).toFile().exists()) {
                LOG.error("SSH private key file '{}' does not exist!", config.getSshPrivateKeyFile());
                return false;
            }
        }
        return true;
    }

    private boolean parseKeypairParameter() {
        final String shortParam = RuleBuilder.RuleNames.KEYPAIR.getShortParam();
        // Parse command line parameter
        if (cl.hasOption(shortParam)) {
            final String value = cl.getOptionValue(shortParam);
            if (!isStringNullOrEmpty(value)) {
                config.setKeypair(value);
            }
        }
        // Validate parameter if required
        if (req.contains(shortParam)) {
            if (isStringNullOrEmpty(config.getKeypair())) {
                LOG.error("-" + shortParam + " option is required! Please specify the name of your keypair.");
                return false;
            }
        }
        return true;
    }

    private boolean parseRegionParameter() {
        final String shortParam = RuleBuilder.RuleNames.REGION.getShortParam();
        final String envParam = RuleBuilder.RuleNames.REGION.getEnvParam();
        // Parse environment variable if not loaded from config file
        if (isStringNullOrEmpty(config.getRegion()) && !isStringNullOrEmpty(System.getenv(envParam))) {
            config.setRegion(System.getenv(envParam));
        }
        // Parse command line parameter
        if (cl.hasOption(shortParam)) {
            final String value = cl.getOptionValue(shortParam);
            if (!isStringNullOrEmpty(value)) {
                config.setRegion(value);
            }
        }
        // Validate parameter if required
        if (req.contains(shortParam)) {
            if (isStringNullOrEmpty(config.getRegion())) {
                LOG.error("-" + shortParam + " option is required! Please specify a region (e.g. region: eu-west-1).");
                return false;
            }
        }
        return true;
    }

    private boolean parseAvailabilityZoneParameter() {
        final String shortParam = RuleBuilder.RuleNames.AVAILABILITY_ZONE.getShortParam();
        // Parse command line parameter
        if (cl.hasOption(shortParam)) {
            final String value = cl.getOptionValue(shortParam);
            if (!isStringNullOrEmpty(value)) {
                config.setAvailabilityZone(value);
            }
        }
        // Validate parameter if required
        if (req.contains(shortParam)) {
            if (isStringNullOrEmpty(config.getAvailabilityZone())) {
                LOG.error("-" + shortParam + " option is required! Please specify an availability zone " +
                        "(e.g. availabilityZone: eu-west-1a).");
                return false;
            }
        }
        return true;
    }

    private boolean parseMasterInstanceTypeParameter() {
        final String shortParam = RuleBuilder.RuleNames.MASTER_INSTANCE_TYPE.getShortParam();
        // Parse command line parameter
        if (cl.hasOption(shortParam)) {
            final String value = cl.getOptionValue(shortParam);
            if (!isStringNullOrEmpty(value)) {
                config.getMasterInstance().setType(value);
            }
        }
        // Validate parameter if required
        if (req.contains(shortParam)) {
            if (isStringNullOrEmpty(config.getMasterInstance().getType())) {
                LOG.error("-" + shortParam + " option is required! Please specify the master instance type.");
                return false;
            }
        }
        return true;
    }

    private boolean parseMasterInstanceImageParameter() {
        final String shortParam = RuleBuilder.RuleNames.MASTER_IMAGE.getShortParam();
        // Parse command line parameter
        if (cl.hasOption(shortParam)) {
            final String value = cl.getOptionValue(shortParam);
            if (!isStringNullOrEmpty(value)) {
                config.getMasterInstance().setImage(value);
            }
        }
        // Validate parameter if required
        if (req.contains(shortParam)) {
            if (isStringNullOrEmpty(config.getMasterInstance().getImage())) {
                LOG.error("-" + shortParam + " option is required! Please specify the master instance image.");
                return false;
            }
        }
        return true;
    }

    private boolean parseMasterAsComputeParameter() {
        Boolean parseResult = parseBooleanParameter(RuleBuilder.RuleNames.USE_MASTER_AS_COMPUTE);
        if (parseResult != null) {
            config.setUseMasterAsCompute(parseResult);
        }
        return true;
    }

    private boolean parseMasterWithPublicIpParameter() {
        Boolean parseResult = parseBooleanParameter(RuleBuilder.RuleNames.USE_MASTER_WITH_PUBLIC_IP);
        if (parseResult != null) {
            config.setUseMasterWithPublicIp(parseResult);
        }
        return true;
    }

    private boolean parseSlaveInstanceTypeParameter() {
        final String shortParam = RuleBuilder.RuleNames.SLAVE_INSTANCE_TYPE.getShortParam();
        // Parse command line parameter
        if (cl.hasOption(shortParam)) {
            final String value = cl.getOptionValue(shortParam);
            if (!isStringNullOrEmpty(value)) {
                if (commandLineSlaveInstance == null) {
                    commandLineSlaveInstance = new Configuration.SlaveInstanceConfiguration();
                    if (config.getSlaveInstances() == null) {
                        config.setSlaveInstances(new ArrayList<>());
                    }
                    config.getSlaveInstances().add(commandLineSlaveInstance);
                }
                commandLineSlaveInstance.setType(value);
            }
        }
        // Validate parameter if required
        if (req.contains(shortParam)) {
            for (Configuration.InstanceConfiguration instanceConfiguration : config.getSlaveInstances()) {
                if (isStringNullOrEmpty(instanceConfiguration.getType())) {
                    LOG.error("-" + shortParam + " option is required! Please specify the slave instance type.");
                    return false;
                }
            }
        }
        /* TODO: better use check like in validate intent
        if (slaveType.getSpec().isClusterInstance() || config.getMasterInstanceType().getSpec().isClusterInstance()) {
            if (!slaveType.toString().equals(config.getMasterInstanceType().toString())) {
                LOG.warn("The instance types should be the same when using cluster types.");
                LOG.warn("Master Instance Type: " + config.getMasterInstanceType().toString());
                LOG.warn("Slave Instance Type: " + slaveType.toString());
            }
        }
        */
        return true;
    }

    private boolean parseSlaveInstanceCountParameter() {
        final String shortParam = RuleBuilder.RuleNames.SLAVE_INSTANCE_COUNT.getShortParam();
        // Parse command line parameter
        if (cl.hasOption(shortParam)) {
            final String value = cl.getOptionValue(shortParam);
            if (!isStringNullOrEmpty(value)) {
                int count;
                try {
                    count = Integer.parseInt(value);
                } catch (NumberFormatException nfe) {
                    LOG.error("Invalid property value for -" + shortParam +
                            ". Please make sure you have a positive integer here.");
                    return false;
                }
                if (commandLineSlaveInstance == null) {
                    commandLineSlaveInstance = new Configuration.SlaveInstanceConfiguration();
                    if (config.getSlaveInstances() == null) {
                        config.setSlaveInstances(new ArrayList<>());
                    }
                    config.getSlaveInstances().add(commandLineSlaveInstance);
                }
                commandLineSlaveInstance.setCount(count);
            }
        }
        // Validate parameter if required
        if (req.contains(shortParam)) {
            for (Configuration.SlaveInstanceConfiguration instanceConfiguration : config.getSlaveInstances()) {
                if (instanceConfiguration.getCount() < 0) {
                    LOG.error("-" + shortParam + " option is required! Please specify the slave instance count.");
                    return false;
                }
            }
        }
        return true;
    }

    private boolean parseSlaveImageParameter() {
        final String shortParam = RuleBuilder.RuleNames.SLAVE_IMAGE.getShortParam();
        // Parse command line parameter
        if (cl.hasOption(shortParam)) {
            String value = cl.getOptionValue(shortParam);
            if (!isStringNullOrEmpty(value)) {
                if (commandLineSlaveInstance == null) {
                    commandLineSlaveInstance = new Configuration.SlaveInstanceConfiguration();
                    if (config.getSlaveInstances() == null) {
                        config.setSlaveInstances(new ArrayList<>());
                    }
                    config.getSlaveInstances().add(commandLineSlaveInstance);
                }
                commandLineSlaveInstance.setImage(value);
            }
        }
        // Validate parameter if required
        if (req.contains(shortParam)) {
            for (Configuration.SlaveInstanceConfiguration instanceConfiguration : config.getSlaveInstances()) {
                if (isStringNullOrEmpty(instanceConfiguration.getImage())) {
                    LOG.error("-" + shortParam + " option is required! Please specify the slave instance image.");
                    return false;
                }
            }
        }
        return true;
    }

    private boolean parsePortsParameter() {
        final String shortParam = RuleBuilder.RuleNames.PORTS.getShortParam();
        // Parse command line parameter
        if (cl.hasOption(shortParam)) {
            final String value = cl.getOptionValue(shortParam);
            if (!isStringNullOrEmpty(value)) {
                try {
                    Pattern p = Pattern.compile("(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})(?:/(\\d{1,2}))*");
                    String[] portsStrings = value.split(",");
                    List<Port> ports = new ArrayList<>();
                    for (String portString : portsStrings) {
                        Port port;
                        // must distinguish between different notation
                        // 5555
                        // 0.0.0.0:5555
                        // 0.0.0.0/0:5555
                        // current:5555
                        // TODO: implement protocol parsing!
                        if (portString.contains(":")) {
                            String address;
                            String[] tmp = portString.split(":");
                            if (tmp[0].equalsIgnoreCase("current")) {
                                address = InetAddress.getLocalHost().getHostAddress() + "/32";
                            } else {
                                Matcher m = p.matcher(tmp[0]);
                                //noinspection ResultOfMethodCallIgnored
                                m.matches();
                                for (int i = 1; i <= 4; i++) {
                                    checkStringAsInt(m.group(i), 255);
                                }
                                if (m.group(5) != null) {
                                    checkStringAsInt(m.group(5), 32);
                                }
                                address = tmp[0];
                            }
                            port = new Port(address, checkStringAsInt(tmp[1], 65535));
                        } else {
                            port = new Port("0.0.0.0/0", checkStringAsInt(portString.trim(), 65535));
                        }
                        ports.add(port);
                    }
                    config.setPorts(ports);
                } catch (Exception e) {
                    LOG.error("Could not parse the supplied port list, please make sure you have a list of " +
                            "comma-separated valid ports without spaces in between. Valid ports have following " +
                            "pattern: [(current|'ip4v-address'|'ip4v-range/CIDR'):]'portnumber'", e);
                    return false;
                }
            }
        }
        // Validate parameter if required
        if (req.contains(shortParam)) {
            if (config.getPorts() == null) {
                LOG.error("-" + shortParam + " option is required!");
                return false;
            }
        }
        return true;
    }

    private boolean parseMasterMountsParameter() {
        final String shortParam = RuleBuilder.RuleNames.MASTER_MOUNTS.getShortParam();
        // Parse command line parameter
        if (cl.hasOption(shortParam)) {
            final String value = cl.getOptionValue(shortParam);
            if (!isStringNullOrEmpty(value)) {
                List<Configuration.MountPoint> mountPoints = parseMountCsv(value, "Master");
                if (mountPoints != null) {
                    config.setMasterMounts(mountPoints);
                }
            }
        }
        // Validate parameter if required
        if (req.contains(shortParam)) {
            if (config.getMasterMounts() == null) {
                LOG.error("-" + shortParam + " option is required!");
                return false;
            }
        }
        return true;
    }

    private static List<Configuration.MountPoint> parseMountCsv(String mountsCsv, String logName) {
        List<Configuration.MountPoint> mountPoints = new ArrayList<>();
        if (mountsCsv != null && !mountsCsv.isEmpty()) {
            try {
                String[] mounts = mountsCsv.split(",");
                for (String mountKeyValue : mounts) {
                    String[] mountSplit = mountKeyValue.trim().split("=");
                    Configuration.MountPoint mountPoint = new Configuration.MountPoint();
                    mountPoint.setSource(mountSplit[0].trim());
                    mountPoint.setTarget(mountSplit[1].trim());
                    mountPoints.add(mountPoint);
                }
            } catch (Exception e) {
                LOG.error("Could not parse the list of {} mounts, please make sure you have a list of " +
                        "comma-separated key=value pairs without spaces in between.", logName);
                return null;
            }
            if (!mountPoints.isEmpty()) {
                StringBuilder mountsDisplay = new StringBuilder();
                for (Configuration.MountPoint mount : mountPoints) {
                    mountsDisplay.append(mount.getSource()).append(" => ").append(mount.getTarget()).append(" ; ");
                }
                LOG.info(V, "{} mounts: {}", logName, mountsDisplay);
            }
        }
        return mountPoints;
    }

    private boolean parseMasterNfsSharesParameter() {
        final String shortParam = RuleBuilder.RuleNames.NFS_SHARES.getShortParam();
        // Parse command line parameter
        if (cl.hasOption(shortParam)) {
            final String value = cl.getOptionValue(shortParam);
            if (!isStringNullOrEmpty(value)) {
                try {
                    config.setNfsShares(Arrays.stream(value.split(",")).map(String::trim).collect(Collectors.toList()));
                } catch (Exception e) {
                    LOG.error("Could not parse the supplied list of shares, please make sure you have a list of " +
                            "comma-separated paths without spaces in between.");
                    return false;
                }
            }
        }
        // Validate parameter if required
        if (req.contains(shortParam)) {
            if (config.getNfsShares() == null) {
                LOG.error("-" + shortParam + " option is required!");
                return false;
            }
        }
        return true;
    }

    private boolean parseExternalNfsSharesParameter() {
        final String shortParam = RuleBuilder.RuleNames.EXT_NFS_SHARES.getShortParam();
        // Parse command line parameter
        if (cl.hasOption(shortParam)) {
            final String value = cl.getOptionValue(shortParam);
            if (!isStringNullOrEmpty(value)) {
                List<Configuration.MountPoint> mountPoints = parseMountCsv(value, "External share");
                if (mountPoints != null) {
                    config.setExtNfsShares(mountPoints);
                }
            }
        }
        // Validate parameter if required
        if (req.contains(shortParam)) {
            if (config.getExtNfsShares() == null) {
                LOG.error("-" + shortParam + " option is required!");
                return false;
            }
        }
        return true;
    }

    private boolean parseUseSpotInstanceRequestParameter() {
        Boolean parseResult = parseBooleanParameter(RuleBuilder.RuleNames.USE_SPOT_INSTANCE_REQUEST);
        if (parseResult != null) {
            config.setUseSpotInstances(parseResult);
        }
        return true;
    }

    private boolean parseEphemeralFilesystemParameter() {
        final String shortParam = RuleBuilder.RuleNames.LOCAL_FS.getShortParam();
        // Parse command line parameter
        if (cl.hasOption(shortParam)) {
            final String value = cl.getOptionValue(shortParam);
            if (!isStringNullOrEmpty(value)) {
                try {
                    config.setLocalFS(Configuration.FS.valueOf(value.toUpperCase()));
                } catch (IllegalArgumentException e) {
                    String options = String.join(", ", Arrays.stream(Configuration.FS.values())
                            .map(x -> "'" + x.toString().toLowerCase() + "'").collect(Collectors.toList()));
                    LOG.error("Local filesystem must be one of {}!", options);
                    return false;
                }
            }
        }
        // Validate parameter if required
        if (req.contains(shortParam)) {
            if (config.getLocalFS() == null) {
                LOG.error("-" + shortParam + " option is required!");
                return false;
            }
        }
        return true;
    }

    private boolean parseGridPropertiesFileParameter() {
        final String shortParam = RuleBuilder.RuleNames.GRID_PROPERTIES_FILE.getShortParam();
        // Parse command line parameter
        if (cl.hasOption(shortParam)) {
            final String value = cl.getOptionValue(shortParam);
            if (!isStringNullOrEmpty(value)) {
                config.setGridPropertiesFile(value);
            }
        }
        // Check if file already exists and warn
        if (!isStringNullOrEmpty(config.getGridPropertiesFile())) {
            Path prop = Paths.get(config.getGridPropertiesFile());
            if (prop.toFile().exists()) {
                LOG.warn("Overwrite an existing properties file '{}'!", prop);
            }
            LOG.info(V, "Write grid properties to '{}' after successful grid startup!", prop);
        }
        return checkRequiredParameter(shortParam, config.getGridPropertiesFile());
    }

    private boolean parseCredentialsFileParameter() {
        final String shortParam = RuleBuilder.RuleNames.CREDENTIALS_FILE.getShortParam();
        // Parse command line parameter
        if (cl.hasOption(shortParam)) {
            final String value = cl.getOptionValue(shortParam);
            if (!isStringNullOrEmpty(value)) {
                config.setCredentialsFile(value);
            }
        }
        // Validate parameter if required
        if (req.contains(shortParam)) {
            if (isStringNullOrEmpty(config.getCredentialsFile())) {
                LOG.error("-" + shortParam + " option is required!");
                return false;
            } else if (!Paths.get(config.getCredentialsFile()).toFile().exists()) {
                LOG.error("Credentials file '{}' does not exist!", config.getCredentialsFile());
                return false;
            }
        }
        return true;
    }

    private boolean parseDebugRequestsParameter() {
        Boolean parseResult = parseBooleanParameter(RuleBuilder.RuleNames.DEBUG_REQUESTS);
        if (parseResult != null) {
            config.setDebugRequests(parseResult);
        }
        return true;
    }

    public boolean validate(String mode) {
        config.setMode(mode);
        if (providerModule == null) {
            return false;
        }
        if (req == null) {
            return true;
        }
        return parseTerminateParameter() &&
                parseCloud9Parameter() &&
                parseCloud9WorkspaceParameter() &&
                parseUserNameParameter() &&
                parseSshUserNameParameter() &&
                parseNetworkParameter() &&
                parseSubnetParameter() &&
                parseSshPublicKeyFileParameter() &&
                parseSshPrivateKeyFileParameter() &&
                parseKeypairParameter() &&
                parseRegionParameter() &&
                parseAvailabilityZoneParameter() &&
                parseMasterInstanceTypeParameter() &&
                parseMasterInstanceImageParameter() &&
                parseSlaveInstanceTypeParameter() &&
                parseSlaveInstanceCountParameter() &&
                parseMasterAsComputeParameter() &&
                parseMasterWithPublicIpParameter() &&
                parseSlaveImageParameter() &&
                parsePortsParameter() &&
                parseMasterMountsParameter() &&
                parseMasterNfsSharesParameter() &&
                parseExternalNfsSharesParameter() &&
                parseUseSpotInstanceRequestParameter() &&
                parseEphemeralFilesystemParameter() &&
                parseSoftwareParameters() &&
                parseGridPropertiesFileParameter() &&
                parseCredentialsFileParameter() &&
                parseDebugRequestsParameter() &&
                validateProviderParameters();
    }

    protected abstract List<String> getRequiredOptions();

    protected abstract boolean validateProviderParameters();

    public boolean validateProviderTypes(Client client) {
        try {
            InstanceType masterType = providerModule.getInstanceType(client, config, config.getMasterInstance().getType());
            config.getMasterInstance().setProviderType(masterType);
        } catch (InstanceTypeNotFoundException e) {
            LOG.error("Invalid master instance type specified!", e);
            return false;
        }
        try {
            for (Configuration.InstanceConfiguration instanceConfiguration : config.getSlaveInstances()) {
                InstanceType slaveType = providerModule.getInstanceType(client, config, instanceConfiguration.getType());
                instanceConfiguration.setProviderType(slaveType);
            }
        } catch (InstanceTypeNotFoundException e) {
            LOG.error("Invalid slave instance type specified!", e);
            return false;
        }
        return true;
    }

    protected static boolean isStringNullOrEmpty(final String s) {
        return s == null || s.trim().isEmpty();
    }

    private int checkStringAsInt(String s, int max) throws Exception {
        int v = Integer.parseInt(s);
        if (v < 0 || v > max) {
            throw new Exception();
        }
        return v;
    }

    public Configuration getConfig() {
        return config;
    }
}
