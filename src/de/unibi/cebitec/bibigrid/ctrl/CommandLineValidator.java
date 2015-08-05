package de.unibi.cebitec.bibigrid.ctrl;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.ec2.model.InstanceType;
import de.unibi.cebitec.bibigrid.model.Configuration;
import de.unibi.cebitec.bibigrid.util.InstanceInformation;
import static de.unibi.cebitec.bibigrid.util.VerboseOutputFilter.V;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.apache.commons.cli.CommandLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommandLineValidator {

    public static final String DEFAULT_PROPERTIES_DIRNAME = System.getProperty("user.home");
    public static final String DEFAULT_PROPERTIES_FILENAME = ".bibigrid.properties";
    public static final Logger log = LoggerFactory.getLogger(CommandLineValidator.class);
    private CommandLine cl;
    private Configuration cfg;
    private Intent intent;
    private Path propertiesFilePath;

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
        List<String> req = this.intent.getRequiredOptions();

        if (!req.isEmpty()) {
            Properties defaults = this.loadDefaultsFromPropertiesFile();
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
            ////////////////////////////////////////////////////////////////////////
            ///// vpc-id ///////////////////////////////////////////////////////////
            
            if (this.cl.hasOption("vpc")) {
                this.cfg.setVpcid(this.cl.getOptionValue("vpc", defaults.getProperty("vpc-id")));
            }
            
            
            ////////////////////////////////////////////////////////////////////////
            ///// autoscale on/off /////////////////////////////////////////////////

            if (this.cl.hasOption("j")) {
                this.cfg.setAutoscaling(true);
                log.info(V, "AutoScaling enabled.");
            } else if (defaults.containsKey("autoscaling")) {
                String value = defaults.getProperty("autoscaling");
                if (value.equalsIgnoreCase("yes")) {
                    this.cfg.setAutoscaling(true);
                    log.info(V, "AutoScaling enabled.");
                } else if (value.equalsIgnoreCase("no")) {
                    log.info(V, "AutoScaling disabled.");
                    this.cfg.setAutoscaling(false);
                } else {
                    log.error("AutoScaling value in properties not recognized. Please use yes/no.");
                    return false;
                }
            }

            ////////////////////////////////////////////////////////////////////////
            ///// cassandra on/off /////////////////////////////////////////////////
            if (this.cl.hasOption("db")) {
                this.cfg.setCassandra(true);
                log.info(V, "Cassandra support enabled.");
            } else if (defaults.containsKey("cassandra")) {
                String value = defaults.getProperty("cassandra");
                if (value.equalsIgnoreCase("yes")) {
                    this.cfg.setCassandra(true);
                    log.info(V, "Cassandra support enabled.");
                } else if (value.equalsIgnoreCase("no")) {
                    log.info(V, "Cassandra support disabled.");
                    this.cfg.setCassandra(false);
                } else {
                    log.error("Cassandra value in properties not recognized. Please use yes/no.");
                    return false;
                }
            }

            if (this.cfg.isCassandra() && this.cfg.isAutoscaling()) {
                log.error("Cassandra and autoscaling are not available at the same time.");
                return false;
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

                    InstanceType masterType = InstanceType.fromValue(masterTypeString.trim());
               
//                    if (!checkInstances(masterType)){
//                        return false;
//                    }
                    this.cfg.setMasterInstanceType(masterType);
                } catch (Exception e) {
                    log.error("Invalid master instance type specified!");
                    return false;
                }
                log.info(V, "Master instance type set. ({})", this.cfg.getMasterInstanceType());
            }

            ////////////////////////////////////////////////////////////////////////
            ///// master-image /////////////////////////////////////////////
            if (req.contains("M")) {
                this.cfg.setMasterImage(this.cl.getOptionValue("M", defaults.getProperty("master-image")).trim());
                if (this.cfg.getMasterImage() == null) {
                    log.error("-M option is required! Please specify the AMI ID for your master node.");
                    return false;
                }
                log.info(V, "Master image set. ({})", this.cfg.getMasterImage());
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
                    InstanceType slaveType = InstanceType.fromValue(slaveTypeString.trim());
//                    if (!checkInstances(slaveType)){
//                        return false;
//                    }
                    this.cfg.setSlaveInstanceType(slaveType);
                    if (InstanceInformation.getSpecs(slaveType).clusterInstance || InstanceInformation.getSpecs(this.cfg.getMasterInstanceType()).clusterInstance) {
                        if (!slaveType.equals(this.cfg.getMasterInstanceType())) {
                            log.error("The instance types have to be the same when using cluster types.");
                            log.error("Master Instance Type: " + this.cfg.getMasterInstanceType().toString());
                            log.error("Slave Instance Type: " + slaveType.toString());
                            return false;
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
                    if (defaults.containsKey("slave-instance-max")) {
                        this.cfg.setSlaveInstanceMaximum(Integer.parseInt(defaults.getProperty("slave-instance-max")));
                    }
                } catch (NumberFormatException nfe) {
                    log.error("Invalid property value for slave-instance-max. Please make sure you have a positive integer here.");
                    return false;
                }
                if (this.cl.hasOption("n")) {
                    try {
                        int numSlaves = Integer.parseInt(this.cl.getOptionValue("n"));
                        if (numSlaves >= 0) {
                            this.cfg.setSlaveInstanceMaximum(numSlaves);
                        } else {
                            log.error("Number of slave nodes has to be at least 0.");
                        }
                    } catch (NumberFormatException nfe) {
                        log.error("Invalid argument for -n. Please make sure you have a positive integer here.");
                    }
                }
                if (this.cfg.getSlaveInstanceMaximum() < 0) {
                    log.error("-n option is required! Please specify the number of slave nodes. (at least 0)");
                    return false;
                } else {
                    log.info(V, "Slave instance count set. ({})", this.cfg.getSlaveInstanceMaximum());
                }
            }

            ///////////////////////////////////////////////////////////////////////
            ///////////////////// slave instance minimum //////////////////////////
            if (req.contains("u")) {
                try {
                    if (defaults.containsKey("slave-instance-min")) {
                        this.cfg.setSlaveInstanceMinimum(Integer.parseInt(defaults.getProperty("slave-instance-min")));
                        if (this.cfg.getSlaveInstanceMaximum() < this.cfg.getSlaveInstanceMinimum()) {
                            throw new NumberFormatException();
                        }
                    }
                } catch (NumberFormatException nfe) {
                    log.error("Invalid property value for slave-instance-min. Please make sure you have a "
                            + "positive integer and your slave instance maximum is bigger or equal to your minimum amount.");
                    return false;
                }
                if (this.cl.hasOption("u")) {
                    try {
                        int numSlaves = Integer.parseInt(this.cl.getOptionValue("u"));
                        if (numSlaves >= 0) {
                            this.cfg.setSlaveInstanceMinimum(numSlaves);
                            if (this.cfg.getSlaveInstanceMaximum() < this.cfg.getSlaveInstanceMinimum()) {
                                throw new NumberFormatException();
                            }
                        } else {
                            log.error("Number of slave nodes has to be at least 0.");
                        }
                    } catch (NumberFormatException nfe) {
                        log.error("Invalid argument for -n. Please make sure you have a positive integer here "
                                + "and your slave instance maximum is bigger or equal to your minimum amount.");
                        return false;
                    }
                }
                if (this.cfg.getSlaveInstanceMinimum() < 0) {
                    log.error("-n option is required! Please specify the number of slave nodes. (at least 0)");
                    return false;
                } else {
                    log.info(V, "Slave instance minimum set. ({})", this.cfg.getSlaveInstanceMinimum());
                }
            }
            ///////////////////////////////////////////////////////////////////////
            ///////////////////// slave instance desired amount//////////////////////////
            if (req.contains("r")) {
                try {
                    if (defaults.containsKey("slave-instance-start")) {
                        this.cfg.setSlaveInstanceStartAmount(Integer.parseInt(defaults.getProperty("slave-instance-start")));
                        if (this.cfg.getSlaveInstanceStartAmount() < this.cfg.getSlaveInstanceMinimum() || this.cfg.getSlaveInstanceStartAmount() > this.cfg.getSlaveInstanceMaximum()) {
                            throw new NumberFormatException();
                        }
                    }
                } catch (NumberFormatException nfe) {
                    log.error("Invalid property value for slave-instance-start. Please make sure you have a "
                            + "positive integer and your slave instance maximum is bigger and slave instance minimum is equal or smaller to it.");
                    return false;
                }
                if (this.cl.hasOption("r")) {
                    try {
                        int numSlaves = Integer.parseInt(this.cl.getOptionValue("r"));
                        if (numSlaves >= 1) {
                            this.cfg.setSlaveInstanceStartAmount(numSlaves);
                            if (this.cfg.getSlaveInstanceStartAmount() < this.cfg.getSlaveInstanceMinimum() || this.cfg.getSlaveInstanceStartAmount() > this.cfg.getSlaveInstanceMaximum()) {
                                throw new NumberFormatException();
                            }
                        } else {
                            log.error("Number of start slave nodes has to be at least 1.");
                        }
                    } catch (NumberFormatException nfe) {
                        log.error("Invalid property value for slave-instance-start. Please make sure you have a "
                                + "positive integer and your slave instance maximum is bigger and the slave instance minimum is equal or smaller to it.");
                        return false;
                    }
                }
                if (this.cfg.getSlaveInstanceStartAmount() <= 0) {
                    log.error("-r option is required! Please specify the number of desired slave nodes at start up. (at least 1)");
                    return false;
                } else {
                    log.info(V, "Slave instance desired amount set. ({})", this.cfg.getSlaveInstanceStartAmount());
                }
            }

            ///////////////////////////////////////////////////////////////////////
            ///////////////////// autoscale master yes or no//////////////////////////
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
                this.cfg.setSlaveImage(this.cl.getOptionValue("S", defaults.getProperty("slave-image")).trim());
                if (this.cfg.getSlaveImage() == null) {
                    log.error("-S option is required! Please specify the AMI ID for your slave nodes.");
                    return false;
                } else {
                    log.info(V, "Slave image set. ({})", this.cfg.getSlaveImage());
                }
            }

            ////////////////////////////////////////////////////////////////////////
            ///// ports ////////////////////////////////////////////////////////////
            this.cfg.setPorts(new ArrayList<Integer>());
            String portsCsv = this.cl.getOptionValue("p", defaults.getProperty("ports"));
            if (portsCsv != null && !portsCsv.isEmpty()) {
                try {
                    String[] portsStrings = portsCsv.split(",");
                    for (String portString : portsStrings) {
                        int port = Integer.parseInt(portString.trim());
                        if (port < 0 || port > 65535) {
                            throw new Exception();
                        }
                        this.cfg.getPorts().add(port);
                    }
                } catch (Exception e) {
                    log.error("Could not parse the supplied port list, please make "
                            + "sure you have a list of comma-separated valid ports without spaces in between.",e);
                    return false;
                }
                if (!this.cfg.getPorts().isEmpty()) {
                    StringBuilder portsDisplay = new StringBuilder();
                    for (int port : this.cfg.getPorts()) {
                        portsDisplay.append(port);
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
            this.cfg.setSlaveMounts(new HashMap<String, String>());
            String slaveMountsCsv = this.cl.getOptionValue("f", defaults.getProperty("slave-mounts"));
            if (slaveMountsCsv != null) {
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
            this.cfg.setNfsShares(new ArrayList<String>());
            String nfsSharesCsv = this.cl.getOptionValue("g", defaults.getProperty("nfs-shares"));
            if (nfsSharesCsv != null) {
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

            String earlyScriptFilePath = null;
            if (defaults.containsKey("early-execute-script")) {
                earlyScriptFilePath = defaults.getProperty("early-execute-script");
            }
            if (this.cl.hasOption("ex")) {
                earlyScriptFilePath = this.cl.getOptionValue("ex");
            }
            if (earlyScriptFilePath != null) {
                Path script = FileSystems.getDefault().getPath(earlyScriptFilePath);
                if (!script.toFile().exists()) {
                    log.error("The supplied early shell script file '{}' does not exist!", script);
                    return false;
                }
                this.cfg.setEarlyShellScriptFile(script);
                log.info(V, "Early Shell script file found! ({})", script);
            }

             ////////////////////////////////////////////////////////////////////////
            ///// GLUSTERFS /////////////////////////////////////////////
            ////////////////////////////////////////////////////////////////////////
            ///// gluster-instance-type /////////////////////////////////////////////
            if (req.contains("gli")) {
                try {
                    String glusterTypeString = this.cl.getOptionValue("gli", defaults.getProperty("gluster-instance-type"));
                    if (glusterTypeString == null) {
                        log.error("-gli option is required! Please specify the instance type of your gluster nodes. (e.g. gluster-instance-type=c3.8xlarge)");
                        return false;
                    }
                    InstanceType glusterType = InstanceType.fromValue(glusterTypeString);
                    this.cfg.setGlusterInstanceType(glusterType);
                } catch (Exception e) {
                    log.error("Invalid gluster instance type specified!");
                    return false;
                }
                log.info(V, "Gluster instance type set. ({})", this.cfg.getGlusterInstanceType());
            }

            ////////////////////////////////////////////////////////////////////////
            ///// Gluster on/off /////////////////////////////////////////////////
            if (this.cl.hasOption("gl")) {
                this.cfg.setUseGluster(true);
                log.info(V, "Gluster support enabled.");
            } else if (defaults.containsKey("use-gluster")) {
                String value = defaults.getProperty("use-gluster");
                if (value.equalsIgnoreCase("yes")) {
                    this.cfg.setUseGluster(true);
                    log.info(V, "Gluster support enabled.");
                } else if (value.equalsIgnoreCase("no")) {
                    log.info(V, "Gluster support disabled.");
                    this.cfg.setUseGluster(false);
                } else {
                    log.error("Use-Gluster value in properties not recognized. Please use yes/no.");
                    return false;
                }
            }

            ///////////////////////////////////////////////////////////////////////
            ///////////////////// Gluster instance amount /////////////////////////
            if (req.contains("gla")) {
                try {
                    if (defaults.containsKey("gluster-instance-amount")) {
                        this.cfg.setGlusterInstanceAmount(Integer.parseInt(defaults.getProperty("gluster-instance-amount")));
                    }
                } catch (NumberFormatException nfe) {
                    log.error("Invalid property value for gluster-instance-amount. Please make sure you have a "
                            + "positive integer.");
                    return false;
                }
                if (this.cfg.getGlusterInstanceAmount() <= 0) {
                    log.error("-gla option is required! Please specify the number of glusterfs nodes. (at least 1)");
                    return false;
                } else {
                    log.info(V, "Gluster instance amount set. ({})", this.cfg.getGlusterInstanceAmount());
                }
            }

            ///////////////////////////////////////////////////////////////////////
            ///////////////////// Gluster AMI /////////////////////////////////////
            if (req.contains("glI")) {
                this.cfg.setGlusterImage(this.cl.getOptionValue("glI", defaults.getProperty("gluster-image").trim()));
                if (this.cfg.getGlusterImage() == null) {
                    log.error("-glI option is required! Please specify the AMI ID for your glusterfs nodes.");
                    return false;
                }
                log.info(V, "Gluster image set. ({})", this.cfg.getGlusterImage());
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

        }

        this.intent.setConfiguration(this.cfg);
        return true;
    }

    public Configuration getCfg() {
        return this.cfg;
    }

    private boolean checkInstances(InstanceType it) {
        log.info(V,"check for unsupported instances!");
        // T2 instances currently not working with BiBiGrid see
        if (it.toString().startsWith("t2")) {
            log.error("t2 instance types are currently not supported!");
            return false;
        }
        return true;
    }
}
