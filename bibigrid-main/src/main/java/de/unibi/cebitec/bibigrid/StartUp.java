package de.unibi.cebitec.bibigrid;

import de.unibi.cebitec.bibigrid.core.Validator;
import de.unibi.cebitec.bibigrid.core.intents.*;
import de.unibi.cebitec.bibigrid.core.model.*;
import de.unibi.cebitec.bibigrid.core.model.exceptions.ClientConnectionFailedException;
import de.unibi.cebitec.bibigrid.core.model.exceptions.ConfigurationException;
import de.unibi.cebitec.bibigrid.core.util.ConfigurationFile;
import de.unibi.cebitec.bibigrid.core.util.VerboseOutputFilter;

import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.Formatter;
import java.util.Locale;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static de.unibi.cebitec.bibigrid.core.util.ImportantInfoOutputFilter.I;

/**
 * Startup/Main class of BiBiGrid.
 *
 * @author Jan Krueger - jkrueger(at)cebitec.uni-bielefeld.de
 */
public class StartUp {
    private static final Logger LOG = LoggerFactory.getLogger(StartUp.class);
    private static final String ABORT_WITH_NOTHING_STARTED = "Aborting operation. No instances started/terminated.";
    private static final String ABORT_WITH_INSTANCES_RUNNING = "Aborting operation. Instances already running. " +
            "I will try to shut them down but in case of an error they might remain running. Please check manually " +
            "afterwards.";

    private static OptionGroup getCMDLineOptionGroup() {
        OptionGroup intentOptions = new OptionGroup();
        intentOptions.setRequired(true);
        Option terminate = new Option(IntentMode.TERMINATE.getShortParam(), IntentMode.TERMINATE.getLongParam(),
                true, "Terminate running cluster");
        terminate.setArgName("cluster-id");
        Option cloud9 = new Option(IntentMode.CLOUD9.getShortParam(), IntentMode.CLOUD9.getLongParam(),
                true, "Start the cloud9 IDE");
        cloud9.setArgName("cluster-id");
        Option ide = new Option(IntentMode.IDE.getShortParam(), IntentMode.IDE.getLongParam(),
                true, "Start a Web IDE");
        ide.setArgName("cluster-id");
        Option list = new Option(IntentMode.LIST.getShortParam(), IntentMode.LIST.getLongParam(),
                true, "List running clusters");
        list.setOptionalArg(true);
        list.setArgName("cluster-id");
        intentOptions
                .addOption(new Option(IntentMode.VERSION.getShortParam(), IntentMode.VERSION.getLongParam(),
                        false, "Version"))
                .addOption(new Option(IntentMode.HELP.getShortParam(), IntentMode.HELP.getLongParam(),
                        false, "Help"))
                .addOption(new Option(IntentMode.CREATE.getShortParam(), IntentMode.CREATE.getLongParam(),
                        false, "Create cluster"))
                .addOption(new Option(IntentMode.PREPARE.getShortParam(), IntentMode.PREPARE.getLongParam(),
                        false, "Prepare cluster images for faster setup"))
                .addOption(list)
                .addOption(new Option(IntentMode.VALIDATE.getShortParam(), IntentMode.VALIDATE.getLongParam(),
                        false, "Validate the configuration file"))
                .addOption(terminate)
                .addOption(cloud9)
                .addOption(ide);
        return intentOptions;
    }

    public static void main(String[] args) {
        CommandLineParser cli = new DefaultParser();
        OptionGroup intentOptions = getCMDLineOptionGroup();
        Options cmdLineOptions = new Options();
        cmdLineOptions.addOption(new Option("h","help",false,"Get some online help"));
        cmdLineOptions.addOption(new Option("v","verbose", false,"More verbose output"));
        cmdLineOptions.addOption(new Option("o","config",true,"Path to JSON configuration file"));

//        Options cmdLineOptions = getRulesToOptions();
        cmdLineOptions.addOptionGroup(intentOptions);
        try {
            CommandLine cl = cli.parse(cmdLineOptions, args);
            if (cl.hasOption("v")) {
                VerboseOutputFilter.SHOW_VERBOSE = true;
            }
            IntentMode intentMode = IntentMode.fromString(intentOptions.getSelected());
            switch (intentMode) {
                case VERSION:
                    printVersionInfo();
                    break;
                case HELP:
                    printHelp(cl, cmdLineOptions);
                    break;
                case CREATE:
                case PREPARE:
                case LIST:
                case TERMINATE:
                case VALIDATE:
                case IDE:
                case CLOUD9:
                    runIntent(cl, intentMode);
                    break;
            }
        } catch (ParseException pe) {
            LOG.error("Error while parsing the commandline arguments: {}", pe.getMessage());
            LOG.error(ABORT_WITH_NOTHING_STARTED);
        }
    }

    private static void printVersionInfo() {
        try {
            URL jarUrl = StartUp.class.getProtectionDomain().getCodeSource().getLocation();
            String jarPath = URLDecoder.decode(jarUrl.getFile(), "UTF-8");
            JarFile jarFile = new JarFile(jarPath);
            Manifest m = jarFile.getManifest();
            System.out.println(String.format("v%s (Build: %s)",
                    m.getMainAttributes().getValue("Bibigrid-version"),
                    m.getMainAttributes().getValue("Bibigrid-build-date")));
        } catch (IOException e) {
            LOG.error("Version info could not be read.");
        }
    }

    /**
     * Prints out terminal help.
     * @param commandLine given cl input arguments
     * @param cmdLineOptions options [-ch -c -v -o ...]
     */
    private static void printHelp(CommandLine commandLine, Options cmdLineOptions) {
        HelpFormatter help = new HelpFormatter();
        String header = "\nDocumentation at https://github.com/BiBiServ/bibigrid/docs\n\n";
        header += "Loaded provider modules: " + String.join(", ", Provider.getInstance().getProviderNames()) + "\n\n";
        String footer = "";
        String modes = Arrays.stream(IntentMode.values()).map(m -> "--" + m.getLongParam()).collect(Collectors.joining("|"));
        help.printHelp("bibigrid " + modes + " [...]", header, cmdLineOptions, footer);
        System.out.println('\n');
        // display instances to create cluster
        runIntent(commandLine, IntentMode.HELP);
    }

    private static void runIntent(CommandLine commandLine, IntentMode intentMode) {
        ConfigurationFile configurationFile = new ConfigurationFile(commandLine);
        String providerMode = parseProviderMode(commandLine, configurationFile);
        if (providerMode == null) {
            LOG.error(StartUp.ABORT_WITH_NOTHING_STARTED);
            return;
        }
        ProviderModule module = Provider.getInstance().getProviderModule(providerMode);
        if (module == null) {
            LOG.error(ABORT_WITH_NOTHING_STARTED);
            return;
        }
        Validator validator;
        try {
            validator = module.getCommandLineValidator(commandLine, configurationFile, intentMode);
        } catch (ConfigurationException e) {
            LOG.error(e.getMessage());
            LOG.error(ABORT_WITH_NOTHING_STARTED);
            return;
        }
        if (validator.validate(providerMode)) {
            Client client;
            try {
                client = module.getClient(validator.getConfig());
            } catch (ClientConnectionFailedException e) {
                LOG.error(e.getMessage());
                LOG.error(ABORT_WITH_NOTHING_STARTED);
                return;
            }
            // In order to validate the native instance types, we need a client. So this step is deferred after
            // client connection is established.
            if (!validator.validateProviderTypes(client)) {
                LOG.error(ABORT_WITH_NOTHING_STARTED);
            }
            switch (intentMode) {
                case HELP:
                    printInstanceTypeHelp(module, client, validator.getConfig());
                    break;
                case LIST:
                    ListIntent listIntent = module.getListIntent(client, validator.getConfig());
                    String clusterId = commandLine.getOptionValue(IntentMode.LIST.getShortParam());
                    if (clusterId != null) {
                        LOG.info(listIntent.toDetailString(clusterId));
                    } else {
                        LOG.info(listIntent.toString());
                    }
                    break;
                case VALIDATE:
                    if (module.getValidateIntent(client, validator.getConfig()).validate()) {
                       LOG.info(I, "You can now start your cluster.");
                    } else {
                       LOG.error("There were one or more errors. Please adjust your configuration.");
                    }
                    break;
                case CREATE:
                    if (module.getValidateIntent(client, validator.getConfig()).validate()) {
                        runCreateIntent(module, validator, client, module.getCreateIntent(client, validator.getConfig()), false);
                    } else {
                        LOG.error("There were one or more errors. Please adjust your configuration.");
                    }
                    break;
                case PREPARE:
                    CreateCluster cluster = module.getCreateIntent(client, validator.getConfig());
                    if (runCreateIntent(module, validator, client, cluster, true)) {
                        module.getPrepareIntent(client, validator.getConfig()).prepare(cluster.getMasterInstance(),
                                cluster.getWorkerInstances());
                        module.getTerminateIntent(client, validator.getConfig()).terminate();
                    }
                    break;
                case TERMINATE:
                    module.getTerminateIntent(client, validator.getConfig()).terminate();
                    break;
                case CLOUD9:
                    LOG.warn("Arg --cloud9 is deprecated. Use --ide instead.");
                case IDE:
                    new IdeIntent(module, client, validator.getConfig()).start();
                    break;
                default:
                    LOG.warn("unknown intent mode");
                    break;
            }
        } else {
            LOG.error(ABORT_WITH_NOTHING_STARTED);
        }
    }

    /**
     * Runs cluster creation and launch processing.
     *
     * @param module responsible for provider accessibility
     * @param validator validates overall configuration
     * @param client Client
     * @param cluster CreateCluster implementation
     * @param prepare true, if still preparation necessary
     * @return true, if cluster built successfully.
     */
    private static boolean runCreateIntent(ProviderModule module, Validator validator, Client client,
                                           CreateCluster cluster, boolean prepare) {
        try {
            boolean success = cluster
                    .createClusterEnvironment()
                    .createNetwork()
                    .createSubnet()
                    .createSecurityGroup()
                    .createPlacementGroup()
                    .configureClusterMasterInstance()
                    .configureClusterWorkerInstance()
                    .launchClusterInstances(prepare);
            if (!success) {
                LOG.error(StartUp.ABORT_WITH_INSTANCES_RUNNING);
                TerminateIntent cleanupIntent = module.getTerminateIntent(client, validator.getConfig());
                cleanupIntent.terminate();
                return false;
            }
        } catch (ConfigurationException ex) {
            // print stacktrace only verbose mode, otherwise the message is fine
            if (VerboseOutputFilter.SHOW_VERBOSE) {
                LOG.error("Failed to create cluster. {} {}", ex.getMessage(), ex);
            } else {
                LOG.error("Failed to create cluster. {}", ex.getMessage());
            }
            return false;
        }
        return true;
    }

    private static String parseProviderMode(CommandLine commandLine, ConfigurationFile configurationFile) {
        if (!commandLine.hasOption("mode")) {
            String[] providerNames = Provider.getInstance().getProviderNames();
            if (configurationFile.getPropertiesMode() == null && providerNames.length == 1) {
                return providerNames[0];
            }
            if (configurationFile.getPropertiesMode() != null) {
                return configurationFile.getPropertiesMode();
            }
        } else {
            try {
                return commandLine.getOptionValue("mode", "").trim();
            } catch (IllegalArgumentException ignored) {
            }
        }
        LOG.error("No suitable mode found in command line or properties file. Exit");
        return null;
    }

    /**
     * Displays table of different machines (name, cores, ram, disk space, swap, ephemerals.
     *
     * @param module responsible for provider accessibility
     * @param client Provider client user to connect to cluster
     * @param config Configuration to get instance type
     */
    private static void printInstanceTypeHelp(ProviderModule module, Client client, Configuration config) {
        StringBuilder display = new StringBuilder();
        Formatter formatter = new Formatter(display, Locale.US);
        display.append("\n");
        formatter.format("%30s | %7s | %14s | %14s | %4s | %10s%n", "name", "cores", "ram Mb", "disk size Mb", "swap",
                "ephemerals");
        display.append(new String(new char[89]).replace('\0', '-')).append("\n");
        for (InstanceType type : module.getInstanceTypes(client, config)) {
            formatter.format("%30s | %7s | %14s | %14s | %4s | %10s%n", type.getValue(), type.getCpuCores(),
                    type.getMaxRam(), type.getMaxDiskSpace(), type.getSwap(), type.getEphemerals());
        }
        System.out.println(display.toString());
    }
}
