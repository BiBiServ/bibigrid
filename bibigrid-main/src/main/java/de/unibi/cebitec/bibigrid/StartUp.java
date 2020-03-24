package de.unibi.cebitec.bibigrid;

import de.unibi.cebitec.bibigrid.core.Validator;
import de.unibi.cebitec.bibigrid.core.intents.*;
import de.unibi.cebitec.bibigrid.core.model.*;
import de.unibi.cebitec.bibigrid.core.model.exceptions.ClientConnectionFailedException;
import de.unibi.cebitec.bibigrid.core.model.exceptions.ConfigurationException;
import de.unibi.cebitec.bibigrid.core.util.VerboseOutputFilter;

import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.*;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static de.unibi.cebitec.bibigrid.core.model.IntentMode.*;
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
            "Attempting to shut them down but in case of an error they might remain running. Please verify " +
            "afterwards.";
    private static final String KEEP = "Keeping the partly configured cluster for debug purposes. Please remember to shut it down afterwards.";

    private static final String CID = "cluster-id";
    private static final String WORKERS = "worker-instances";
    private static final int SCALE_ARGS = 3; // cluster-id, workerBatch, count

    private static OptionGroup getCMDLineOptionGroup() {
        OptionGroup intentOptions = new OptionGroup();
        intentOptions.setRequired(true);
        Option terminate = new Option(TERMINATE.getShortParam(), TERMINATE.getLongParam(),
                true, "Terminate running cluster");
        terminate.setArgs(Option.UNLIMITED_VALUES);
        terminate.setArgName(CID);
        Option upscale = new Option(SCALE_UP.getShortParam(), SCALE_UP.getLongParam(),
                true, "Adds a given amount of workers of a specific instance type");
        upscale.setArgs(SCALE_ARGS);
        upscale.setArgName(WORKERS);
        Option downscale = new Option(SCALE_DOWN.getShortParam(), SCALE_DOWN.getLongParam(),
                true, "Terminates a given amount of workers of a specific instance type");
        downscale.setArgs(SCALE_ARGS);
        downscale.setArgName(WORKERS);
        Option cloud9 = new Option(CLOUD9.getShortParam(), CLOUD9.getLongParam(),
                true, "Start the cloud9 IDE");
        cloud9.setArgName(CID);
        Option ide = new Option(IDE.getShortParam(), IDE.getLongParam(),
                true, "Start a Web IDE");
        ide.setArgName(CID);
        Option list = new Option(LIST.getShortParam(), LIST.getLongParam(),
                true, "List running clusters");
        list.setOptionalArg(true);
        list.setArgName(CID);
        intentOptions
                .addOption(new Option(VERSION.getShortParam(), VERSION.getLongParam(),
                        false, "Version"))
                .addOption(new Option(HELP.getShortParam(), HELP.getLongParam(),
                        false, "Help"))
                .addOption(new Option(CREATE.getShortParam(), CREATE.getLongParam(),
                        false, "Create cluster"))
                .addOption(new Option(PREPARE.getShortParam(), PREPARE.getLongParam(),
                        false, "Prepare cluster images for faster setup"))
                .addOption(list)
                .addOption(new Option(VALIDATE.getShortParam(), VALIDATE.getLongParam(),
                        false, "Validate the configuration file"))
                .addOption(terminate)
                .addOption(upscale)
                .addOption(downscale)
                .addOption(cloud9)
                .addOption(ide);
        return intentOptions;
    }

    public static void main(String[] args) {
        CommandLineParser cli = new DefaultParser();
        OptionGroup intentOptions = getCMDLineOptionGroup();
        Options cmdLineOptions = new Options()
                .addOption(new Option("h","help",false,"Get some online help"))
                .addOption(new Option("v","verbose", false,"More verbose output"))
                .addOption(new Option("o","config",true,"Path to JSON configuration file"))
                .addOption(new Option("d","debug",false,"Don't shut down cluster in the case of a configuration error."))
                .addOption(new Option("m","mode",true,"One of "+String.join(",",Provider.getInstance().getProviderNames())))
                .addOptionGroup(intentOptions);
        try {
            CommandLine cl = cli.parse(cmdLineOptions, args);

            IntentMode intentMode = IntentMode.fromString(intentOptions.getSelected());

            // Only version info print necessary
            if (intentMode == VERSION) {
                printVersionInfo();
                return;
            }
            // Just to get information faster
            if (intentMode == HELP) {
                printHelp(cl, cmdLineOptions);
            }

            // Options
            if (cl.hasOption("verbose")) {
                VerboseOutputFilter.SHOW_VERBOSE = true;
                LOG.info("Enable verbose mode for more detailed output.");
            }

            if (cl.hasOption("debug")) {
                Configuration.DEBUG = true;
                LOG.info("Enable debug mode to keep cluster in case of a configuration error.");
            }

            String providerMode = cl.getOptionValue("mode");
            String configurationFile = cl.getOptionValue("config");

            ProviderModule module = loadProviderModule(providerMode);

            try {
                // Provider specific configuration and validator
                Configuration config = module.getConfiguration(configurationFile);
                Validator validator =  module.getValidator(config,module);

                // Map of IntentMode and clusterIds
                Map<IntentMode, String[]> clOptions = new HashMap<>();
                for (IntentMode im : IntentMode.values()) {
                    String[] parameters = cl.getOptionValues(im.getShortParam());
                    if (parameters == null) {
                        parameters = new String[] {cl.getOptionValue(im.getShortParam())};
                    }
                    clOptions.put(im, parameters);
                }

                if (validator.validateProviderParameters()) {
                    runIntent(module, validator, clOptions, intentMode, config);
                } else {
                    LOG.error(ABORT_WITH_NOTHING_STARTED);
                }
            } catch (ConfigurationException e) {
                LOG.error(e.getMessage());
                LOG.error(ABORT_WITH_NOTHING_STARTED);
            }
        } catch (ParseException pe) {
            LOG.error("Error while parsing the commandline arguments: {}", pe.getMessage());
            LOG.error(ABORT_WITH_NOTHING_STARTED);
        }
    }

    /**
     * Initializes provider module.
     * @param providerMode name of provider (default OpenStack if nothing specified)
     * @return provider module or null, if initialization was not successfully
     */
    private static ProviderModule loadProviderModule(String providerMode) {
        ProviderModule module;
        String [] availableProviderModes = Provider.getInstance().getProviderNames();
        if (availableProviderModes.length == 1) {
            LOG.info("Use {} provider.", availableProviderModes[0]);
            module = Provider.getInstance().getProviderModule(availableProviderModes[0]);
        } else {
            // ProviderMode only used when other providers possible / supported
            LOG.info("Use {} provider.", providerMode);
            module = Provider.getInstance().getProviderModule(providerMode);
        }
        if (module == null) {
            LOG.error(ABORT_WITH_NOTHING_STARTED);
        }
        return module;
    }

    /**
     * Runs intent of a client by specified cl intentMode.
     * @param module provider specific interface
     * @param validator validation of configuration
     * @param clOptions Map of IntentMode and clusterIds
     * @param intentMode Current IntentMode
     * @param config Configuration
     */
    private static void runIntent(ProviderModule module, Validator validator, Map<IntentMode, String[]> clOptions, IntentMode intentMode, Configuration config) {
            Client client;
            try {
                client = module.getClient(config);
            } catch (ClientConnectionFailedException e) {
                LOG.error(e.getMessage());
                LOG.error(ABORT_WITH_NOTHING_STARTED);
                return;
            }

            // Usually parameters equals clusterId(s) or null
            String[] parameters = clOptions.get(intentMode);

            // -h and -l parameters don't need validation
            if (intentMode == HELP) {
                printInstanceTypeHelp(module, client, config);
                return;
            } else if (intentMode == LIST) {
                LoadClusterConfigurationIntent loadIntent = module.getLoadClusterConfigurationIntent(client, config);
                loadIntent.loadClusterConfiguration();
                Map<String, Cluster> clusterMap = loadIntent.getClusterMap();
                if (clusterMap.isEmpty()) {
                    return;
                }
                ListIntent listIntent = module.getListIntent((HashMap<String, Cluster>) clusterMap);
                if (parameters == null) {
                    LOG.info(listIntent.toString());
                } else {
                    String clusterId = parameters[0];
                    if (clusterId != null) {
                        LOG.info(listIntent.toDetailString(clusterId));
                    } else {
                        LOG.info(listIntent.toString());
                    }
                }
                 return;
            }

            // In order to validate the native instance types, we need a client.
            // So this step is deferred after client connection is established.
            if (!validator.validateProviderTypes(client) || !validator.validateConfiguration()) {
                LOG.error(ABORT_WITH_NOTHING_STARTED);
                return;
            }

            switch (intentMode) {
                case VALIDATE:
                    if (module.getValidateIntent(client, config).validate()) {
                       LOG.info(I, "You can now start your cluster.");
                    } else {
                       LOG.error("There were one or more errors. Please adjust your configuration.");
                    }
                    break;
                case CREATE:
                    if (module.getValidateIntent(client, config).validate()) {
                        String clusterId = parameters != null ? parameters[0] : null;
                        CreateCluster cluster = module.getCreateIntent(client, config, clusterId);
                        runCreateIntent(module, config, client, cluster, false);
                    } else {
                        LOG.error("There were one or more errors. Please adjust your configuration.");
                    }
                    break;
                case PREPARE:
                    if (module.getValidateIntent(client, config).validate()) {
                        String clusterId = parameters != null ? parameters[0] : null;
                        CreateCluster cluster = module.getCreateIntent(client, config, clusterId);
                        if (runCreateIntent(module, config, client, cluster, true)) {
                            module.getPrepareIntent(client, config).prepare(cluster);
                            TerminateIntent terminateIntent = module.getTerminateIntent(client, config);
                            if (!terminateIntent.terminate(clusterId)) {
                                LOG.error("Could not terminate instances with given clusterId {}", clusterId);
                            }
                        }
                    } else {
                        LOG.error("There were one or more errors. Please adjust your configuration.");
                    }
                    break;
                case TERMINATE:
                    if (!module.getTerminateIntent(client, config).terminate(parameters)) {
                        if (parameters.length == 1) {
                            LOG.error("Could not terminate instances with given parameter");
                        } else {
                            StringBuilder error = new StringBuilder("Could not terminate instances with given parameters ");
                            for (int p = 0; p < parameters.length; p++) {
                                String parameter = parameters[p];
                                error.append(parameter);
                                if (p < parameters.length -1) {
                                    error.append(", ");
                                }
                            }
                            LOG.error(error.toString());
                        }
                    }
                    break;
                case SCALE_UP:
                    String clusterId = parameters[0];
                    int workerBatch;
                    int count;
                    try {
                        workerBatch = Integer.parseInt(parameters[1]);
                        count = Integer.parseInt(parameters[2]);
                    } catch (NumberFormatException nf) {
                        LOG.error("Wrong usage. Please use '-su <cluster-id> <workerBatch> <count> instead.'");
                        return;
                    }
                    CreateCluster createIntent = module.getCreateIntent(client, config, clusterId);
//                    try {
//                        createIntent.createClusterEnvironment();
//                    } catch (ConfigurationException e) {
//                        e.printStackTrace();
//                    }
                    if (!createIntent.createWorkerInstances(workerBatch, count)) {
                        LOG.error("Could not create worker instances with specified batch.");
                        return;
                    }
                    break;
                case SCALE_DOWN:
                    clusterId = parameters[0];
                    try {
                        workerBatch = Integer.parseInt(parameters[1]);
                        count = Integer.parseInt(parameters[2]);
                    } catch (NumberFormatException nf) {
                        LOG.error("Wrong usage. Please use '-sd <cluster-id> <workerBatch> <count> instead.'");
                        return;
                    }
                    module.getTerminateIntent(client, config)
                            .terminateInstances(clusterId, workerBatch, count);
                    break;
                case CLOUD9:
                    LOG.warn("Command-line option --cloud9 is deprecated. Please use --ide instead. Continuing ...");
                case IDE:
                    try {
                        // Load private key file
                        clusterId = parameters.length == 1 ? parameters[0] : null;
                        config.getClusterKeyPair().setName(CreateCluster.PREFIX + clusterId);
                        config.getClusterKeyPair().load();
                        new IdeIntent(module, client, clusterId, config).start();
                    } catch (IOException e) {
                        LOG.error("Exception occurred loading private key. {}",e.getMessage());
                        if (Configuration.DEBUG) {
                            e.printStackTrace();
                        }
                    }
                    break;
                default:
                    LOG.warn("Unknown intent mode.");
                    break;
            }
    }

    /**
     * Runs cluster creation and launch processing.
     *
     * @param module responsible for provider accessibility
     * @param config overall configuration
     * @param client Client
     * @param cluster CreateCluster implementation
     * @param prepare true, if in prepare mode
     * @return true, if cluster built successfully
     */
    private static boolean runCreateIntent(ProviderModule module, Configuration config, Client client,
                                           CreateCluster cluster, boolean prepare) {
        try {
            // configure environment
            cluster .createClusterEnvironment()
                    .createNetwork()
                    .createSubnet()
                    .createSecurityGroup()
                    .createKeyPair()
                    .createPlacementGroup();
            // configure cluster
            boolean success =  cluster
                    .configureClusterMasterInstance()
                    .configureClusterWorkerInstance()
                    .launchClusterInstances(prepare);
            if (!success) {
                //  In DEBUG mode keep partial configured cluster running, otherwise clean it up
                if (Configuration.DEBUG) {
                    LOG.error(StartUp.KEEP);
                } else {
                    LOG.error(StartUp.ABORT_WITH_INSTANCES_RUNNING);
                    module.getTerminateIntent(client, config).terminate(cluster.getClusterId());
                }
                return false;
            }
        } catch (ConfigurationException ex) {
            // print stacktrace only in verbose mode, otherwise just the message is fine
            if (VerboseOutputFilter.SHOW_VERBOSE) {
                LOG.error("Failed to create cluster. {} {}", ex.getMessage(), ex);
            } else {
                LOG.error("Failed to create cluster. {}", ex.getMessage());
            }
            return false;
        }
        return true;
    }

    /**
     * Prints out version of BiBiGrid with date of build.
     */
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
        //help.printHelp("bibigrid " + modes + " [...]", header, cmdLineOptions, footer);
        System.out.println('\n');
        // display instances to create cluster
        //runIntent(commandLine, IntentMode.HELP);
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
