package de.unibi.cebitec.bibigrid;

import de.unibi.cebitec.bibigrid.core.Constant;
import de.unibi.cebitec.bibigrid.core.model.IntentMode;
import org.apache.commons.cli.*;

import static de.unibi.cebitec.bibigrid.core.intents.LoadClusterConfigurationIntent.LOG;
import static de.unibi.cebitec.bibigrid.core.model.IntentMode.*;
import static de.unibi.cebitec.bibigrid.core.model.IntentMode.LIST;

/**
 * Parses commandline and contains options.
 *
 * @author tdilger - t.dilger(at)uni-bielefeld.de
 */
public class CommandLineInterpreter {
    private CommandLine cl;
    private Options cmdLineOptions;
    private OptionGroup intentOptions;
    private IntentMode intentMode;

    CommandLineInterpreter(String[] parameter) {
        CommandLineParser clParser = new DefaultParser();
        addOptions();
        try {
            cl = clParser.parse(cmdLineOptions, parameter);

            intentMode = IntentMode.fromString(intentOptions.getSelected());
        } catch (ParseException pe) {
            LOG.error("Error while parsing the commandline arguments: {}", pe.getMessage());
            LOG.error(Constant.ABORT_WITH_NOTHING_STARTED);
        }
    }

    /**
     * Adds terminal behaviour and intent options.
     */
    private void addOptions() {
        intentOptions = IntentOptions.getIntentOptions();
        cmdLineOptions = new Options()
                .addOption(new Option("v", "verbose", false,"More verbose output"))
                .addOption(new Option("o","config",true,"Path to JSON configuration file"))
                .addOption(new Option("d","debug",false,"Don't shut down cluster in the case of a configuration error."))
                .addOption(new Option("m","mode",true,"One of "+String.join(",", Provider.getInstance().getProviderNames())))
                .addOptionGroup(intentOptions);
    }

    public static CommandLineInterpreter parseCommandLine(String[] parameter) {
        return new CommandLineInterpreter(parameter);
    }

    public boolean isMode(String mode) {
        return cl.hasOption(mode);
    }

    public Options getCmdLineOptions() {
        return cmdLineOptions;
    }

    public IntentMode getIntentMode() {
        return intentMode;
    }

    public String getOptionValue(String option) {
        return cl.getOptionValue(option);
    }

    public String[] getOptionValues(String option) {
        return cl.getOptionValues(option);
    }
}

/**
 * Additional intent options Version.
 * Contains Help, Validate, Create, Scale Up, Scale Down, Terminate, IDE and List
 */
class IntentOptions extends OptionGroup {
    private static final String CID = "cluster-id";
    private static final String SCALE = CID + " worker-Batch count";
    private static final int SCALE_ARGS = 3; // cluster-id, workerBatch, count
    private static final boolean REQUIRED = true;

    private IntentOptions() {
        super();
        this.setRequired(REQUIRED);

        Option version = new Option(VERSION.getShortParam(), VERSION.getLongParam(),
                false, VERSION.getDescription());
        this.addOption(version);

        Option help = new Option(HELP.getShortParam(), HELP.getLongParam(),
                false, HELP.getDescription());
        this.addOption(help);

        Option validate = new Option(VALIDATE.getShortParam(), VALIDATE.getLongParam(),
                false, VALIDATE.getDescription());
        this.addOption(validate);

        Option create = new Option(CREATE.getShortParam(), CREATE.getLongParam(),
                false, CREATE.getDescription());
        this.addOption(create);

        Option terminate = new Option(TERMINATE.getShortParam(), TERMINATE.getLongParam(),
                true, TERMINATE.getDescription());
        terminate.setArgs(Option.UNLIMITED_VALUES);
        terminate.setArgName(CID);
        this.addOption(terminate);

        Option upscale = new Option(SCALE_UP.getShortParam(), SCALE_UP.getLongParam(),
                true, SCALE_UP.getDescription());
        upscale.setArgs(SCALE_ARGS);
        upscale.setArgName(SCALE);
        this.addOption(upscale);

        Option downscale = new Option(SCALE_DOWN.getShortParam(), SCALE_DOWN.getLongParam(),
                true, SCALE_DOWN.getDescription());
        downscale.setArgs(SCALE_ARGS);
        downscale.setArgName(SCALE);
        this.addOption(downscale);

        Option ide = new Option(IDE.getShortParam(), IDE.getLongParam(),
                true, IDE.getDescription());
        ide.setArgName(CID);
        this.addOption(ide);

        Option list = new Option(LIST.getShortParam(), LIST.getLongParam(),
                true, LIST.getDescription());
        list.setOptionalArg(true);
        list.setArgName(CID);
        this.addOption(list);
    }

    static OptionGroup getIntentOptions() {
        return new IntentOptions();
    }
}

