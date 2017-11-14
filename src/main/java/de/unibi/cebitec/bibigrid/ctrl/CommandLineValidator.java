package de.unibi.cebitec.bibigrid.ctrl;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import de.unibi.cebitec.bibigrid.meta.aws.InstanceTypeAWS;
import de.unibi.cebitec.bibigrid.meta.googlecloud.InstanceTypeGoogleCloud;
import de.unibi.cebitec.bibigrid.meta.openstack.InstanceTypeOpenstack;
import de.unibi.cebitec.bibigrid.model.Configuration;
import de.unibi.cebitec.bibigrid.model.Configuration.FS;
import de.unibi.cebitec.bibigrid.model.Configuration.MODE;
import de.unibi.cebitec.bibigrid.model.InstanceType;
import de.unibi.cebitec.bibigrid.model.OpenStackCredentials;
import de.unibi.cebitec.bibigrid.model.Port;

import static de.unibi.cebitec.bibigrid.util.VerboseOutputFilter.V;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.unibi.cebitec.bibigrid.util.RuleBuilder;
import org.apache.commons.cli.CommandLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommandLineValidator {
    private static final String CMD_PARAMETER_MODE = "mode";
    private static final String KEYWORD_YES = "yes";
    private static final String KEYWORD_NO = "no";

    private class ParseResult {
        String value;
        boolean success;

        boolean getEnabled() {
            return value != null && value.equalsIgnoreCase(KEYWORD_YES);
        }
    }

    private static final String DEFAULT_PROPERTIES_DIRNAME = System.getProperty("user.home");
    private static final String DEFAULT_PROPERTIES_FILENAME = ".bibigrid.properties";
    public static final Logger LOG = LoggerFactory.getLogger(CommandLineValidator.class);
    private final CommandLine cl;
    private final Configuration cfg;
    private final Intent intent;
    private Path propertiesFilePath;

    private Properties machineImage = null;

    public CommandLineValidator(CommandLine cl, Intent intent) {
        this.cl = cl;
        this.intent = intent;
        this.cfg = new Configuration();
        if (cl.hasOption("o")) {
            String path = this.cl.getOptionValue("o");
            Path newPath = FileSystems.getDefault().getPath(path);
            if (Files.isReadable(newPath)) {
                propertiesFilePath = newPath;
                LOG.info("Alternative config file {} will be used.", newPath.toString());
                cfg.setAlternativeConfigFile(true);
                cfg.setAlternativeConfigPath(newPath.toString());
            } else {
                LOG.error("Alternative config ({}) file is not readable. Try to use default.", newPath.toString());
            }
        }
        if (propertiesFilePath == null) {
            propertiesFilePath = FileSystems.getDefault().getPath(DEFAULT_PROPERTIES_DIRNAME, DEFAULT_PROPERTIES_FILENAME);
        }
        // some messages
        if (Files.exists(propertiesFilePath)) {
            LOG.info(V, "Reading default options from properties file at '{}'.", propertiesFilePath);
        } else {
            LOG.info("No properties file for default options found ({}). Using command line parameters only.", propertiesFilePath);
        }
    }

    private Properties loadDefaultsFromPropertiesFile() {
        Properties defaultProperties = new Properties();
        try {
            if (propertiesFilePath != null) {
                defaultProperties.load(Files.newInputStream(propertiesFilePath));
            }
        } catch (IOException e) {
            //nothing to do here, just return empty properties. validate() will catch that.
        }
        return defaultProperties;
    }

    private ParseResult parseYesNoParameter(Properties defaults, String name, RuleBuilder.RuleNames shortParam,
                                            RuleBuilder.RuleNames longParam) {
        ParseResult result = new ParseResult();
        result.success = true;
        if (this.cl.hasOption(shortParam.toString())) {
            result.value = KEYWORD_YES;
            LOG.info(V, name + " support enabled.");
        } else if (defaults.containsKey(longParam.toString())) {
            String value = defaults.getProperty(longParam.toString());
            if (value.equalsIgnoreCase(KEYWORD_YES)) {
                result.value = KEYWORD_YES;
                LOG.info(V, name + " support enabled.");
            } else if (value.equalsIgnoreCase(KEYWORD_NO)) {
                result.value = KEYWORD_NO;
                LOG.info(V, name + " support disabled.");
            } else {
                LOG.error(name + " value not recognized. Please use yes/no.");
                result.success = false;
            }
        }
        return result;
    }

    private ParseResult parseParameter(Properties defaults, RuleBuilder.RuleNames shortParam,
                                       RuleBuilder.RuleNames longParam, RuleBuilder.RuleNames envParam) {
        ParseResult result = new ParseResult();
        result.success = true;
        if (cl.hasOption(shortParam.toString())) {
            result.value = cl.getOptionValue(shortParam.toString()).trim();
        } else if (defaults.getProperty(longParam.toString()) != null) {
            result.value = defaults.getProperty(longParam.toString()).trim();
        } else if (envParam != null && System.getenv(envParam.toString()) != null) {
            result.value = System.getenv(envParam.toString()).trim();
        } else {
            result.success = false;
        }
        return result;
    }

    private boolean parseTerminateParameter(List<String> req) {
        // terminate (cluster-id)
        if (req.contains("t")) {
            this.cfg.setClusterId(this.cl.getOptionValue("t"));
        }
        return true;
    }

    private boolean parseAwsCredentialsFileParameter(List<String> req, Properties defaults) {
        final String shortParam = RuleBuilder.RuleNames.AWS_CREDENTIALS_FILE_S.toString();
        final String longParam = RuleBuilder.RuleNames.AWS_CREDENTIALS_FILE_L.toString();
        if (req.contains(shortParam)) {
            String awsCredentialsFilePath = null;
            if (defaults.containsKey(longParam)) {
                awsCredentialsFilePath = defaults.getProperty(longParam);
            }
            if (this.cl.hasOption(shortParam)) {
                awsCredentialsFilePath = this.cl.getOptionValue(shortParam);
            }
            if (awsCredentialsFilePath == null) {
                if (Files.exists(this.propertiesFilePath)) {
                    awsCredentialsFilePath = this.propertiesFilePath.toString();
                } else {
                    LOG.error("Default credentials file not found! ({})", this.propertiesFilePath);
                    LOG.error("-" + shortParam + " option is required! Please specify the properties file containing the aws credentials.");
                    return false;
                }
            }
            File credentialsFile = new File(awsCredentialsFilePath);
            try {
                AWSCredentials keys = new PropertiesCredentials(credentialsFile);
                this.cfg.setCredentials(keys);
                LOG.info(V, "AWS-Credentials successfully loaded! ({})", awsCredentialsFilePath);
            } catch (IOException | IllegalArgumentException e) {
                LOG.error("AWS-Credentials from properties: {}", e.getMessage());
                return false;
            }
        }
        return true;
    }

    private boolean parseGoogleCloudParameters(Properties defaults) {
        final String shortParamProject = RuleBuilder.RuleNames.GOOGLE_PROJECT_ID_S.toString();
        final String longParamProject = RuleBuilder.RuleNames.GOOGLE_PROJECT_ID_L.toString();
        if (cfg.getMode().equals(MODE.GOOGLECLOUD)) {
            if (cl.hasOption(shortParamProject)) {  // Google Cloud - required
                cfg.setGoogleProjectId(cl.getOptionValue(shortParamProject).trim());
            } else if (defaults.containsKey(longParamProject)) {
                cfg.setGoogleProjectId(defaults.getProperty(longParamProject));
            } else {
                LOG.error("No suitable entry for Google-ProjectId (" + shortParamProject + ") found! Exit");
                return false;
            }

            final String shortParamCredentials = RuleBuilder.RuleNames.GOOGLE_CREDENTIALS_FILE_S.toString();
            final String longParamCredentials = RuleBuilder.RuleNames.GOOGLE_CREDENTIALS_FILE_L.toString();
            if (cl.hasOption(shortParamCredentials)) {  // Google Cloud - required
                cfg.setGoogleCredentialsFile(cl.getOptionValue(shortParamCredentials));
            } else if (defaults.containsKey(longParamCredentials)) {
                cfg.setGoogleCredentialsFile(defaults.getProperty(longParamCredentials));
            } else {
                LOG.error("No suitable entry for Google-Credentials-File (" + shortParamCredentials + ") found! Exit");
                return false;
            }
        }
        return true;
    }

    private boolean parseOpenStackParameters(Properties defaults) {
        if (!cfg.getMode().equals(MODE.OPENSTACK)) {
            return true;
        }
        OpenStackCredentials osc = new OpenStackCredentials();
        // OpenStack username
        ParseResult result = parseParameter(defaults, RuleBuilder.RuleNames.OPENSTACK_USERNAME_S,
                RuleBuilder.RuleNames.OPENSTACK_USERNAME_L, RuleBuilder.RuleNames.OPENSTACK_USERNAME_ENV);
        if (!result.success) {
            LOG.error("No suitable entry for OpenStack-Username (osu) found nor environment OS_USERNAME set! Exit");
            return false;
        }
        osc.setUsername(result.value);
        // OpenStack tenant name
        result = parseParameter(defaults, RuleBuilder.RuleNames.OPENSTACK_TENANT_NAME_S,
                RuleBuilder.RuleNames.OPENSTACK_TENANT_NAME_L, RuleBuilder.RuleNames.OPENSTACK_TENANT_NAME_ENV);
        if (!result.success) {
            LOG.error("No suitable entry for OpenStack-Tenantname (ost) found nor environment OS_PROJECT_NAME set! Exit");
            return false;
        }
        osc.setTenantDomain(result.value);
        // OpenStack tenant domain
        result = parseParameter(defaults, RuleBuilder.RuleNames.OPENSTACK_TENANT_DOMAIN_S,
                RuleBuilder.RuleNames.OPENSTACK_TENANT_DOMAIN_L, null);
        if (!result.success) {
            LOG.info("No suitable entry for OpenStack-TenantDomain (ostd) found! Use OpenStack-Domain(osd) instead!");
            return false;
        }
        osc.setTenantDomain(result.value);
        // OpenStack password
        result = parseParameter(defaults, RuleBuilder.RuleNames.OPENSTACK_PASSWORD_S,
                RuleBuilder.RuleNames.OPENSTACK_PASSWORD_L, RuleBuilder.RuleNames.OPENSTACK_PASSWORD_ENV);
        if (!result.success) {
            LOG.error("No suitable entry for OpenStack-Password (osp) found nor environment OS_PASSWORD set! Exit");
            return false;
        }
        osc.setPassword(result.value);
        // OpenStack endpoint
        result = parseParameter(defaults, RuleBuilder.RuleNames.OPENSTACK_ENDPOINT_S,
                RuleBuilder.RuleNames.OPENSTACK_ENDPOINT_L, RuleBuilder.RuleNames.OPENSTACK_ENDPOINT_ENV);
        if (!result.success) {
            LOG.error("No suitable entry for OpenStack-Endpoint (ose) found nor environment OS_AUTH_URL set! Exit");
            return false;
        }
        osc.setEndpoint(result.value);
        // OpenStack domain
        result = parseParameter(defaults, RuleBuilder.RuleNames.OPENSTACK_DOMAIN_S,
                RuleBuilder.RuleNames.OPENSTACK_DOMAIN_L, RuleBuilder.RuleNames.OPENSTACK_DOMAIN_ENV);
        if (result.success) {
            osc.setDomain(result.value);
        } else {
            LOG.info("Keystone V2 API.");
        }
        this.cfg.setOpenstackCredentials(osc);
        return true;
    }

    private boolean parseUserNameParameter(Properties defaults) {
        final String shortParam = RuleBuilder.RuleNames.USER_S.toString();
        final String longParam = RuleBuilder.RuleNames.USER_L.toString();
        if (cl.hasOption(shortParam) || defaults.containsKey(longParam)) {
            String value = cl.getOptionValue(shortParam, defaults.getProperty(longParam));
            if (value != null && !value.isEmpty()) {
                cfg.setUser(value);
            } else {
                LOG.error("User (-" + shortParam + ") can't be null or empty.");
                return false;
            }
        }
        return true;
    }

    private boolean parseNetworkParameters(Properties defaults) {
        // AWS - required
        ParseResult result = parseParameter(defaults, RuleBuilder.RuleNames.VPC_S, RuleBuilder.RuleNames.VPC_L, null);
        if (result.success) {
            cfg.setVpcid(result.value);
        }
        // Openstack - optional, but recommend
        result = parseParameter(defaults, RuleBuilder.RuleNames.ROUTER_S, RuleBuilder.RuleNames.ROUTER_L, null);
        if (result.success) {
            cfg.setRoutername(result.value);
        }
        // Openstack - optional
        result = parseParameter(defaults, RuleBuilder.RuleNames.NETWORK_S, RuleBuilder.RuleNames.NETWORK_L, null);
        if (result.success) {
            cfg.setNetworkname(result.value);
        }
        // Openstack - optional
        result = parseParameter(defaults, RuleBuilder.RuleNames.SUBNET_S, RuleBuilder.RuleNames.SUBNET_L, null);
        if (result.success) {
            cfg.setSubnetname(result.value);
        }
        return true;
    }

    private boolean parseMesosParameters(Properties defaults) {
        ParseResult result = parseYesNoParameter(defaults, "Mesos", RuleBuilder.RuleNames.MESOS_S,
                RuleBuilder.RuleNames.MESOS_L);
        if (result.success) {
            this.cfg.setMesos(result.getEnabled());
        }
        return result.success;
    }

    private boolean parseGridEngineParameters(Properties defaults) {
        ParseResult result = parseYesNoParameter(defaults, "OpenGridEngine", RuleBuilder.RuleNames.OPEN_GRID_ENGINE_S,
                RuleBuilder.RuleNames.OPEN_GRID_ENGINE_L);
        if (result.success) {
            this.cfg.setOge(result.getEnabled());
        }
        return result.success;
    }

    private boolean parseNfsParameters(Properties defaults) {
        ParseResult result = parseYesNoParameter(defaults, "NFS", RuleBuilder.RuleNames.NFS_S,
                RuleBuilder.RuleNames.NFS_L);
        if (result.success) {
            this.cfg.setNfs(result.getEnabled());
        }
        return result.success;
    }

    private boolean parseCassandraParameters(Properties defaults) {
        ParseResult result = parseYesNoParameter(defaults, "Cassandra", RuleBuilder.RuleNames.CASSANDRA_S,
                RuleBuilder.RuleNames.CASSANDRA_L);
        if (result.success) {
            this.cfg.setCassandra(result.getEnabled());
        }
        return result.success;
    }

    private boolean parseSparkParameters(Properties defaults) {
        ParseResult result = parseYesNoParameter(defaults, "Spark", RuleBuilder.RuleNames.SPARK_S,
                RuleBuilder.RuleNames.SPARK_L);
        if (result.success) {
            this.cfg.setHdfs(result.getEnabled());
        }
        return result.success;
    }

    private boolean parseHdfsParameters(Properties defaults) {
        ParseResult result = parseYesNoParameter(defaults, "HDFS", RuleBuilder.RuleNames.HDFS_S,
                RuleBuilder.RuleNames.HDFS_L);
        if (result.success) {
            this.cfg.setSpark(result.getEnabled());
        }
        return result.success;
    }

    private boolean parseIdentityFileParameter(List<String> req, Properties defaults) {
        final String shortParam = RuleBuilder.RuleNames.IDENTITY_FILE_S.toString();
        final String longParam = RuleBuilder.RuleNames.IDENTITY_FILE_L.toString();
        if (req.contains(shortParam)) {
            String identityFilePath = null;
            if (defaults.containsKey(longParam)) {
                identityFilePath = defaults.getProperty(longParam);
            }
            if (this.cl.hasOption(shortParam)) {
                identityFilePath = this.cl.getOptionValue(shortParam);
            }
            if (identityFilePath == null) {
                LOG.error("-" + shortParam + " option is required! Please specify the absolute path to your identity " +
                        "file (private ssh key file).");
                return false;
            }
            Path identity = FileSystems.getDefault().getPath(identityFilePath);
            if (!identity.toFile().exists()) {
                LOG.error("Identity private key file '{}' does not exist!", identity);
                return false;
            }
            this.cfg.setIdentityFile(identity);
            LOG.info(V, "Identity file found! ({})", identity);
        }
        return true;
    }

    private boolean parseKeypairParameter(List<String> req, Properties defaults) {
        final String shortParam = RuleBuilder.RuleNames.KEYPAIR_S.toString();
        final String longParam = RuleBuilder.RuleNames.KEYPAIR_L.toString();
        if (req.contains(shortParam)) {
            this.cfg.setKeypair(this.cl.getOptionValue(shortParam, defaults.getProperty(longParam)));
            if (this.cfg.getKeypair() == null) {
                LOG.error("-" + shortParam + " option is required! Please specify the name of your keypair.");
                return false;
            }
            LOG.info(V, "Keypair name set. ({})", this.cfg.getKeypair());
        }
        return true;
    }

    private boolean parseRegionParameter(List<String> req, Properties defaults) {
        final String shortParam = RuleBuilder.RuleNames.REGION_S.toString();
        final String longParam = RuleBuilder.RuleNames.REGION_L.toString();
        final String envParam = RuleBuilder.RuleNames.REGION_ENV.toString();
        if (req.contains(shortParam)) {
            cfg.setRegion(cl.getOptionValue(shortParam, defaults.getProperty(longParam)));
            if (this.cfg.getRegion() == null) {
                if (System.getenv(envParam) != null) {
                    cfg.setRegion(System.getenv(envParam));
                } else {
                    LOG.error("-" + shortParam + " option is required! Please specify the url of your region "
                            + "(e.g. region=eu-west-1).");
                    return false;
                }
            }
            LOG.info(V, "Region set. ({})", this.cfg.getRegion());
        }
        return true;
    }

    private boolean parseAvailabilityZoneParameter(List<String> req, Properties defaults) {
        final String shortParam = RuleBuilder.RuleNames.AVAILABILITY_ZONE_S.toString();
        final String longParam = RuleBuilder.RuleNames.AVAILABILITY_ZONE_L.toString();
        if (req.contains(shortParam)) {
            this.cfg.setAvailabilityZone(this.cl.getOptionValue(shortParam, defaults.getProperty(longParam)));
            if (this.cfg.getAvailabilityZone() == null) {
                LOG.error("-" + shortParam + " option is required! Please specify an availability zone "
                        + "(e.g. availability-zone=eu-west-1a).");
                return false;
            }
            LOG.info(V, "Availability zone set. ({})", this.cfg.getAvailabilityZone());
        }
        return true;
    }

    private boolean parseMasterInstanceTypeParameter(List<String> req, Properties defaults) {
        final String shortParam = RuleBuilder.RuleNames.MASTER_INSTANCE_TYPE_S.toString();
        final String longParam = RuleBuilder.RuleNames.MASTER_INSTANCE_TYPE_L.toString();
        if (req.contains(shortParam)) {
            try {
                String masterTypeString = this.cl.getOptionValue(shortParam, defaults.getProperty(longParam));
                if (masterTypeString == null) {
                    LOG.error("-" + shortParam + " option is required! Please specify the instance type of your " +
                            "master node. (e.g. master-instance-type=t1.micro)");
                    return false;
                }

                InstanceType masterType = null;
                switch (cfg.getMode()) {
                    case AWS:
                        masterType = new InstanceTypeAWS(masterTypeString.trim());
                        break;
                    case OPENSTACK:
                        masterType = new InstanceTypeOpenstack(cfg, masterTypeString.trim());
                        break;
                    case GOOGLECLOUD:
                        masterType = new InstanceTypeGoogleCloud(masterTypeString.trim());
                        break;
                }
                this.cfg.setMasterInstanceType(masterType);
            } catch (Exception e) {
                LOG.error("Invalid master instance type specified!", e);
                return false;
            }
            LOG.info(V, "Master instance type set. ({})", this.cfg.getMasterInstanceType());
        }
        return true;
    }

    private boolean parseMasterImageParameter(List<String> req, Properties defaults) {
        final String shortParam = RuleBuilder.RuleNames.MASTER_IMAGE_S.toString();
        final String longParam = RuleBuilder.RuleNames.MASTER_IMAGE_L.toString();
        if (req.contains(shortParam)) {
            cfg.setMasterImage(cl.getOptionValue(shortParam, defaults.getProperty(longParam)));
            if (cfg.getMasterImage() == null) {
                // try to load machine image id from URL
                loadMachineImageId();
                if (machineImage != null) {
                    cfg.setMasterImage(machineImage.getProperty("master"));
                } else {
                    LOG.error("-" + shortParam + " option is required! Please specify the AMI ID for your master node.");
                    return false;
                }
            }
            LOG.info(V, "Master image set. ({})", cfg.getMasterImage());
        }
        return true;
    }

    private boolean parseSlaveInstanceTypeParameter(List<String> req, Properties defaults) {
        final String shortParam = RuleBuilder.RuleNames.SLAVE_INSTANCE_TYPE_S.toString();
        final String longParam = RuleBuilder.RuleNames.SLAVE_INSTANCE_TYPE_L.toString();
        if (req.contains(shortParam)) {
            try {
                String slaveTypeString = this.cl.getOptionValue(shortParam, defaults.getProperty(longParam));
                if (slaveTypeString == null) {
                    LOG.error("-" + shortParam + " option is required! Please specify the instance type of your " +
                            "slave nodes (" + longParam + "=t1.micro).");
                    return false;
                }
                InstanceType slaveType = null;
                switch (cfg.getMode()) {
                    case AWS:
                        slaveType = new InstanceTypeAWS(slaveTypeString.trim());
                        break;
                    case OPENSTACK:
                        slaveType = new InstanceTypeOpenstack(cfg, slaveTypeString.trim());
                        break;
                    case GOOGLECLOUD:
                        slaveType = new InstanceTypeGoogleCloud(slaveTypeString.trim());
                        break;
                }
                cfg.setSlaveInstanceType(slaveType);
                if (slaveType.getSpec().isClusterInstance() || cfg.getMasterInstanceType().getSpec().isClusterInstance()) {
                    if (!slaveType.toString().equals(cfg.getMasterInstanceType().toString())) {
                        LOG.warn("The instance types should be the same when using cluster types.");
                        LOG.warn("Master Instance Type: " + cfg.getMasterInstanceType().toString());
                        LOG.warn("Slave Instance Type: " + slaveType.toString());
                    }
                }
            } catch (Exception e) {
                LOG.error("Invalid slave instance type specified!");
                return false;
            }
            LOG.info(V, "Slave instance type set. ({})", this.cfg.getSlaveInstanceType());
        }
        return true;
    }

    private boolean parseSlaveInstanceMaxParameter(List<String> req, Properties defaults) {
        final String shortParam = RuleBuilder.RuleNames.SLAVE_INSTANCE_COUNT_S.toString();
        final String longParam = RuleBuilder.RuleNames.SLAVE_INSTANCE_COUNT_L.toString();
        if (req.contains(shortParam)) {
            try {
                if (defaults.containsKey(longParam)) {
                    this.cfg.setSlaveInstanceCount(Integer.parseInt(defaults.getProperty(longParam)));
                }
            } catch (NumberFormatException nfe) {
                LOG.error("Invalid property value for " + longParam + ". Please make sure you have a positive integer here.");
                return false;
            }
            if (this.cl.hasOption(shortParam)) {
                try {
                    int numSlaves = Integer.parseInt(this.cl.getOptionValue(shortParam));
                    if (numSlaves >= 0) {
                        this.cfg.setSlaveInstanceCount(numSlaves);
                    } else {
                        LOG.error("Number of slave nodes has to be at least 0.");
                    }
                } catch (NumberFormatException nfe) {
                    LOG.error("Invalid argument for -" + shortParam + ". Please make sure you have a positive integer here.");
                }
            }
            if (this.cfg.getSlaveInstanceCount() < 0) {
                LOG.error("-" + shortParam + " option is required! Please specify the number of slave nodes. (at least 0)");
                return false;
            } else {
                LOG.info(V, "Slave instance count set. ({})", this.cfg.getSlaveInstanceCount());
            }
        }
        return true;
    }

    private boolean parseMasterAsComputeParameter(List<String> req, Properties defaults) {
        final String shortParam = RuleBuilder.RuleNames.USE_MASTER_AS_COMPUTE_S.toString();
        if (req.contains(shortParam)) {
            ParseResult result = parseYesNoParameter(defaults, "Master as compute",
                    RuleBuilder.RuleNames.USE_MASTER_AS_COMPUTE_S, RuleBuilder.RuleNames.USE_MASTER_AS_COMPUTE_L);
            if (result.success) {
                this.cfg.setUseMasterAsCompute(result.getEnabled());
            }
            if (!result.success) {
                LOG.error("-" + shortParam + " option is required! Please make sure it is set as yes or no");
                return false;
            }
        }
        return true;
    }

    private boolean parseSlaveImageParameter(List<String> req, Properties defaults) {
        final String shortParam = RuleBuilder.RuleNames.SLAVE_IMAGE_S.toString();
        final String longParam = RuleBuilder.RuleNames.SLAVE_IMAGE_L.toString();
        if (req.contains(shortParam)) {
            this.cfg.setSlaveImage(this.cl.getOptionValue(shortParam, defaults.getProperty(longParam)));
            if (this.cfg.getSlaveImage() == null) {
                // try to load machine image id from URL
                loadMachineImageId();
                if (machineImage != null) {
                    cfg.setSlaveImage(machineImage.getProperty("slave"));
                } else {

                    LOG.error("-" + shortParam + " option is required! Please specify the AMI ID for your slave nodes.");
                    return false;
                }
            } else {
                LOG.info(V, "Slave image set. ({})", this.cfg.getSlaveImage());
            }
        }
        return true;
    }

    private boolean parseSecurityGroupParameter(Properties defaults) {
        final String shortParam = RuleBuilder.RuleNames.SECURITY_GROUP_S.toString();
        final String longParam = RuleBuilder.RuleNames.SECURITY_GROUP_L.toString();
        cfg.setSecuritygroup(cl.getOptionValue(shortParam, defaults.getProperty(longParam)));
        return true;
    }

    private boolean parsePortsParameter(Properties defaults) {
        final String shortParam = RuleBuilder.RuleNames.PORTS_S.toString();
        final String longParam = RuleBuilder.RuleNames.PORTS_L.toString();
        String portsCsv = this.cl.getOptionValue(shortParam, defaults.getProperty(longParam));
        if (portsCsv != null && !portsCsv.isEmpty()) {
            try {
                Pattern p = Pattern.compile("(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})(?:/(\\d{1,2}))*");
                String[] portsStrings = portsCsv.split(",");
                for (String portString : portsStrings) {
                    Port port;
                    // must distinguish between different notation
                    // 5555
                    // 0.0.0.0:5555
                    // 0.0.0.0/0:5555
                    // current:5555
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
                    cfg.getPorts().add(port);
                }
            } catch (Exception e) {
                LOG.error("Could not parse the supplied port list, please make "
                        + "sure you have a list of comma-separated valid ports "
                        + "without spaces in between. Valid ports have following pattern :"
                        + "[(current|'ip4v-address'|'ip4v-range/CIDR'):]'portnumber'", e);
                return false;
            }
            if (!this.cfg.getPorts().isEmpty()) {
                StringBuilder portsDisplay = new StringBuilder();
                for (Port port : cfg.getPorts()) {
                    portsDisplay.append(port.toString()).append(" ");
                }
                LOG.info(V, "Additional open ports set: {}", portsDisplay);
            }
        }
        return true;
    }

    private boolean parseExecuteScriptParameter(Properties defaults) {
        final String shortParam = RuleBuilder.RuleNames.EXECUTE_SCRIPT_S.toString();
        final String longParam = RuleBuilder.RuleNames.EXECUTE_SCRIPT_L.toString();
        String scriptFilePath = this.cl.hasOption(shortParam) || defaults.containsKey(longParam) ?
                this.cl.getOptionValue(shortParam, defaults.getProperty(longParam)) :
                null;
        if (scriptFilePath != null) {
            Path script = FileSystems.getDefault().getPath(scriptFilePath);
            if (!script.toFile().exists()) {
                LOG.error("The supplied shell script file '{}' does not exist!", script);
                return false;
            }
            this.cfg.setShellScriptFile(script);
            LOG.info(V, "Shell script file found! ({})", script);
        }
        return true;
    }

    private boolean parseMasterMountsParameter(Properties defaults) {
        final String shortParam = RuleBuilder.RuleNames.MASTER_MOUNTS_S.toString();
        final String longParam = RuleBuilder.RuleNames.MASTER_MOUNTS_L.toString();
        cfg.setMasterMounts(new HashMap<>());
        String masterMountsCsv = this.cl.getOptionValue(shortParam, defaults.getProperty(longParam));
        if (masterMountsCsv != null && !masterMountsCsv.isEmpty()) {
            try {
                String[] masterMounts = masterMountsCsv.split(",");
                for (String masterMountsKeyValue : masterMounts) {
                    String[] masterMountsSplit = masterMountsKeyValue.trim().split("=");
                    String snapshot = masterMountsSplit[0].trim();
                    String mountPoint = masterMountsSplit[1].trim();
                    this.cfg.getMasterMounts().put(snapshot, mountPoint);
                }
            } catch (Exception e) {
                LOG.error("Could not parse the list of master mounts, please make sure you have a list of " +
                        "comma-separated key=value pairs without spaces in between.");
                return false;
            }
            if (!this.cfg.getMasterMounts().isEmpty()) {
                StringBuilder masterMountsDisplay = new StringBuilder();
                for (Map.Entry<String, String> mount : this.cfg.getMasterMounts().entrySet()) {
                    masterMountsDisplay.append(mount.getKey());
                    masterMountsDisplay.append(" => ");
                    masterMountsDisplay.append(mount.getValue());
                    masterMountsDisplay.append(" ; ");
                }
                LOG.info(V, "Master mounts: {}", masterMountsDisplay);
            }
        }
        return true;
    }

    private boolean parseSlaveMountsParameter(Properties defaults) {
        final String shortParam = RuleBuilder.RuleNames.SLAVE_MOUNTS_S.toString();
        final String longParam = RuleBuilder.RuleNames.SLAVE_MOUNTS_L.toString();
        this.cfg.setSlaveMounts(new HashMap<>());
        String slaveMountsCsv = this.cl.getOptionValue(shortParam, defaults.getProperty(longParam));
        if (slaveMountsCsv != null && !slaveMountsCsv.isEmpty()) {
            try {
                String[] slaveMounts = slaveMountsCsv.split(",");
                for (String slaveMountsKeyValue : slaveMounts) {
                    String[] slaveMountsSplit = slaveMountsKeyValue.trim().split("=");
                    String snapshot = slaveMountsSplit[0].trim();
                    String mountPoint = slaveMountsSplit[1].trim();
                    this.cfg.getSlaveMounts().put(snapshot, mountPoint);
                }
            } catch (Exception e) {
                LOG.error("Could not parse the list of slave mounts, please make sure you have a list of " +
                        "comma-separated key=value pairs without spaces in between.");
                return false;
            }
            if (!this.cfg.getSlaveMounts().isEmpty()) {
                StringBuilder slaveMountsDisplay = new StringBuilder();
                for (Map.Entry<String, String> mount : this.cfg.getSlaveMounts().entrySet()) {
                    slaveMountsDisplay.append(mount.getKey());
                    slaveMountsDisplay.append(" => ");
                    slaveMountsDisplay.append(mount.getValue());
                    slaveMountsDisplay.append(" ; ");
                }
                LOG.info(V, "Slave mounts: {}", slaveMountsDisplay);
            }
        }
        return true;
    }

    private boolean parseMasterNfsSharesParameter(Properties defaults) {
        final String shortParam = RuleBuilder.RuleNames.NFS_SHARES_S.toString();
        final String longParam = RuleBuilder.RuleNames.NFS_SHARES_L.toString();
        this.cfg.setNfsShares(new ArrayList<>());
        String nfsSharesCsv = this.cl.getOptionValue(shortParam, defaults.getProperty(longParam));
        if (nfsSharesCsv != null && !nfsSharesCsv.isEmpty()) {
            try {
                String[] nfsSharesArray = nfsSharesCsv.split(",");
                for (String share : nfsSharesArray) {
                    this.cfg.getNfsShares().add(share.trim());
                }
            } catch (Exception e) {
                LOG.error("Could not parse the supplied list of shares, please make "
                        + "sure you have a list of comma-separated paths without spaces in between.");
                return false;
            }
            if (!this.cfg.getNfsShares().isEmpty()) {
                StringBuilder nfsSharesDisplay = new StringBuilder();
                for (String share : this.cfg.getNfsShares()) {
                    nfsSharesDisplay.append(share);
                    nfsSharesDisplay.append(" ");
                }
                LOG.info(V, "NFS shares set: {}", nfsSharesDisplay);
            }
        }
        return true;
    }

    private boolean parseExternNfsSharesParameter(Properties defaults) {
        final String shortParam = RuleBuilder.RuleNames.EXT_NFS_SHARES_S.toString();
        final String longParam = RuleBuilder.RuleNames.EXT_NFS_SHARES_L.toString();
        cfg.setExtNfsShares(new HashMap<>());
        String extNfsShareMap = cl.getOptionValue(shortParam, defaults.getProperty(longParam));
        if (extNfsShareMap != null && !extNfsShareMap.isEmpty()) {
            try {
                String[] tmp = extNfsShareMap.split(",");
                for (String share : tmp) {
                    String[] kv = share.split("=");
                    if (kv.length == 2) {
                        cfg.getExtNfsShares().put(kv[0], kv[1]);
                    } else {
                        throw new Exception();
                    }
                }
            } catch (Exception e) {
                LOG.error("Could not parse the supplied list of external shares, please make " +
                        "sure you have list of comma-separated nfsserver=path pairs without spaces in between.");
            }
        }
        return true;
    }

    private boolean parseEarlyExecuteScriptParameter(Properties defaults) {
        final String shortParam = RuleBuilder.RuleNames.EARLY_EXECUTE_SCRIPT_S.toString();
        final String longParam = RuleBuilder.RuleNames.EARLY_EXECUTE_SCRIPT_L.toString();
        String earlyMasterScriptFilePath = this.cl.hasOption(shortParam) || defaults.containsKey(longParam) ?
                this.cl.getOptionValue(shortParam, defaults.getProperty(longParam)) :
                null;
        if (earlyMasterScriptFilePath != null) {
            Path script = FileSystems.getDefault().getPath(earlyMasterScriptFilePath);
            if (!script.toFile().exists()) {
                LOG.error("The supplied early master shell script file '{}' does not exist!", script);
                return false;
            }
            cfg.setEarlyMasterShellScriptFile(script);
            LOG.info(V, "Early master shell script file found! ({})", script);
        }
        return true;
    }

    private boolean parseEarlySlaveExecuteScriptParameter(Properties defaults) {
        final String shortParam = RuleBuilder.RuleNames.EARLY_SLAVE_EXECUTE_SCRIPT_S.toString();
        final String longParam = RuleBuilder.RuleNames.EARLY_SLAVE_EXECUTE_SCRIPT_L.toString();
        String earlySlaveScriptFilePath = this.cl.hasOption(shortParam) || defaults.containsKey(longParam) ?
                this.cl.getOptionValue(shortParam, defaults.getProperty(longParam)) :
                null;
        if (earlySlaveScriptFilePath != null) {
            Path script = FileSystems.getDefault().getPath(earlySlaveScriptFilePath);
            if (!script.toFile().exists()) {
                LOG.error("The supplied early slave shell script file '{}' does not exist!", script);
                return false;
            }
            cfg.setEarlySlaveShellScriptFile(script);
            LOG.info(V, "Early slave shell script file found! ({})", script);
        }
        return true;
    }

    private boolean parsePublicSlaveIpParameter(Properties defaults) {
        final String shortParam = RuleBuilder.RuleNames.PUBLIC_SLAVE_IP_S.toString();
        final String longParam = RuleBuilder.RuleNames.PUBLIC_SLAVE_IP_L.toString();
        // public ip address for all slaves
        if (cl.hasOption(shortParam) || defaults.containsKey(longParam)) {
            cfg.setPublicSlaveIps(cl.getOptionValue(shortParam, defaults.getProperty(longParam)).equalsIgnoreCase(KEYWORD_YES));
        }
        return true;
    }

    private boolean parseSpotInstanceParameters(Properties defaults) {
        final String spotShortParam = RuleBuilder.RuleNames.USE_SPOT_INSTANCE_REQUEST_S.toString();
        final String spotLongParam = RuleBuilder.RuleNames.USE_SPOT_INSTANCE_REQUEST_L.toString();
        if (cl.hasOption(spotShortParam) || defaults.containsKey(spotLongParam)) {
            String value = cl.getOptionValue(spotShortParam, defaults.getProperty(spotLongParam));
            if (value.equalsIgnoreCase(KEYWORD_YES)) {
                cfg.setUseSpotInstances(true);
                if (cl.hasOption("bd") || defaults.containsKey("bidprice")) {
                    try {
                        cfg.setBidPrice(Double.parseDouble(cl.getOptionValue("bd", defaults.getProperty("bidprice"))));
                        if (cfg.getBidPrice() <= 0.0) {
                            throw new NumberFormatException();
                        }
                    } catch (NumberFormatException e) {
                        LOG.error("Argument bp/bidprice is not a valid double value  and must be > 0.0 !");
                        return false;
                    }
                } else {
                    LOG.error("If use-spot-instance-request is set, a bidprice must defined!");
                    return false;
                }
                if (cl.hasOption("bdm") || defaults.containsKey("bidprice-master")) {
                    try {
                        cfg.setBidPriceMaster(Double.parseDouble(cl.getOptionValue("bdm", defaults.getProperty("bidprice-master"))));
                        if (cfg.getBidPriceMaster() <= 0.0) {
                            throw new NumberFormatException();
                        }
                    } catch (NumberFormatException e) {
                        LOG.error("Argument bpm/bidprice-master is not a valid double value and must be > 0.0 !");
                        return false;
                    }
                } else {
                    LOG.info(V, "Bidprice master is not set, use general bidprice instead!");
                }
                LOG.info(V, "Use spot request for all");
            } else if (value.equalsIgnoreCase(KEYWORD_NO)) {
                LOG.info(V, "SpotInstance usage disabled.");
                this.cfg.setMesos(false);
            } else {
                LOG.error("SpotInstanceRequest value not recognized. Please use yes/no.");
                return false;
            }
        }
        return true;
    }

    private boolean parseEphemeralFilesystemParameter(Properties defaults) {
        final String shortParam = RuleBuilder.RuleNames.LOCAL_FS_S.toString();
        final String longParam = RuleBuilder.RuleNames.LOCAL_FS_L.toString();
        if (cl.hasOption(shortParam) || defaults.containsKey(longParam)) {
            String value = cl.getOptionValue(shortParam, defaults.getProperty(longParam));
            try {
                FS fs = FS.valueOf(value.toUpperCase());
                cfg.setLocalFS(fs);
            } catch (IllegalArgumentException e) {
                LOG.error("Local filesystem must be one of 'ext2', 'ext3', 'ext4' or 'xfs'!");
                return false;
            }
        }
        return true;
    }

    private void parseGridPropertiesFileParameter(Properties defaults) {
        final String shortParam = RuleBuilder.RuleNames.GRID_PROPERTIES_FILE_S.toString();
        final String longParam = RuleBuilder.RuleNames.GRID_PROPERTIES_FILE_L.toString();
        String gridPropertiesFile = this.cl.hasOption(shortParam) || defaults.containsKey(longParam) ?
                this.cl.getOptionValue(shortParam, defaults.getProperty(longParam)) :
                null;
        if (gridPropertiesFile != null) {
            Path prop = FileSystems.getDefault().getPath(gridPropertiesFile);
            if (prop.toFile().exists()) {
                LOG.warn("Overwrite an existing properties file '{}'!", prop);
            }
            this.cfg.setGridPropertiesFile(prop.toFile());
            LOG.info(V, "Wrote grid properties to '{}' after successful grid startup!", prop);
        }
    }

    private void parseDebugRequestsParameter(Properties defaults) {
        final String shortParam = RuleBuilder.RuleNames.DEBUG_REQUESTS_S.toString();
        final String longParam = RuleBuilder.RuleNames.DEBUG_REQUESTS_L.toString();
        if (cl.hasOption(shortParam)) {
            this.cfg.setLogHttpRequests(true);
        } else if (defaults.containsKey(longParam) && defaults.getProperty(longParam).equalsIgnoreCase(KEYWORD_YES)) {
            this.cfg.setLogHttpRequests(true);
        }
    }

    public boolean validate() {
        Properties defaults = this.loadDefaultsFromPropertiesFile();
        // if no mode aws given keep default mode instead.
        try {
            if (cl.hasOption(CMD_PARAMETER_MODE) || defaults.containsKey(CMD_PARAMETER_MODE)) {
                String mode = cl.getOptionValue(CMD_PARAMETER_MODE, defaults.getProperty(CMD_PARAMETER_MODE)).trim();
                cfg.setMode(MODE.valueOf(mode.toUpperCase()));
            }
        } catch (IllegalArgumentException iae) {
            LOG.error("No suitable mode found. Exit");
            return false;
        }
        List<String> req = intent.getRequiredOptions(cfg.getMode());
        if (!req.isEmpty()) {
            if (!parseTerminateParameter(req)) return false;
            if (!parseAwsCredentialsFileParameter(req, defaults)) return false;
            if (!parseGoogleCloudParameters(defaults)) return false;
            if (!parseOpenStackParameters(defaults)) return false;
            if (!parseUserNameParameter(defaults)) return false;
            if (!parseNetworkParameters(defaults)) return false;
            if (!parseMesosParameters(defaults)) return false;
            if (!parseGridEngineParameters(defaults)) return false;
            if (!parseNfsParameters(defaults)) return false;
            if (!parseCassandraParameters(defaults)) return false;
            if (!parseSparkParameters(defaults)) return false;
            if (!parseHdfsParameters(defaults)) return false;
            if (!parseIdentityFileParameter(req, defaults)) return false;
            if (!parseKeypairParameter(req, defaults)) return false;
            if (!parseRegionParameter(req, defaults)) return false;
            if (!parseAvailabilityZoneParameter(req, defaults)) return false;
            if (!parseMasterInstanceTypeParameter(req, defaults)) return false;
            if (!parseMasterImageParameter(req, defaults)) return false;
            if (!parseSlaveInstanceTypeParameter(req, defaults)) return false;
            if (!parseSlaveInstanceMaxParameter(req, defaults)) return false;
            if (!parseMasterAsComputeParameter(req, defaults)) return false;
            if (!parseSlaveImageParameter(req, defaults)) return false;
            if (!parseSecurityGroupParameter(defaults)) return false;
            if (!parsePortsParameter(defaults)) return false;
            if (!parseExecuteScriptParameter(defaults)) return false;
            if (!parseMasterMountsParameter(defaults)) return false;
            if (!parseSlaveMountsParameter(defaults)) return false;
            if (!parseMasterNfsSharesParameter(defaults)) return false;
            if (!parseExternNfsSharesParameter(defaults)) return false;
            if (!parseEarlyExecuteScriptParameter(defaults)) return false;
            if (!parseEarlySlaveExecuteScriptParameter(defaults)) return false;
            if (!parsePublicSlaveIpParameter(defaults)) return false;
            if (!parseSpotInstanceParameters(defaults)) return false;
            if (!parseEphemeralFilesystemParameter(defaults)) return false;
            parseGridPropertiesFileParameter(defaults);
            parseDebugRequestsParameter(defaults);
        }
        // if successful validated set configuration to intent
        intent.setConfiguration(cfg);
        return true;
    }

    private int checkStringAsInt(String s, int max) throws Exception {
        int v = Integer.parseInt(s);
        if (v < 0 || v > max) {
            throw new Exception();
        }
        return v;
    }

    private void loadMachineImageId() {
        machineImage = new Properties();
        String str = "https://bibiserv.cebitec.uni-bielefeld.de/resources/bibigrid/" +
                cfg.getMode().name().toLowerCase() + "/" + cfg.getRegion() + ".ami.properties";
        try {
            URL url = new URL(str);
            machineImage.load(url.openStream());
            if (!machineImage.containsKey("master")) {
                throw new IOException("Key/value for master image is missing in properties file!");
            }
            if (!machineImage.containsKey("slave")) {
                throw new IOException("Key/value for slave image is missing in properties file!");
            }
        } catch (IOException ex) {
            LOG.warn("No machine image properties file found for " + cfg.getMode().name() + " and region '" +
                    cfg.getRegion() + "' found!");
            LOG.error(V, "Exception: {}", ex.getMessage());
            LOG.error(V, "Try : {}", str);
            machineImage = null;
        }
    }
}
