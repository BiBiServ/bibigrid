package de.unibi.cebitec.bibigrid;

import com.amazonaws.services.ec2.model.InstanceType;
import de.unibi.cebitec.bibigrid.ctrl.*;
import de.unibi.cebitec.bibigrid.exception.IntentNotConfiguredException;
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

    public static OptionGroup getRulesToOptions(){

        RuleBuilder ruleBuild = new RuleBuilder();
        TparamGroup ruleSet = ruleBuild.getRules();
        OptionGroup ruleOptions = new OptionGroup();

        for (Object ob : ruleSet.getParamrefOrParamGroupref()){

            Tparam tp = (Tparam) ob;
            boolean hasArg;

            hasArg = tp.getType() != null;

            ruleOptions.addOption(new Option(tp.getId(),tp.getOption(),hasArg,tp.getShortDescription().get(0).getValue()));
        }

        return ruleOptions;
    }
    
    public static Options getCMDLineOptions(OptionGroup optgrp, OptionGroup optfrorules) {
        Options cmdLineOptions = new Options();
        cmdLineOptions
                .addOptionGroup(optgrp)
                .addOptionGroup(optfrorules);

        return cmdLineOptions;
    }

    public static void main(String[] args) {

        CommandLineParser cli = new DefaultParser();
        OptionGroup intentOptions = getCMDLineOptionGroup();
        OptionGroup ruleOptions = getRulesToOptions();
        Options cmdLineOptions = getCMDLineOptions(intentOptions, ruleOptions);

        
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
