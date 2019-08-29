package de.unibi.cebitec.bibigrid.core.util;

import de.unibi.cebitec.bibigrid.core.model.Configuration;
import de.unibi.cebitec.bibigrid.core.model.exceptions.ConfigurationException;
import org.apache.commons.cli.CommandLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import static de.unibi.cebitec.bibigrid.core.util.VerboseOutputFilter.V;

/**
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
public final class ConfigurationFile {
    private static final Logger LOG = LoggerFactory.getLogger(ConfigurationFile.class);
    private static final String DEFAULT_DIRNAME = System.getProperty("user.home");
    private static final String DEFAULT_FILENAME = ".bibigrid.yml";
    private static final String PROPERTIES_FILEPATH_PARAMETER = "o";

    private Path propertiesFilePath;
    private boolean isAlternativeFilepath;
    private String propertiesMode;

    public ConfigurationFile(CommandLine commandLine) {
        Path defaultPropertiesFilePath = Paths.get(DEFAULT_DIRNAME, DEFAULT_FILENAME);
        if (commandLine.hasOption(PROPERTIES_FILEPATH_PARAMETER)) {
            String path = commandLine.getOptionValue(PROPERTIES_FILEPATH_PARAMETER);
            Path newPath = Paths.get(path);
            if (Files.isReadable(newPath)) {
                propertiesFilePath = newPath;
                isAlternativeFilepath = true;
                LOG.info("Using alternative config file: '{}'.", propertiesFilePath.toString());
            } else {
                LOG.error("Alternative config ({}) file is not readable. Falling back to default: '{}'", newPath.toString(), defaultPropertiesFilePath.toString());
            }
        }
        if (propertiesFilePath == null) {
            propertiesFilePath = defaultPropertiesFilePath;
        }
        if (Files.exists(propertiesFilePath)) {
            LOG.info(V, "Reading options from properties file at '{}'.", propertiesFilePath);
            try {
                // In order to load the yaml file directly into the provider Configuration we have to peek the mode
                Map<String, String> yamlMap = new Yaml().load(new FileInputStream(propertiesFilePath.toFile()));
                propertiesMode = yamlMap.getOrDefault("mode", null);
            } catch (FileNotFoundException e) {
                LOG.error("Failed to load mode parameter from properties file.");
            }
        } else {
            LOG.info("No properties file found at '{}'. Using command line parameters only.",
                    propertiesFilePath);
        }
    }

    public boolean isAlternativeFilepath() {
        return isAlternativeFilepath;
    }

    public Path getPropertiesFilePath() {
        return propertiesFilePath;
    }

    public String getPropertiesMode() {
        return propertiesMode;
    }

    public Configuration loadConfiguration(Class<? extends Configuration> configurationClass)
            throws ConfigurationException {
        if (Files.exists(propertiesFilePath)) {
            try {
                return new Yaml().loadAs(new FileInputStream(propertiesFilePath.toFile()), configurationClass);
            } catch (FileNotFoundException e) {
                throw new ConfigurationException("Failed to load properties file.", e);
            } catch (YAMLException e) {
                throw new ConfigurationException("Failed to parse configuration file. "+e.getMessage(), e);
            }
        }
        try {
            return configurationClass.getConstructor().newInstance();
        } catch (Exception e) {
            throw new ConfigurationException("Failed to instantiate empty configuration.", e);
        }
    }
}
