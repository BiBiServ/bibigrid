package de.unibi.cebitec.bibigrid.core.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.nodes.Tag;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class YamlInterpreter {
    private static final Logger LOG = LoggerFactory.getLogger(YamlInterpreter.class);
    /**
     * Uses stream to write map on remote.
     * @param stream OutputStream to remote instance
     * @param map (yml) file content
     */
    public static void writeToOutputStream(OutputStream stream, Object map) {
        try (OutputStreamWriter writer = new OutputStreamWriter(stream, StandardCharsets.UTF_8)) {
            if (map instanceof Map) {
                writer.write(new Yaml().dumpAsMap(map));
            } else {
                writer.write(new Yaml().dumpAs(map, Tag.SEQ, DumperOptions.FlowStyle.BLOCK));
            }
        } catch (IOException e) {
            LOG.error("Could not successfully write to remote.");
            e.printStackTrace();
        }
    }

    /**
     * Parses yaml file from remote into map.
     * @param stream InputStream
     * @return map of Yaml syntax
     */
    public static Map<String, Object> readFromInputStream(InputStream stream) {
        StringBuilder yamlContent = new StringBuilder();
        try (Reader reader = new BufferedReader(new InputStreamReader(stream))) {
            int c;
            while ((c = reader.read()) != -1) {
                yamlContent.append((char) c);
            }
        } catch (IOException e) {
            LOG.error("Could not successfully read from remote.");
            e.printStackTrace();
        }
        Yaml yaml = new Yaml();
        return yaml.load(yamlContent.toString());
    }

    /**
     * Checks with RegEx if ip could be valid address.
     * @param file name of file to be checked
     * @return true, if file contains valid ipv4-address
     */
    public static boolean isIPAddressFile(String file) {
        String ip = file.replace(".yml", "");
        final String IPV4_REGEX =
                "^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
                        "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
                        "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
                        "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";
        final Pattern IPV4_PATTERN = Pattern.compile(IPV4_REGEX);
        Matcher matcher = IPV4_PATTERN.matcher(ip);
        return matcher.matches();
    }
}
