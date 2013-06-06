package de.unibi.cebitec.bibigrid.ctrl;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.ec2.model.InstanceType;
import de.unibi.cebitec.bibigrid.model.Configuration;
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
        this.propertiesFilePath = FileSystems.getDefault().getPath(DEFAULT_PROPERTIES_DIRNAME, DEFAULT_PROPERTIES_FILENAME);
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
            ///// endpoint /////////////////////////////////////////////////////////

            if (req.contains("e")) {
                this.cfg.setEndpoint(this.cl.getOptionValue("e", defaults.getProperty("endpoint")));
                if (this.cfg.getEndpoint() == null) {
                    log.error("-e option is required! Please specify the url of your API endpoint "
                            + "(e.g. ec2.eu-west-1.amazonaws.com).");
                    return false;
                }
                log.info(V, "Endpoint set. ({})", this.cfg.getEndpoint());
            }


            ////////////////////////////////////////////////////////////////////////
            ///// availability-zone ////////////////////////////////////////////////

            if (req.contains("z")) {
                this.cfg.setAvailabilityZone(this.cl.getOptionValue("z", defaults.getProperty("availability-zone")));
                if (this.cfg.getAvailabilityZone() == null) {
                    log.error("-z option is required! Please specify an availability zone "
                            + "(e.g. eu-west-1a).");
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
                        log.error("-m option is required! Please specify the instance type of your master node.");
                        return false;
                    }
                    InstanceType masterType = InstanceType.fromValue(masterTypeString);
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
                this.cfg.setMasterImage(this.cl.getOptionValue("M", defaults.getProperty("master-image")));
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
                        log.error("-s option is required! Please specify the instance type of your slave nodes.");
                        return false;
                    }
                    InstanceType slaveType = InstanceType.fromValue(slaveTypeString);
                    this.cfg.setSlaveInstanceType(slaveType);
                } catch (Exception e) {
                    log.error("Invalid slave instance type specified!");
                    return false;
                }
                log.info(V, "Slave instance type set. ({})", this.cfg.getSlaveInstanceType());
            }


            ////////////////////////////////////////////////////////////////////////
            ///// slave-instance-count /////////////////////////////////////////////

            if (req.contains("n")) {
                try {
                    if (defaults.containsKey("slave-instance-count")) {
                        this.cfg.setSlaveInstanceCount(Integer.parseInt(defaults.getProperty("slave-instance-count")));
                    }
                } catch (NumberFormatException nfe) {
                    log.error("Invalid property value for slave-instance-count. Please make sure you have an integer here.");
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
                        log.error("Invalid argument for -n. Please make sure you have an integer here.");
                    }
                }
                if (this.cfg.getSlaveInstanceCount() < 0) {
                    log.error("-n option is required! Please specify the number of slave nodes. (at least 0)");
                    return false;
                } else {
                    log.info(V, "Slave instance count set. ({})", this.cfg.getSlaveInstanceCount());
                }
            }


            ////////////////////////////////////////////////////////////////////////
            ///// slave-image //////////////////////////////////////////////////////

            if (req.contains("S")) {
                this.cfg.setSlaveImage(this.cl.getOptionValue("S", defaults.getProperty("slave-image")));
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
            if (portsCsv != null) {
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
                            + "sure you have a list of comma-separated valid ports without spaces in between.");
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
            if (masterMountsCsv != null) {
                try {
                    String[] masterMounts = masterMountsCsv.split(",");
                    for (String masterMountsKeyValue : masterMounts) {
                        String[] masterMountsSplit = masterMountsKeyValue.trim().split("=");
                        String snapshot = masterMountsSplit[0].trim();
                        String mountpoint = masterMountsSplit[1].trim();
                        this.cfg.getMasterMounts().put(snapshot, mountpoint);
                    }
                } catch (Exception e) {
                    log.error("Could notparse the list of master mounts, please make "
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
                    log.error("Could notparse the list of slave mounts, please make "
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

        }



        this.intent.setConfiguration(this.cfg);
        return true;
    }

    public Configuration getCfg() {
        return this.cfg;
    }
}
