/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.unibi.cebitec.bibigrid.util;

import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author chenke
 */
public class JSchLogger implements com.jcraft.jsch.Logger {

    public static final Logger log = LoggerFactory.getLogger(JSchLogger.class);
    static final Map<Integer, String> name = new HashMap<>();

    static {
        name.put(new Integer(DEBUG), "DEBUG:");
        name.put(new Integer(INFO), "INFO:");
        name.put(new Integer(WARN), "WARN:");
        name.put(new Integer(ERROR), "ERROR:");
        name.put(new Integer(FATAL), "FATAL:");
    }

    @Override
    public boolean isEnabled(int level) {
        return true;
    }

    @Override
    public void log(int level, String message) {
        log.debug("JSCH {} {}", name.get(new Integer(level)), message);
    }
}