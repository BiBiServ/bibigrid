package de.unibi.cebitec.bibigrid.core.util;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JSchLogger implements com.jcraft.jsch.Logger {
    private static final Logger LOG = LoggerFactory.getLogger(JSchLogger.class);
    private static final Map<Integer, String> levelNameMap = new HashMap<>();

    static {
        levelNameMap.put(DEBUG, "DEBUG:");
        levelNameMap.put(INFO, "INFO:");
        levelNameMap.put(WARN, "WARN:");
        levelNameMap.put(ERROR, "ERROR:");
        levelNameMap.put(FATAL, "FATAL:");
    }

    @Override
    public boolean isEnabled(int level) {
        return true;
    }

    @Override
    public void log(int level, String message) {
        LOG.debug("JSCH {} {}", levelNameMap.get(level), message);
    }
}