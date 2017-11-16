package de.unibi.cebitec.bibigrid;

import de.unibi.cebitec.bibigrid.intents.CreateCluster;
import de.unibi.cebitec.bibigrid.intents.TerminateIntent;
import de.unibi.cebitec.bibigrid.intents.ValidateIntent;
import de.unibi.cebitec.bibigrid.model.IntentMode;
import de.unibi.cebitec.bibigrid.model.ProviderModule;
import de.unibi.cebitec.bibigrid.model.exceptions.ConfigurationException;
import de.unibi.cebitec.bibigrid.util.DefaultPropertiesFile;
import de.unibi.cebitec.bibigrid.util.RuleBuilder;
import de.unibi.cebitec.bibigrid.util.VerboseOutputFilter;

import java.net.URL;
import java.net.URLDecoder;
import java.util.Properties;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import de.unibi.techfak.bibiserv.cms.Tparam;
import de.unibi.techfak.bibiserv.cms.TparamGroup;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Startup/Main class of BiBiGrid.
 *
 * @author Jan Krueger - jkrueger(at)cebitec.uni-bielefeld.de
 */
public class StartUp {
    public static final Logger LOG = LoggerFactory.getLogger(StartUp.class);
    private static final String ABORT_WITH_NOTHING_STARTED = "Aborting operation. No instances started/terminated.";
    private static final String ABORT_WITH_INSTANCES_RUNNING = "Aborting operation. Instances already running. " +
            "I will try to shut them down but in case of an error they might remain running. Please check manually " +
            "afterwards.";

    private static OptionGroup getCMDLineOptionGroup() {
        OptionGroup intentOptions = new OptionGroup();
        intentOptions.setRequired(true);
        Option terminate = new Option(IntentMode.TERMINATE.getShortParam(), IntentMode.TERMINATE.getLongParam(),
                true, "terminate running cluster");
        terminate.setArgName("cluster-id");
        intentOptions
                .addOption(new Option(IntentMode.VERSION.getShortParam(), IntentMode.VERSION.getLongParam(),
                        false, "version"))
                .addOption(new Option(IntentMode.HELP.getShortParam(), IntentMode.HELP.getLongParam(),
                        false, "help"))
                .addOption(new Option(IntentMode.CREATE.getShortParam(), IntentMode.CREATE.getLongParam(),
                        false, "create cluster"))
                .addOption(new Option(IntentMode.LIST.getShortParam(), IntentMode.LIST.getLongParam(),
                        false, "list running clusters"))
                .addOption(new Option(IntentMode.VALIDATE.getShortParam(), IntentMode.VALIDATE.getLongParam(),
                        false, "check config file"))
                .addOption(terminate);
        return intentOptions;
    }

    private static Options getRulesToOptions() {
        RuleBuilder ruleBuild = new RuleBuilder();
        TparamGroup ruleSet = ruleBuild.getRules();
        Options ruleOptions = new Options();
        for (Object ob : ruleSet.getParamrefOrParamGroupref()) {
            Tparam tp = (Tparam) ob;
            boolean hasArg;
            hasArg = tp.getType() != null;
            ruleOptions.addOption(new Option(tp.getId(), tp.getOption(), hasArg, tp.getShortDescription().get(0).getValue()));
        }
        return ruleOptions;
    }

    public static void main(String[] args) {
        CommandLineParser cli = new DefaultParser();
        OptionGroup intentOptions = getCMDLineOptionGroup();
        Options cmdLineOptions = getRulesToOptions();
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
                    printHelp(cmdLineOptions);
                    break;
                case CREATE:
                case LIST:
                case TERMINATE:
                case VALIDATE:
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
        } catch (Exception e) {
            LOG.error("Version info could not be read.");
        }
    }

    private static void printHelp(Options cmdLineOptions) {
        HelpFormatter help = new HelpFormatter();
        String header = ""; //TODO: infotext (auf default props hinweisen); instanzgroessen auflisten
        String footer = "";
        help.printHelp("bibigrid --create|--list|--terminate|--check [...]", header, cmdLineOptions, footer);
    }

    private static void runIntent(CommandLine commandLine, IntentMode intentMode) {
        DefaultPropertiesFile defaultPropertiesFile = new DefaultPropertiesFile(commandLine);
        String providerMode = parseProviderMode(commandLine, defaultPropertiesFile);
        if (providerMode == null) {
            LOG.error(StartUp.ABORT_WITH_INSTANCES_RUNNING);
            return;
        }
        ProviderModule module = Provider.getInstance().getProviderModule(providerMode);
        if (module == null) {
            LOG.error(ABORT_WITH_NOTHING_STARTED);
            return;
        }
        CommandLineValidator validator = module.getCommandLineValidator(commandLine, defaultPropertiesFile, intentMode);
        if (validator.validate(providerMode)) {
            switch (intentMode) {
                case LIST:
                    LOG.info(module.getListIntent(validator.getConfig()).toString());
                    break;
                case VALIDATE:
                    ValidateIntent intent = module.getValidateIntent(validator.getConfig());
                    if (intent != null) {
                        intent.validate();
                    }
                    break;
                case CREATE:
                    try {
                        CreateCluster cluster = module.getCreateIntent(validator.getConfig());
                        boolean success = cluster
                                .createClusterEnvironment()
                                .createVPC()
                                .createSubnet()
                                .createSecurityGroup()
                                .createPlacementGroup()
                                .configureClusterMasterInstance()
                                .configureClusterSlaveInstance()
                                .launchClusterInstances();
                        if (!success) {
                            LOG.error(StartUp.ABORT_WITH_INSTANCES_RUNNING);
                            TerminateIntent cleanupIntent = module.getTerminateIntent(validator.getConfig());
                            cleanupIntent.terminate();
                        }
                    } catch (ConfigurationException ex) {
                        LOG.error("Exception : {}", ex.getMessage());
                    }
                    break;
                case TERMINATE:
                    module.getTerminateIntent(validator.getConfig()).terminate();
                    break;
            }
        } else {
            LOG.error(ABORT_WITH_NOTHING_STARTED);
        }
    }

    private static String parseProviderMode(CommandLine commandLine, DefaultPropertiesFile defaultPropertiesFile) {
        final String modeParameter = "mode";
        Properties defaults = defaultPropertiesFile.getDefaultProperties();
        try {
            if (commandLine.hasOption(modeParameter) || defaults.containsKey(modeParameter)) {
                return commandLine.getOptionValue(modeParameter, defaults.getProperty(modeParameter)).trim();
            }
        } catch (IllegalArgumentException iae) {
            LOG.error("No suitable mode found. Exit");
        }
        return null;
    }
}
