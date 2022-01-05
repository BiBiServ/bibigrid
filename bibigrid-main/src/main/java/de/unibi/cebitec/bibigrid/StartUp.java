package de.unibi.cebitec.bibigrid;

import de.unibi.cebitec.bibigrid.core.Constant;
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

    public static void main(String[] args) {
        CommandLineInterpreter cli = CommandLineInterpreter.parseCommandLine(args);
        IntentMode intentMode = cli.getIntentMode();

        // Only version info print necessary
        if (intentMode == VERSION) {
            printVersionInfo();
            return;
        }
        // Just to get information faster
        // TODO verbose mode print table of different machines?

        if (intentMode == HELP) {
            printHelp(cli.getCmdLineOptions());
            return;
        }

        // Options
        if (cli.isMode("verbose")) {
            VerboseOutputFilter.SHOW_VERBOSE = true;
            LOG.info("Enable verbose mode for more detailed output.");
        }

        if (cli.isMode("debug")) {
            Configuration.DEBUG = true;
            LOG.info("Enable debug mode to keep cluster in case of a configuration error.");
        }

        String providerMode = cli.getOptionValue("mode");
        String configurationFile = cli.getOptionValue("config");

        ProviderModule module = loadProviderModule(providerMode);

        try {
            // Provider specific configuration and validator
            // TODO add another module -> loadConfiguration with ssh .. from config file / source, but else loaded from remote
            Configuration config = module.getConfiguration(configurationFile);
            Validator validator =  module.getValidator(config, module);

            // Map of IntentMode and clusterIds
            Map<IntentMode, String[]> clOptions = new HashMap<>();
            for (IntentMode im : IntentMode.values()) {
                String[] parameters = cli.getOptionValues(im.getShortParam());
                if (parameters == null) {
                    parameters = new String[] {cli.getOptionValue(im.getShortParam())};
                }
                clOptions.put(im, parameters);
            }

            if (validator.validateProviderParameters()) {
                runIntent(module, validator, clOptions, intentMode, config);
            } else {
                LOG.error(Constant.ABORT_WITH_NOTHING_STARTED);
            }
        } catch (ConfigurationException e) {
            LOG.error(e.getMessage());
            LOG.error(Constant.ABORT_WITH_NOTHING_STARTED);
        }
    }

    /**
     * Initializes provider module.
     * @param providerMode name of provider (default OpenStack if nothing specified)
     * @return provider module or null, if initialization was not successfully
     */
    private static ProviderModule loadProviderModule(String providerMode) {
        ProviderModule module;
        String[] availableProviderModes = Provider.getInstance().getProviderNames();
        if (availableProviderModes.length == 1) {
            LOG.info("Use {} provider.", availableProviderModes[0]);
            module = Provider.getInstance().getProviderModule(availableProviderModes[0]);
        } else {
            // ProviderMode only used when other providers possible / supported
            LOG.info("Use {} provider.", providerMode);
            module = Provider.getInstance().getProviderModule(providerMode);
        }
        if (module == null) {
            LOG.error(Constant.ABORT_WITH_NOTHING_STARTED);
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
    private static void runIntent(
            ProviderModule module,
            Validator validator,
            Map<IntentMode, String[]> clOptions,
            IntentMode intentMode,
            Configuration config) {
            try {
                module.createClient(config);
            } catch (ClientConnectionFailedException e) {
                LOG.error(e.getMessage());
                LOG.error(ABORT_WITH_NOTHING_STARTED);
                return;
            }

            // In order to validate the native instance types, we need a client.
            // So validating configuration is deferred after client connection is established.
            // Config validation in validate and create intentMode only
            if (intentMode == VALIDATE || intentMode == CREATE) {
                try {
                    if (!validator.validateConfiguration()) {
                        LOG.error(ABORT_WITH_NOTHING_STARTED);
                        return;
                    }
                } catch (Exception e){
                    LOG.error(e.getMessage());
                    if (Configuration.DEBUG) {
                        e.printStackTrace();
                    }
                    return;
                }
                if (module.getValidateIntent(config).validate()) {
                    if (intentMode == VALIDATE) LOG.info(I, "You can now start your cluster.");
                    else {
                        // intentMode CREATE - Start cluster creation
                        CreateCluster cluster = module.getCreateIntent(config, null);
                        runCreateIntent(module, config, cluster);
                    }
                } else {
                    LOG.error("There were one or more errors. Please adjust your configuration.");
                }
                return;
            }

            // Usually parameters equals clusterId(s) or null
            String[] parameters = clOptions.get(intentMode);

            // If parameter given, parameter[0] is clusterId
            String clusterId = parameters.length == 0 ? null : parameters[0];

            // Load cluster config from cloud provider -> clusterMap for specific intentModes
            LoadClusterConfigurationIntent loadIntent = module.getLoadClusterConfigurationIntent(config);
            loadIntent.loadClusterConfiguration(clusterId);
            Map<String, Cluster> clusterMap = loadIntent.getClusterMap();
            if (clusterMap == null || clusterMap.isEmpty()) {
                // if there is no cluster, none action possible
                return;
            }

            switch (intentMode) {
                case LIST:
                    ListIntent listIntent = module.getListIntent(clusterMap);
                    if (clusterId == null) {
                        LOG.info(listIntent.toString());
                    } else {
                        // TODO list of more than one cluster optional?
                        LOG.info(listIntent.toDetailString(clusterId));
                    }
                    break;
                case TERMINATE:
                    boolean terminate_success = module.getTerminateIntent(config, clusterMap).terminate(parameters);
                    if (!terminate_success) {
                        if (parameters.length == 1) {
                            LOG.error("Could not terminate instances with given parameter {}.", clusterId);
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
                    int workerBatch;
                    int count;
                    try {
                        workerBatch = Integer.parseInt(parameters[1]);
                        count = Integer.parseInt(parameters[2]);
                    } catch (NumberFormatException nf) {
                        LOG.error("Wrong usage. Please use '-su <cluster-id> <workerBatch> <count>' instead.");
                        return;
                    }
                    CreateCluster createIntent = module.getCreateIntent(config, clusterId);
                    boolean create_success = createIntent.createWorkerInstances(workerBatch, count);
                    if (!create_success) {
                        LOG.error("Could not create {} worker instances with specified batch {}.", count, workerBatch);
                        return;
                    }
                    break;
                case SCALE_DOWN:
                    try {
                        workerBatch = Integer.parseInt(parameters[1]);
                        count = Integer.parseInt(parameters[2]);
                    } catch (NumberFormatException nf) {
                        LOG.error("Wrong usage. Please use '-sd <cluster-id> <workerBatch> <count>' instead.");
                        return;
                    }
                    module.getTerminateIntent(config, clusterMap).terminateInstances(clusterId, workerBatch, count);
                    break;
                case IDE:
                    try {
                        // Load private key file
                        config.getClusterKeyPair().setName(CreateCluster.PREFIX + clusterId);
                        config.getClusterKeyPair().load();
                        Cluster cluster = clusterMap.get(clusterId);
                        if (loadIntent.loadIdeConfiguration(cluster.getPublicIp())) {
                            new IdeIntent(cluster, config).start();
                        }
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
     * @param cluster CreateCluster implementation
     * @return true, if cluster built successfully
     */
    private static boolean runCreateIntent(ProviderModule module, Configuration config, CreateCluster cluster) {
        try {
            // configure environment
            cluster .createClusterEnvironment()
                    .createNetwork()
                    .createSubnet()
                    .createSecurityGroup()
                    .createKeyPair()
                    .createPlacementGroup();
            // configure cluster
            boolean success =  cluster.configureClusterInstances() && cluster.launchClusterInstances();
            if (!success) {
                //  In DEBUG mode keep partial configured cluster running, otherwise clean it up
                if (Configuration.DEBUG) {
                    LOG.error(StartUp.KEEP);
                } else {
                    LOG.error(StartUp.ABORT_WITH_INSTANCES_RUNNING);
                    Map<String, Cluster> clusterMap = new HashMap<>();
                    clusterMap.put(cluster.getCluster().getClusterId(), cluster.getCluster());
                    module.getTerminateIntent(config, clusterMap).terminate(cluster.getCluster());
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
        Map<String, String> m = getVersionInfo();
        LOG.info(String.format("v%s (Build: %s)", m.get("version"), m.get("build")));
    }

    private static Map<String, String> getVersionInfo(){
        Map<String,String> s = new HashMap<>();
        try {
            URL jarUrl = StartUp.class.getProtectionDomain().getCodeSource().getLocation();
            String jarPath = URLDecoder.decode(jarUrl.getFile(), "UTF-8");
            JarFile jarFile = new JarFile(jarPath);
            Manifest m = jarFile.getManifest();
            s.put("version",m.getMainAttributes().getValue("Bibigrid-version"));
            s.put("build",m.getMainAttributes().getValue("Bibigrid-build-date"));
        } catch (IOException e) {
            LOG.error("Version info could not be read.");
        }
        return s;
    }

    /**
     * Prints out terminal help.
     * @param cmdLineOptions options [-ch -c -v -o ...]
     */
    private static void printHelp(Options cmdLineOptions) {
        Map<String, String> map = getVersionInfo();
        HelpFormatter help = new HelpFormatter();
        String footer = "\nDocumentation at https://github.com/BiBiServ/bibigrid/docs\n";
        footer += "Loaded provider modules: " + String.join(", ", Provider.getInstance().getProviderNames());

        String modes = Arrays.stream(IntentMode.values()).map(m -> "--" + m.getLongParam()).collect(Collectors.joining("|"));
        System.out.println("BiBiGrid is a tool for an easy cluster setup inside a cloud environment.\n");
        help.printHelp(100,
                "java -jar bibigrid-"
                        + String.join(", ", Provider.getInstance().getProviderNames())
                        + "-" + map.get("version")+".jar","", cmdLineOptions, footer);
        System.out.println('\n');
    }

    /**
     * Displays table of different machines (name, cores, ram, disk space, swap, ephemerals.
     *
     * @param module responsible for provider accessibility
     * @param config Configuration to get instance type
     */
    private static void printInstanceTypeHelp(ProviderModule module, Configuration config) {
        StringBuilder display = new StringBuilder();
        Formatter formatter = new Formatter(display, Locale.US);
        display.append("\n");
        formatter.format("%30s | %7s | %14s | %14s | %4s | %10s%n", "name", "cores", "ram Mb", "disk size Mb", "swap",
                "ephemerals");
        display.append(new String(new char[89]).replace('\0', '-')).append("\n");
        for (InstanceType type : module.getInstanceTypes(config)) {
            formatter.format("%30s | %7s | %14s | %14s | %4s | %10s%n", type.getValue(), type.getCpuCores(),
                    type.getMaxRam(), type.getMaxDiskSpace(), type.getSwap(), type.getEphemerals());
        }
        System.out.println(display.toString());
    }
}
