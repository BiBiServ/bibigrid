package de.unibi.cebitec.bibigrid.util;

import org.apache.commons.cli.CommandLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static de.unibi.cebitec.bibigrid.util.VerboseOutputFilter.V;

/**
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
public final class DefaultPropertiesFile {
    public static final Logger LOG = LoggerFactory.getLogger(DefaultPropertiesFile.class);
    private static final String DEFAULT_DIRNAME = System.getProperty("user.home");
    private static final String DEFAULT_FILENAME = ".bibigrid.properties";
    private static final String PROPERTIES_FILEPATH_PARAMETER = "o";

    private Path propertiesFilePath;
    private boolean isAlternativeFilepath;
    private Properties defaultProperties;

    public DefaultPropertiesFile(CommandLine commandLine) {
        if (commandLine.hasOption(PROPERTIES_FILEPATH_PARAMETER)) {
            String path = commandLine.getOptionValue(PROPERTIES_FILEPATH_PARAMETER);
            Path newPath = FileSystems.getDefault().getPath(path);
            if (Files.isReadable(newPath)) {
                propertiesFilePath = newPath;
                isAlternativeFilepath = true;
                LOG.info("Alternative config file {} will be used.", propertiesFilePath.toString());
            } else {
                LOG.error("Alternative config ({}) file is not readable. Try to use default.", newPath.toString());
            }
        }
        if (propertiesFilePath == null) {
            propertiesFilePath = FileSystems.getDefault().getPath(DEFAULT_DIRNAME, DEFAULT_FILENAME);
        }
        if (Files.exists(propertiesFilePath)) {
            LOG.info(V, "Reading default options from properties file at '{}'.", propertiesFilePath);
        } else {
            LOG.info("No properties file for default options found ({}). Using command line parameters only.",
                    propertiesFilePath);
        }
        defaultProperties = new Properties();
        try {
            if (propertiesFilePath != null) {
                defaultProperties.load(Files.newInputStream(propertiesFilePath));
            }
        } catch (IOException ignored) {
        }
    }

    public boolean isAlternativeFilepath() {
        return isAlternativeFilepath;
    }

    public Path getPropertiesFilePath() {
        return propertiesFilePath;
    }

    public Properties getDefaultProperties() {
        return defaultProperties;
    }
}
