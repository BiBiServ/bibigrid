package de.unibi.cebitec.bibigrid;

import de.unibi.cebitec.bibigrid.core.Constant;
import de.unibi.cebitec.bibigrid.core.DataBase;
import de.unibi.cebitec.bibigrid.core.Validator;
import de.unibi.cebitec.bibigrid.core.intents.*;
import de.unibi.cebitec.bibigrid.core.model.*;
import de.unibi.cebitec.bibigrid.core.model.exceptions.ClientConnectionFailedException;
import de.unibi.cebitec.bibigrid.core.model.exceptions.ConfigurationException;
import de.unibi.cebitec.bibigrid.core.util.ConfigurationFile;
import de.unibi.cebitec.bibigrid.core.util.Status;
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
        cmdLineOptions.addOption(new Option("d","debug",false,"Don't shut down cluster in the case of a configuration error."));
        cmdLineOptions.addOption(new Option("m","mode",true,"One of "+String.join(",",Provider.getInstance().getProviderNames())));
        cmdLineOptions.addOptionGroup(intentOptions);
        try {
            CommandLine cl = cli.parse(cmdLineOptions, args);

            // Help and Version
            IntentMode intentMode = IntentMode.fromString(intentOptions.getSelected());
            switch (intentMode) {
                case VERSION:
                    printVersionInfo();
                    break;
                case HELP:
                    printHelp(cl, cmdLineOptions);
                    break;
            }

            // Options
            VerboseOutputFilter.SHOW_VERBOSE = cl.hasOption("v");
            Configuration.DEBUG = cl.hasOption("debug");

            String configurationFile = cl.getOptionValue("config");

            ProviderModule module = initProviderModule(cl.getOptionValue("mode"));

            try {
                // get provider specific configuration
                Configuration config = module.getConfiguration(configurationFile);

                // get provider specific validator
                Validator validator =  module.getValidator(config,module);

                switch (intentMode){
                    case TERMINATE:
                        config.setClusterIds(cl.getOptionValue(IntentMode.TERMINATE.getShortParam()).trim());
                        break;
                    case CLOUD9:
                    case IDE:
                        config.setId(cl.getOptionValue(IntentMode.IDE.getShortParam().trim()));
                        break;
                    case CREATE:
                    case PREPARE:
                    case LIST:
                        String id = cl.getOptionValue(IntentMode.LIST.getShortParam());
                        if (id != null) {
                            config.setId(id.trim());
                        }
                        break;
                    case VALIDATE:
                }
                if (validator.validate()) {
                    runIntent(module, validator, config, intentMode);
                } else {
                    LOG.error(Constant.ABORT_WITH_NOTHING_STARTED);
                }
            } catch (ConfigurationException e) {
                LOG.error(e.getMessage());
                LOG.error(Constant.ABORT_WITH_NOTHING_STARTED);
            }
        } catch (ParseException pe) {
            LOG.error("Error while parsing the commandline arguments: {}", pe.getMessage());
            LOG.error(Constant.ABORT_WITH_NOTHING_STARTED);
        }
    }

    /**
     * Initializes ProviderModule.
     * @param providerMode cl option "mode"
     * @return module, if initialization finished properly, otherwise null
     */
    private static ProviderModule initProviderModule(String providerMode) {
        ProviderModule module;
        String [] availableProviderModes = Provider.getInstance().getProviderNames();
        if (availableProviderModes.length == 1) {
            LOG.info("Use {} provider.",availableProviderModes[0]);
            module = Provider.getInstance().getProviderModule(availableProviderModes[0]);
        } else {
            LOG.info("Use {} provider.",providerMode);
            module = Provider.getInstance().getProviderModule(providerMode);
        }
        if (module == null) {
            LOG.error(Constant.ABORT_WITH_NOTHING_STARTED);
        }
        return module;
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
    }

    private static void runIntent(ProviderModule module, Validator validator, Configuration config, IntentMode intentMode) {
        Client client;
        try {
            client = module.getClient(config);
        } catch (ClientConnectionFailedException e) {
            LOG.error(e.getMessage());
            LOG.error(Constant.ABORT_WITH_NOTHING_STARTED);
            return;
        }

        switch (intentMode) {
            case HELP:
                printInstanceTypeHelp(module, client, config);
                break;
            case LIST:
                ListIntent listIntent = module.getListIntent(client, config);
                if (config.getId() == null || config.getId().isEmpty()) {
                    LOG.info(listIntent.toString());
                } else {
                    LOG.info(listIntent.toDetailString(config.getId()));
                }
                break;
            case VALIDATE:
                if (!validator.validateProviderTypes(client)) {
                    LOG.error(Constant.ABORT_WITH_NOTHING_STARTED);
                }
                ValidateIntent validate = module.getValidateIntent(client, config);
                if (validate.validate()) {
                    LOG.info(I, "You can now start your cluster.");
                } else {
                    LOG.error(validate.getValidateResponse());
                    LOG.error("There were one or more errors. Please adjust your configuration.");
                }
                break;
            case CREATE:
                if (!validator.validateProviderTypes(client)) {
                    LOG.error(Constant.ABORT_WITH_NOTHING_STARTED);
                }
                if (module.getValidateIntent(client, config).validate()) {
                    CreateIntent create = new CreateIntent(module, config, client);
                    create.create();
                    Status status = DataBase.getDataBase().status.get(create.getClusterId());
                    if (status.code == Status.CODE.Error) {
                        LOG.error(status.msg);
                    }

                } else {
                    LOG.error("There were one or more errors. Please adjust your configuration.");
                }
                break;
//            case PREPARE:
//                if (!validator.validateProviderTypes(client)) {
//                    LOG.error(Constant.ABORT_WITH_NOTHING_STARTED);
//                }
//                CreateCluster cluster = module.getCreateIntent(client, config);
//                if (runCreateIntent(module, config, client, cluster, true)) {
//                    module.getPrepareIntent(client, config).prepare(cluster.getMasterInstance(),
//                            cluster.getWorkerInstances());
//                    module.getTerminateIntent(client, config).terminate();
//                }
//                break;
            case TERMINATE:
                module.getTerminateIntent(client, config).terminate();
                break;
            case CLOUD9:
                LOG.warn("Command-line option --cloud9 is deprecated. Please use --ide instead.");
            case IDE:
                // load private key
                try {
                    config.getClusterKeyPair().setName(CreateCluster.PREFIX+config.getId());
                    config.getClusterKeyPair().load();
                    new IdeIntent(module, client, config).start();
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
