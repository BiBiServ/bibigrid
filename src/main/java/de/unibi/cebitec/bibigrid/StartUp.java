package de.unibi.cebitec.bibigrid;

import com.amazonaws.services.ec2.model.InstanceType;
import de.unibi.cebitec.bibigrid.ctrl.*;
import de.unibi.cebitec.bibigrid.exception.IntentNotConfiguredException;
import de.unibi.cebitec.bibigrid.util.VerboseOutputFilter;
import java.net.URL;
import java.net.URLDecoder;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Startup/Main class of BiBiGrid.
 * 
 * @author Jan Krueger - jkrueger(at)cebitec.un-bielefel.de
 */
public class StartUp {

    public static final Logger log = LoggerFactory.getLogger(StartUp.class);
    public static final String ABORT_WITH_NOTHING_STARTED = "Aborting operation."
            + " No instances started/terminated.";
    public static final String ABORT_WITH_INSTANCES_RUNNING = "Aborting operation."
            + " Instances already running. I will try to shut them down but in case of"
            + " an error they might remain running. Please check manually afterwards.";

    
    public static OptionGroup getCMDLineOptionGroup() {
        OptionGroup intentOptions = new OptionGroup();
        intentOptions.setRequired(true);
        Option terminate = new Option("t","terminate",true,"terminate running cluster");
        terminate.setArgName("cluster-id");
        intentOptions
                .addOption(new Option("V","version",false,"version"))
                .addOption(new Option("h","help",false,"help"))
                .addOption(new Option("c","create",false,"create cluster"))
                .addOption(new Option("l","list",false,"list running clusters"))
                .addOption(new Option("ch","check",false,"check config file"))
                .addOption(terminate);
        return intentOptions;
    }
    
    public static Options getCMDLineOptions(OptionGroup optgrp) {
        Options cmdLineOptions = new Options();
        cmdLineOptions
                .addOptionGroup(optgrp)
                .addOption("m", "master-instance-type", true, "see INSTANCE-TYPES below")
                .addOption("mme", "max-master-ephemerals", true, "limits the maxium number of used ephemerals for master spool volume (raid 0)")
                .addOption("M", "master-image", true, "machine image id for master, if not set  images defined at https://bibiserv.cebitec.uni-bielefeld.de/resoruces/bibigrid/<framework>/<region>.ami.properties are used!")
                .addOption("s", "slave-instance-type", true, "see INSTANCE-TYPES below")
                .addOption("mse", "max-slave-ephemerals", true, "limits the maxium number of used ephemerals for slave spool volume (raid 0 )")
                .addOption("n", "slave-instance-count", true, "min: 0")
                .addOption("S", "slave-image", true, "machine image id for slaves, same behaviour like master-image")
                .addOption("usir", "use-spot-instance-request", true, " Yes or No of spot instances should be used  (Type t instance types are unsupported).")
                .addOption("bp", "bidprice", true, "bid price for spot instances")
                .addOption("bpm", "bidprice-master", true, "bid price for the master spot instance, if not set general 'bidprice' is used.")
                .addOption("k", "keypair", true, "name of the keypair in aws console")
                .addOption("i", "identity-file", true, "absolute path to private ssh key file")
                .addOption("e", "region", true, "region of instance")
                .addOption("z", "availability-zone", true, "")
                .addOption("ex", "early-execute-script", true, "path to shell script to be executed on master instance startup (size limitation of 10K chars)")
                .addOption("esx", "early-slave-execute-script", true, " path to shell script to be executed on slave instance(s) startup (size limitation of 10K chars)")
                .addOption("a", "aws-credentials-file", true, "containing access-key-id & secret-key, default: ~/.bibigrid.properties")
                .addOption("p", "ports", true, "comma-separated list of additional ports (tcp & udp) to be opened for all nodes (e.g. 80,443,8080). Ignored if 'security-group' is set!")
                .addOption("sg", "security-group", true,"security group id used by current setup")
                .addOption("d", "master-mounts", true, "comma-separated snapshot=mountpoint list (e.g. snap-12234abcd=/mnt/mydir1,snap-5667889ab=/mnt/mydir2) mounted to master. (Optional: Partition selection with ':', e.g. snap-12234abcd:1=/mnt/mydir1)")
                .addOption("f", "slave-mounts", true, "comma-separated snapshot=mountpoint list (e.g. snap-12234abcd=/mnt/mydir1,snap-5667889ab=/mnt/mydir2) mounted to all slaves individually")
                .addOption("x", "execute-script", true, "shell script file to be executed on master")
                .addOption("g", "nfs-shares", true, "comma-separated list of paths on master to be shared via NFS")
                .addOption("v", "verbose", false, "more console output")
                .addOption("o", "config", true, "path to alternative config file")
                .addOption("b", "use-master-as-compute", true, "yes or no if master is supposed to be used as a compute instance")
                .addOption("db", "cassandra", false, "Enable Cassandra database support")
                .addOption("gpf", "grid-properties-file", true, "store essential grid properties like master & slave dns values and grid id in a Java property file")
                .addOption("vpc", "vpc-id", true, "Vpc ID used instead of default vpc")
                .addOption("router", "router", true, "Name of router used (Openstack), only one of --router --network or --subnet should be used. ")
                .addOption("network", "network", true, "Name of network used (Openstack), only one of --router --network or --subnet should be used.")
                .addOption("subnet", "subnet", true, "Naem of subnet used (Openstack), only one of --router --network or --subnet should be used.")
                .addOption("psi", "public-slave-ip", true, "Slave instances also get an public ip address")
                .addOption("me", "mesos", true, "Yes or no if Mesos framework should be configured/started. Default is No")
                .addOption("mode", "meta-mode", true, "Allows you to use a different cloud provider e.g openstack with meta=openstack. Default AWS is used!")
                .addOption("oge", "oge", true, "Yes or no if OpenGridEngine should be configured/started. Default is Yes!")
                .addOption("nfs", "nfs", true, "Yes or no if NFS should be configured/started. Default is Yes!")
                .addOption("lfs", "local-fs", true, "File system used for internal (empheral) diskspace. One of 'ext2', 'ext3', 'ext4' or 'xfs'. Default is 'xfs'.")
                .addOption("u", "user", true, "User name (mandatory)")
                .addOption("osu", "openstack-username", true, "The given Openstack Username")
                .addOption("ost", "openstack-tenantname", true, "The given Openstack Tenantname")
                .addOption("osp", "openstack-password", true, "The given Openstack User-Password")
                .addOption("ose", "openstack-endpoint", true, "The given Openstack Endpoint e.g. (http://xxx.xxx.xxx.xxx:5000/v2.0/)")
                .addOption("osd", "openstack-domain", true, "The given Openstack Domain")
                .addOption("dr", "debug-requests", false, "Enable HTTP request and response logging.")
                .addOption("ansadd", "ansible-additionals", false, "Path to folder containing ansible playbooks which will be copied to the master instance.");
        return cmdLineOptions;
    }

    public static void main(String[] args) {

        CommandLineParser cli = new DefaultParser();
        OptionGroup intentOptions = getCMDLineOptionGroup();
        Options cmdLineOptions = getCMDLineOptions(intentOptions);

        
        try {
            CommandLine cl = cli.parse(cmdLineOptions, args);
            CommandLineValidator validator = null;
            Intent intent = null;
            if (cl.hasOption("v")) {
                VerboseOutputFilter.SHOW_VERBOSE = true;
            }
            switch (intentOptions.getSelected()) {
                case "V":
                    try {
                        URL jarUrl = StartUp.class.getProtectionDomain().getCodeSource().getLocation();
                        String jarPath = URLDecoder.decode(jarUrl.getFile(), "UTF-8");
                        JarFile jarFile = new JarFile(jarPath);
                        Manifest m = jarFile.getManifest();
                        StringBuilder versionInfo = new StringBuilder("v");
                        versionInfo.append(m.getMainAttributes().getValue("Bibigrid-version"));
                        versionInfo.append(" (Build: ");
                        versionInfo.append(m.getMainAttributes().getValue("Bibigrid-build-date"));
                        versionInfo.append(")");
                        System.out.println(versionInfo.toString());
                    } catch (Exception e) {
                        log.error("Version info could not be read.");
                    }
                    break;
                case "h":
                    HelpFormatter help = new HelpFormatter();
                    String header = ""; //TODO: infotext (auf default props hinweisen); instanzgroessen auflisten
                    StringBuilder footer = new StringBuilder(" \nValid INSTANCE-TYPES are:");
                    for (InstanceType type : InstanceType.values()) {
                        footer.append("\n-\t");
                        footer.append(type.toString());
                    }
                    footer.append("\n.");
                    help.printHelp("bibigrid --create|--list|--terminate|--check [...]", header, cmdLineOptions, footer.toString());
                    break;
                case "c":
                    intent = new CreateIntent();
                    validator = new CommandLineValidator(cl, intent);
                    break;
                case "l":
                    intent = new ListIntent();
                    validator = new CommandLineValidator(cl, intent);
                    break;
                case "t":
                    intent = new TerminateIntent();
                    validator = new CommandLineValidator(cl, intent);
                    break;
                case "ch":
                    intent = new ValidationIntent();
                    validator = new CommandLineValidator(cl, intent);
                    break;
                default:
                    return;
            }
            if (intent != null) {
                if (validator.validate()) {
                    try {
                        intent.execute();
                    } catch (IntentNotConfiguredException e) {
                        log.error(e.getMessage());
                    }
                } else {
                    log.error(ABORT_WITH_NOTHING_STARTED);
                }
            }
        } catch (ParseException pe) {
            log.error("Error while parsing the commandline arguments: {}", pe.getMessage());
            log.error(ABORT_WITH_NOTHING_STARTED);
        }
    }
}
