package de.unibi.cebitec.bibigrid.ctrl;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import de.unibi.cebitec.bibigrid.meta.aws.InstanceTypeAWS;
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
import org.apache.commons.cli.CommandLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommandLineValidator {

    public static final String DEFAULT_PROPERTIES_DIRNAME = System.getProperty("user.home");
    public static final String DEFAULT_PROPERTIES_FILENAME = ".bibigrid.properties";
    public static final Logger log = LoggerFactory.getLogger(CommandLineValidator.class);
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
                this.propertiesFilePath = newPath;
                log.info("Alternative config file " + newPath.toString() + " will be used.");
                this.cfg.setAlternativeConfigFile(true);
                this.cfg.setAlternativeConfigPath(newPath.toString());
            }
        } else {
            this.propertiesFilePath = FileSystems.getDefault().getPath(DEFAULT_PROPERTIES_DIRNAME, DEFAULT_PROPERTIES_FILENAME);
        }
    }

    private Properties loadDefaultsFromPropertiesFile() {
        Properties defaultProperties = new Properties();
        try {
            defaultProperties.load(Files.newInputStream(this.propertiesFilePath));
        } catch (IOException e) {
            //nothing to do here, just return empty properties. validate() will catch that.
        }
        return defaultProperties;
    }

    public boolean validate() {
        Properties defaults = this.loadDefaultsFromPropertiesFile(); // load props

        String mode = "";
        try {
            if (cl.hasOption("mode")) { // if commandline has option mode
                mode = cl.getOptionValue("mode").trim();
                cfg.setMode(MODE.valueOf(mode.toUpperCase()));
            } else if (defaults.getProperty("mode") != null) { // if props has option mode
                mode = defaults.getProperty("mode");
                cfg.setMode(MODE.valueOf(mode.toUpperCase()));
            }
        } catch (IllegalArgumentException iae) {
            log.error("No suitable mode found. Exit");
            return false;
        }
        // if no mode aws given keep default mode instead.

        List<String> req = this.intent.getRequiredOptions(cfg.getMode());
        
        req = new ArrayList<>(req);

        if (!req.isEmpty()) {

            if (Files.exists(this.propertiesFilePath)) {
                log.info(V, "Reading default options from properties file at '{}'.", this.propertiesFilePath);
            } else {
                log.info("No properties file for default options found ({}). Using command line parameters only.", this.propertiesFilePath);
            }

            ////////////////////////////////////////////////////////////////////////
            ///// terminate (cluster-id) /////////////////////////////////////////////
            if (req.contains("t")) {
                this.cfg.setClusterId(this.cl.getOptionValue("t"));
            }

            ////////////////////////////////////////////////////////////////////////
            ///// aws-credentials-file /////////////////////////////////////////////
            if (req.contains("a")) {
                String awsCredentialsFilePath = null;
                if (defaults.containsKey("aws-credentials-file")) {
                    awsCredentialsFilePath = defaults.getProperty("aws-credentials-file");
                }
                if (this.cl.hasOption("a")) {
                    awsCredentialsFilePath = this.cl.getOptionValue("a");
                }
                if (awsCredentialsFilePath == null) {
                    if (Files.exists(this.propertiesFilePath)) {
                        awsCredentialsFilePath = this.propertiesFilePath.toString();
                    } else {
                        log.error("Default credentials file not found! ({})", this.propertiesFilePath);
                        log.error("-a option is required! Please specify the properties file containing the aws credentials.");
                        return false;
                    }
                }
                File credentialsFile = new File(awsCredentialsFilePath);
                try {
                    AWSCredentials keys = new PropertiesCredentials(credentialsFile);
                    this.cfg.setCredentials(keys);
                    log.info(V, "AWS-Credentials successfully loaded! ({})", awsCredentialsFilePath);
                } catch (IOException | IllegalArgumentException e) {
                    log.error("AWS-Credentials from properties: {}", e.getMessage());
                    return false;
                }
            }

            if (req.contains("u")) {
                if (cl.hasOption("u") || defaults.containsKey("user")) {
                    String value = cl.getOptionValue("u", defaults.getProperty("user"));
                    if (value != null && !value.isEmpty()) {
                        cfg.setUser(value);
                    } else {
                        log.error("User (-u) can't be null or empty.");
                        return false;
                    }

                } else {
                    log.error("-u option is required!");
                    return false;
                }
            }
            ////////////////////////////////////////////////////////////////////////
            ///// vpc-id ///////////////////////////////////////////////////////////

            if (this.cl.hasOption("vpc")) {
                this.cfg.setVpcid(this.cl.getOptionValue("vpc"));
            } else if (defaults.containsKey("vpc")) {
                cfg.setVpcid(defaults.getProperty("vpc"));
            }

            /**
             * Openstack meta area.
             */
            if (cfg.getMode().equals(MODE.OPENSTACK)) {

                OpenStackCredentials osc = new OpenStackCredentials();

                if (cl.hasOption("osu")) {
                    osc.setUsername(cl.getOptionValue("osu").trim());
                } else if (defaults.getProperty("openstack-username") != null) {
                    osc.setUsername(defaults.getProperty("openstack-username"));
                } else {
                    log.error("No suitable entry for OpenStack-Username (osu) found! Exit");
                    return false;
                }

                if (cl.hasOption("ost")) {
                    osc.setTenantName(cl.getOptionValue("ost").trim());
                } else if (defaults.getProperty("openstack-tenantname") != null) {
                    osc.setTenantName(defaults.getProperty("openstack-tenantname"));
                } else {
                    log.error("No suitable entry for OpenStack-Tenantname (ost) found! Exit");
                    return false;
                }

                if (cl.hasOption("osp")) {
                    osc.setPassword(cl.getOptionValue("osp").trim());
                } else if (defaults.getProperty("openstack-password") != null) {
                    osc.setPassword(defaults.getProperty("openstack-password"));
                } else {
                    log.error("No suitable entry for OpenStack-Password (osp) found! Exit");
                    return false;
                }

                if (cl.hasOption("ose")) {
                    osc.setEndpoint(cl.getOptionValue("ose").trim());
                } else if (defaults.getProperty("openstack-endpoint") != null) {
                    osc.setEndpoint(defaults.getProperty("openstack-endpoint"));
                } else {
                    log.error("No suitable entry for OpenStack-Endpoint (ose) found! Exit");
                    return false;
                }
                
                if (cl.hasOption("osd")) {
                    osc.setDomain(cl.getOptionValue("osd").trim());
                } else if (defaults.getProperty("openstack-domain") != null) {
                    osc.setDomain(defaults.getProperty("openstack-domain"));
                } else {
                    log.info("Keystone V2 API.");
                    // V2
                }
                
                this.cfg.setOpenstackCredentials(osc);
            }

           
            
            ////////////////////////////////////////////////////////////////////////
            ///// mesos on/off /////////////////////////////////////////////////
            if (this.cl.hasOption("me") || defaults.containsKey("mesos")) {
                String value = cl.getOptionValue("mesos", defaults.getProperty("mesos"));
                if (value.equalsIgnoreCase("yes")) {
                    this.cfg.setMesos(true);
                    log.info(V, "Mesos support enabled.");
                } else if (value.equalsIgnoreCase("no")) {
                    log.info(V, "Mesos support disabled.");
                    this.cfg.setMesos(false);
                } else {
                    log.error("Mesos value not recognized. Please use yes/no.");
                    return false;
                }
            }

            ////////////////////////////////////////////////////////////////////////
            ///// OGE on/off /////////////////////////////////////////////////
            if (this.cl.hasOption("oge") || defaults.containsKey("oge")) {
                String value = cl.getOptionValue("oge", defaults.getProperty("oge"));
                if (value.equalsIgnoreCase("yes")) {
                    cfg.setOge(true);
                    log.info(V, "OpenGridEngine support enabled.");
                } else if (value.equalsIgnoreCase("no")) {
                    log.info(V, "OpenGridEngine support disabled.");
                    cfg.setOge(false);
                } else {
                    log.error("OpenGridEngine value not recognized. Please use yes/no.");
                    return false;
                }
            }
            ////////////////////////////////////////////////////////////////////////
            ///// NFS on/off /////////////////////////////////////////////////
            if (this.cl.hasOption("nfs") || defaults.containsKey("nfs")) {
                String value = cl.getOptionValue("nfs", defaults.getProperty("nfs"));
                if (value.equalsIgnoreCase("yes")) {
                    this.cfg.setNfs(true);
                    log.info(V, "NFS enabled.");
                } else if (value.equalsIgnoreCase("no")) {
                    log.info(V, "NFS disabled.");
                    this.cfg.setNfs(false);
                } else {
                    log.error("NFS value not recognized. Please use yes/no.");
                    return false;
                }
            }

            ////////////////////////////////////////////////////////////////////////
            ///// cassandra on/off /////////////////////////////////////////////////
            if (cl.hasOption("db")) {
                cfg.setCassandra(true);
                log.info(V, "Cassandra support enabled.");
            } else if (defaults.containsKey("cassandra")) {
                String value = defaults.getProperty("cassandra");
                if (value.equalsIgnoreCase("yes")) {
                    cfg.setCassandra(true);
                    log.info(V, "Cassandra support enabled.");
                } else if (value.equalsIgnoreCase("no")) {
                    log.info(V, "Cassandra support disabled.");
                    this.cfg.setCassandra(false);
                } else {
                    log.error("Cassandra value in properties not recognized. Please use yes/no.");
                    return false;
                }
            }

            ////////////////////////////////////////////////////////////////////////
            ///// identity-file ////////////////////////////////////////////////////
            if (req.contains("i")) {
                String identityFilePath = null;
                if (defaults.containsKey("identity-file")) {
                    identityFilePath = defaults.getProperty("identity-file");
                }
                if (this.cl.hasOption("i")) {
                    identityFilePath = this.cl.getOptionValue("i");
                }
                if (identityFilePath == null) {
                    log.error("-i option is required! Please specify the absolute path to your identity file (private ssh key file).");
                    return false;
                }
                Path identity = FileSystems.getDefault().getPath(identityFilePath);
                if (!identity.toFile().exists()) {
                    log.error("Identity private key file '{}' does not exist!", identity);
                    return false;
                }
                this.cfg.setIdentityFile(identity);
                log.info(V, "Identity file found! ({})", identity);
            }

            ////////////////////////////////////////////////////////////////////////
            ///// keypair //////////////////////////////////////////////////////////
            if (req.contains("k")) {
                this.cfg.setKeypair(this.cl.getOptionValue("k", defaults.getProperty("keypair")));
                if (this.cfg.getKeypair() == null) {
                    log.error("-k option is required! Please specify the name of your keypair.");
                    return false;
                }
                log.info(V, "Keypair name set. ({})", this.cfg.getKeypair());
            }

            ////////////////////////////////////////////////////////////////////////
            ///// Region /////////////////////////////////////////////////////////
            if (req.contains("e")) {
                this.cfg.setRegion(this.cl.getOptionValue("e", defaults.getProperty("region")));
                if (this.cfg.getRegion() == null) {
                    log.error("-e option is required! Please specify the url of your region "
                            + "(e.g. region=eu-west-1).");
                    return false;
                }
                log.info(V, "Region set. ({})", this.cfg.getRegion());
            }
            
            ////////////////////////////////////////////////////////////////////////
            ///// availability-zone ////////////////////////////////////////////////
            if (req.contains("z")) {
                this.cfg.setAvailabilityZone(this.cl.getOptionValue("z", defaults.getProperty("availability-zone")));
                if (this.cfg.getAvailabilityZone() == null) {
                    log.error("-z option is required! Please specify an availability zone "
                            + "(e.g. availability-zone=eu-west-1a).");
                    return false;
                }
                log.info(V, "Availability zone set. ({})", this.cfg.getAvailabilityZone());
            }

            ////////////////////////////////////////////////////////////////////////
            ///// master-instance-type /////////////////////////////////////////////
            if (req.contains("m")) {
                try {
                    String masterTypeString = this.cl.getOptionValue("m", defaults.getProperty("master-instance-type"));
                    if (masterTypeString == null) {
                        log.error("-m option is required! Please specify the instance type of your master node. (e.g. master-instance-type=t1.micro)");
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
                    }
                    this.cfg.setMasterInstanceType(masterType);
                } catch (Exception e) {
                    log.error("Invalid master instance type specified!",e);
                    return false;
                }
                log.info(V, "Master instance type set. ({})", this.cfg.getMasterInstanceType());
            }

            ////////////////////////////////////////////////////////////////////////
            ///// master-image /////////////////////////////////////////////
            if (req.contains("M")) {
                cfg.setMasterImage(cl.getOptionValue("M", defaults.getProperty("master-image")));
                if (cfg.getMasterImage() == null) {
                    // try to load machine image id from URL
                    loadMachineImageId();
                    if (machineImage != null ){
                        cfg.setMasterImage(machineImage.getProperty("master"));
                    } else {
                        log.error("-M option is required! Please specify the AMI ID for your master node.");
                        return false;
                    }
                }
                log.info(V, "Master image set. ({})", cfg.getMasterImage());
            }
            
            ////////////////////////////////////////////////////////////////////////
            ///// slave-instance-type //////////////////////////////////////////////
            if (req.contains("s")) {
                try {
                    String slaveTypeString = this.cl.getOptionValue("s", defaults.getProperty("slave-instance-type"));
                    if (slaveTypeString == null) {
                        log.error("-s option is required! Please specify the instance type of your slave nodes(slave-instance-type=t1.micro).");
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
                    }

                    cfg.setSlaveInstanceType(slaveType);
                    if (slaveType.getSpec().clusterInstance || cfg.getMasterInstanceType().getSpec().clusterInstance) {
                        if (!slaveType.toString().equals(cfg.getMasterInstanceType().toString())) {
                            log.warn("The instance types should be the same when using cluster types.");
                            log.warn("Master Instance Type: " + cfg.getMasterInstanceType().toString());
                            log.warn("Slave Instance Type: " + slaveType.toString());
                            
                           
                        }
                    }
                } catch (Exception e) {
                    log.error("Invalid slave instance type specified!");
                    return false;
                }
                log.info(V, "Slave instance type set. ({})", this.cfg.getSlaveInstanceType());
            }

            ////////////////////////////////////////////////////////////////////////
            ///// slave-instance-max /////////////////////////////////////////////
            if (req.contains("n")) {
                try {
                    if (defaults.containsKey("slave-instance-count")) {
                        this.cfg.setSlaveInstanceCount(Integer.parseInt(defaults.getProperty("slave-instance-count")));
                    }
                } catch (NumberFormatException nfe) {
                    log.error("Invalid property value for slave-instance-max. Please make sure you have a positive integer here.");
                    return false;
                }
                if (this.cl.hasOption("n")) {
                    try {
                        int numSlaves = Integer.parseInt(this.cl.getOptionValue("n"));
                        if (numSlaves >= 0) {
                            this.cfg.setSlaveInstanceCount(numSlaves);
                        } else {
                            log.error("Number of slave nodes has to be at least 0.");
                        }
                    } catch (NumberFormatException nfe) {
                        log.error("Invalid argument for -n. Please make sure you have a positive integer here.");
                    }
                }
                if (this.cfg.getSlaveInstanceCount() < 0) {
                    log.error("-n option is required! Please specify the number of slave nodes. (at least 0)");
                    return false;
                } else {
                    log.info(V, "Slave instance count set. ({})", this.cfg.getSlaveInstanceCount());
                }
            }

            ///////////////////////////////////////////////////////////////////////
            ///////////////////// use master as compute yes or no//////////////////////////
            if (req.contains("b")) {
                boolean valueSet = false;
                try {
                    if (defaults.containsKey("use-master-as-compute")) {
                        String value = defaults.getProperty("use-master-as-compute");
                        if (value.equalsIgnoreCase("yes")) {
                            this.cfg.setUseMasterAsCompute(true);
                            valueSet = true;

                        } else if (value.equalsIgnoreCase("no")) {
                            this.cfg.setUseMasterAsCompute(false);
                            valueSet = true;
                        } else {
                            throw new IllegalArgumentException("Value not yes or no");
                        }
                    }
                } catch (IllegalArgumentException iae) {
                    log.error("Invalid value for use-master-as-compute. Please make sure it is set as yes or no.");
                }
                if (this.cl.hasOption("b")) {
                    try {
                        String value = defaults.getProperty("use-master-as-compute");
                        if (value.equalsIgnoreCase("yes")) {
                            this.cfg.setUseMasterAsCompute(true);
                            valueSet = true;

                        } else if (value.equalsIgnoreCase("no")) {
                            this.cfg.setUseMasterAsCompute(false);
                            valueSet = true;
                        } else {
                            throw new IllegalArgumentException("Value not yes or no");
                        }
                    } catch (IllegalArgumentException iae) {
                        log.error("Invalid value for use-master-as-compute. Please make sure it is set as yes or no.");
                    }
                }
                if (!valueSet) {
                    log.error("-b option is required! Please make sure it is set as yes or no");
                    return false;
                } else {
                    log.info(V, "Use Master as compute instance. ({})", this.cfg.isUseMasterAsCompute());
                }
            }
            ////////////////////////////////////////////////////////////////////////
            ///// slave-image //////////////////////////////////////////////////////

            if (req.contains("S")) {
                this.cfg.setSlaveImage(this.cl.getOptionValue("S", defaults.getProperty("slave-image")));
                if (this.cfg.getSlaveImage() == null) {
                     // try to load machine image id from URL
                    loadMachineImageId();
                    if (machineImage != null ){
                        cfg.setSlaveImage(machineImage.getProperty("slave"));
                    } else {
                       
                        log.error("-S option is required! Please specify the AMI ID for your slave nodes.");
                    return false;
                    }
                } else {
                    log.info(V, "Slave image set. ({})", this.cfg.getSlaveImage());
                }
            }
            
            ////////////////////////////////////////////////////////////////////////
            ///// ports ////////////////////////////////////////////////////////////
            this.cfg.setPorts(new ArrayList<>());
            String portsCsv = this.cl.getOptionValue("p", defaults.getProperty("ports"));
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
                            String addr;
                            String[] tmp = portString.split(":");
                            if (tmp[0].equalsIgnoreCase("current")) {
                                addr = InetAddress.getLocalHost().getHostAddress() + "/32";
                            } else {
                                Matcher m = p.matcher(tmp[0]);
                                
                                boolean matches = m.matches();
                                
                                for (int i = 1; i <= 4; i++) {
                                    checkStringAsInt(m.group(i), 0, 255);
                                }
                                if (m.group(5) != null) {
                                    checkStringAsInt(m.group(5), 0, 32);
                                }
                                addr = tmp[0];
                            }
                            port = new Port(addr, checkStringAsInt(tmp[1], 0, 65535));
                        } else {
                            port = new Port("0.0.0.0/0", checkStringAsInt(portString.trim(), 0, 65535));

                        }
                        cfg.getPorts().add(port);
                    }
                } catch (Exception e) {
                    log.error("Could not parse the supplied port list, please make "
                            + "sure you have a list of comma-separated valid ports "
                            + "without spaces in between. Valid ports have following pattern :"
                            + "[(current|'ip4v-address'|'ip4v-range/CIDR'):]'portnumber'", e);
                    return false;
                }
                if (!this.cfg.getPorts().isEmpty()) {
                    StringBuilder portsDisplay = new StringBuilder();
                    for (Port port : cfg.getPorts()) {
                        portsDisplay.append(port.iprange).append(":").append(port.number);
                        portsDisplay.append(" ");
                    }
                    log.info(V, "Additional open ports set: {}", portsDisplay);
                }
            }

            ////////////////////////////////////////////////////////////////////////
            ///// execute-script ///////////////////////////////////////////////////
            String scriptFilePath = null;
            if (defaults.containsKey("execute-script")) {
                scriptFilePath = defaults.getProperty("execute-script");
            }
            if (this.cl.hasOption("x")) {
                scriptFilePath = this.cl.getOptionValue("x");
            }
            if (scriptFilePath != null) {
                Path script = FileSystems.getDefault().getPath(scriptFilePath);
                if (!script.toFile().exists()) {
                    log.error("The supplied shell script file '{}' does not exist!", script);
                    return false;
                }
                this.cfg.setShellScriptFile(script);
                log.info(V, "Shell script file found! ({})", script);
            }

            ////////////////////////////////////////////////////////////////////////
            ///// master-mounts ////////////////////////////////////////////////////
            this.cfg.setMasterMounts(new HashMap<String, String>());
            String masterMountsCsv = this.cl.getOptionValue("d", defaults.getProperty("master-mounts"));
            if (masterMountsCsv != null && !masterMountsCsv.isEmpty()) {
                try {
                    String[] masterMounts = masterMountsCsv.split(",");
                    for (String masterMountsKeyValue : masterMounts) {
                        String[] masterMountsSplit = masterMountsKeyValue.trim().split("=");
                        String snapshot = masterMountsSplit[0].trim();
                        String mountpoint = masterMountsSplit[1].trim();
                        this.cfg.getMasterMounts().put(snapshot, mountpoint);
                    }
                } catch (Exception e) {
                    log.error("Could not parse the list of master mounts, please make "
                            + "sure you have a list of comma-separated key=value pairs without spaces in between.");
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
                    log.info(V, "Master mounts: {}", masterMountsDisplay);
                }
            }

            ////////////////////////////////////////////////////////////////////////
            ///// slave-mounts /////////////////////////////////////////////////////
            this.cfg.setSlaveMounts(new HashMap<>());
            String slaveMountsCsv = this.cl.getOptionValue("f", defaults.getProperty("slave-mounts"));
            if (slaveMountsCsv != null && !slaveMountsCsv.isEmpty()) {
                try {
                    String[] slaveMounts = slaveMountsCsv.split(",");
                    for (String slaveMountsKeyValue : slaveMounts) {
                        String[] slaveMountsSplit = slaveMountsKeyValue.trim().split("=");
                        String snapshot = slaveMountsSplit[0].trim();
                        String mountpoint = slaveMountsSplit[1].trim();
                        this.cfg.getSlaveMounts().put(snapshot, mountpoint);
                    }
                } catch (Exception e) {
                    log.error("Could not parse the list of slave mounts, please make "
                            + "sure you have a list of comma-separated key=value pairs without spaces in between.");
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
                    log.info(V, "Slave mounts: {}", slaveMountsDisplay);
                }
            }

            ////////////////////////////////////////////////////////////////////////
            ///// master-nfs-shares ////////////////////////////////////////////////
            this.cfg.setNfsShares(new ArrayList<>());
            String nfsSharesCsv = this.cl.getOptionValue("g", defaults.getProperty("nfs-shares"));
            if (nfsSharesCsv != null && !nfsSharesCsv.isEmpty()) {
                try {
                    String[] nfsSharesArray = nfsSharesCsv.split(",");
                    for (String share : nfsSharesArray) {
                        this.cfg.getNfsShares().add(share.trim());
                    }
                } catch (Exception e) {
                    log.error("Could not parse the supplied list of shares, please make "
                            + "sure you have a list of comma-separated paths without spaces in between.");
                    return false;
                }
                if (!this.cfg.getNfsShares().isEmpty()) {
                    StringBuilder nfsSharesDisplay = new StringBuilder();
                    for (String share : this.cfg.getNfsShares()) {
                        nfsSharesDisplay.append(share);
                        nfsSharesDisplay.append(" ");
                    }
                    log.info(V, "NFS shares set: {}", nfsSharesDisplay);
                }
            }

            /* ------------------------ early shell script Master ------------------------- */
            String earlyMasterScriptFilePath = null;
            if (defaults.containsKey("early-execute-script")) {
                earlyMasterScriptFilePath = defaults.getProperty("early-execute-script");
            }
            if (this.cl.hasOption("ex")) {
                earlyMasterScriptFilePath = this.cl.getOptionValue("ex");
            }
            if (earlyMasterScriptFilePath != null) {
                Path script = FileSystems.getDefault().getPath(earlyMasterScriptFilePath);
                if (!script.toFile().exists()) {
                    log.error("The supplied early master shell script file '{}' does not exist!", script);
                    return false;
                }
                cfg.setEarlyMasterShellScriptFile(script);
                log.info(V, "Early master shell script file found! ({})", script);
            }
            
            
            /* ------------------------ early shell script Master ------------------------- */
            String earlySlaveScriptFilePath = null;
            if (defaults.containsKey("early-slave-execute-script")) {
                earlySlaveScriptFilePath = defaults.getProperty("early-slave-execute-script");
            }
            if (this.cl.hasOption("esx")) {
                earlySlaveScriptFilePath = this.cl.getOptionValue("esx");
            }
            if (earlySlaveScriptFilePath != null) {
                Path script = FileSystems.getDefault().getPath(earlySlaveScriptFilePath);
                if (!script.toFile().exists()) {
                    log.error("The supplied early slave shell script file '{}' does not exist!", script);
                    return false;
                }
                cfg.setEarlySlaveShellScriptFile(script);
                log.info(V, "Early slave shell script file found! ({})", script);
            }
            
            
            
            /* ------------------------ public ip address for all slaves ------------------------- */
            if (cl.hasOption("psi") || defaults.containsKey("public-slave-ip")){
                cfg.setPublicSlaveIps(cl.getOptionValue("psi", defaults.getProperty("public-slave-ip")).equalsIgnoreCase("yes"));
            }
            
            /* ------------------------- spot instance request --------------------------- */
            if (cl.hasOption("usir") || defaults.containsKey("use-spot-instance-request")) {
                String value = cl.getOptionValue("usir", defaults.getProperty("use-spot-instance-request"));
                if (value.equalsIgnoreCase("yes")) {
                    cfg.setUseSpotInstances(true);
                    
                    if (cl.hasOption("bd") || defaults.containsKey("bidprice")) {
                        try {
                            cfg.setBidPrice(Double.parseDouble(cl.getOptionValue("bd", defaults.getProperty("bidprice"))));
                            if (cfg.getBidPrice() <= 0.0) {
                                throw new NumberFormatException();
                            }
                        } catch (NumberFormatException e){
                            log.error("Argument bp/bidprice is not a valid double value  and must be > 0.0 !");
                            return false;
                        }                     
                    } else {
                        log.error("If use-spot-instance-request is set, a bidprice must defined!");
                        return false;
                    }
                    
                    if (cl.hasOption("bdm") || defaults.containsKey("bidprice-master")) {
                        try{
                            cfg.setBidPriceMaster(Double.parseDouble(cl.getOptionValue("bdm",defaults.getProperty("bidprice-master"))));
                            if (cfg.getBidPriceMaster() <= 0.0) {
                                throw new NumberFormatException();
                            }
                        } catch (NumberFormatException e) {
                            log.error("Argument bpm/bidprice-master is not a valid double value and must be > 0.0 !");
                            return false;
                        }
                    } else {
                        log.info(V,"Bidprice master is not set, use general bidprice instead!");
                    }
                              
                    log.info(V, "Use spot request for all");
                } else if (value.equalsIgnoreCase("no")) {
                    log.info(V, "SpotInstance ussage disabled.");
                    this.cfg.setMesos(false);
                } else {
                    log.error("SpotInstanceRequest value not recognized. Please use yes/no.");
                    return false;
                }
            }
            
            /* ----------------------- ephemeral fs ----------------------------- */
            if (cl.hasOption("lfs") || defaults.containsKey("local-fs")) {
                String value = cl.getOptionValue("lfs",defaults.getProperty("local-fs"));
                try {
                    FS fs  = FS.valueOf(value.toUpperCase());
                    cfg.setLocalFS(fs);
                } catch (IllegalArgumentException e) {
                    log.error("Local filesustem must be one of 'ext2', 'ext3', 'ext4' or 'xfs'!");
                    return false;
                }
                
            
                        
            }

            ///////////////////////////////////////////////////////////////////////
            ///////////////////// grid-properties-file ////////////////////////////
            String gridpropertiesfile = null;
            if (defaults.containsKey("grid-properties-file")) {
                gridpropertiesfile = defaults.getProperty("early-execute-script");
            }
            if (this.cl.hasOption("gpf")) {
                gridpropertiesfile = this.cl.getOptionValue("gpf");
            }
            if (gridpropertiesfile != null) {
                Path prop = FileSystems.getDefault().getPath(gridpropertiesfile);
                if (prop.toFile().exists()) {
                    log.warn("Overwrite an existing properties file '{}'!", prop);

                }
                this.cfg.setGridPropertiesFile(prop.toFile());
                log.info(V, "Wrote grid properties to '{}' after successful grid startup!", prop);
            }
            
            ////////////////////////////////////////////////////////////////////////
            ///// debug-requests ///////////////////////////////////////////////////
            if (this.cl.hasOption("dr")) {
                this.cfg.setLogHttpRequests(true);
            }

        }

        this.intent.setConfiguration(this.cfg);
        return true;
    }

    public Configuration getCfg() {
        return this.cfg;
    }

    private int checkStringAsInt(String s, int min, int max) throws Exception {
        int v = Integer.parseInt(s);
        if (v < min || v > max) {
            throw new Exception();
        }
        return v;
    }

    private void loadMachineImageId(){
        machineImage = new Properties();
        String str = "https://bibiserv.cebitec.uni-bielefeld.de/resources/bibigrid/"+cfg.getMode().name().toLowerCase()+"/"+cfg.getRegion()+".ami.properties";
        try {
            
            URL url = new URL("https://bibiserv.cebitec.uni-bielefeld.de/resources/bibigrid/"+cfg.getMode().name().toLowerCase()+"/"+cfg.getRegion()+".ami.properties");
            machineImage.load(url.openStream());
            
            if (!machineImage.containsKey("master")) {
                throw new IOException("Key/value for master image is missing in properties file!");
            }
            if (!machineImage.containsKey("slave")) {
                throw new IOException("Key/value for slave image is missing in properties file!");
            }
        } catch (IOException ex) {
            log.warn("No machine image properties file found for "+cfg.getMode().name()+" and region '"+cfg.getRegion()+"' found!");
            log.error(V,"Exception: {}",ex.getMessage());
            log.error(V,"Try : {}",str);
            machineImage = null;
        }

    }
    
}
