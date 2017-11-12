package de.unibi.cebitec.bibigrid;

import de.unibi.cebitec.bibigrid.ctrl.*;
import de.unibi.cebitec.bibigrid.model.exceptions.IntentNotConfiguredException;
import de.unibi.cebitec.bibigrid.util.RuleBuilder;
import de.unibi.cebitec.bibigrid.util.VerboseOutputFilter;

import java.net.URL;
import java.net.URLDecoder;
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
 * @author Jan Krueger - jkrueger(at)cebitec.un-bielefel.de
 */
public class StartUp {
    public static final Logger log = LoggerFactory.getLogger(StartUp.class);
    public static final String ABORT_WITH_NOTHING_STARTED = "Aborting operation. No instances started/terminated.";
    public static final String ABORT_WITH_INSTANCES_RUNNING = "Aborting operation. Instances already running. " +
            "I will try to shut them down but in case of an error they might remain running. Please check manually " +
            "afterwards.";

    private static OptionGroup getCMDLineOptionGroup() {
        OptionGroup intentOptions = new OptionGroup();
        intentOptions.setRequired(true);
        Option terminate = new Option("t", "terminate", true, "terminate running cluster");
        terminate.setArgName("cluster-id");
        intentOptions
                .addOption(new Option("V", "version", false, "version"))
                .addOption(new Option("h", "help", false, "help"))
                .addOption(new Option("c", "create", false, "create cluster"))
                .addOption(new Option("l", "list", false, "list running clusters"))
                .addOption(new Option("ch", "check", false, "check config file"))
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
            switch (intentOptions.getSelected()) {
                case "V":
                    printVersionInfo();
                    break;
                case "h":
                    printHelp(cmdLineOptions);
                    break;
                case "c":
                    runIntent(cl, new CreateIntent());
                    break;
                case "l":
                    runIntent(cl, new ListIntent());
                    break;
                case "t":
                    runIntent(cl, new TerminateIntent());
                    break;
                case "ch":
                    runIntent(cl, new ValidationIntent());
                    break;
            }
        } catch (ParseException pe) {
            log.error("Error while parsing the commandline arguments: {}", pe.getMessage());
            log.error(ABORT_WITH_NOTHING_STARTED);
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
            log.error("Version info could not be read.");
        }
    }

    private static void printHelp(Options cmdLineOptions) {
        HelpFormatter help = new HelpFormatter();
        String header = ""; //TODO: infotext (auf default props hinweisen); instanzgroessen auflisten
        String footer = "";
        help.printHelp("bibigrid --create|--list|--terminate|--check [...]", header, cmdLineOptions, footer);
    }

    private static void runIntent(CommandLine cl, Intent intent) {
        CommandLineValidator validator = new CommandLineValidator(cl, intent);
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
}
