package de.unibi.cebitec.bibigrid.googlecloud;

import com.google.api.client.http.HttpTransport;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class HttpRequestLogHandler extends Handler {
    static void attachToCloudHttpTransport() {
        Logger logger = Logger.getLogger(HttpTransport.class.getName());
        // Check if already attached
        for (Handler handler : logger.getHandlers()) {
            if (handler.getClass() == HttpRequestLogHandler.class) {
                return;
            }
        }
        logger.setLevel(Level.CONFIG);
        logger.addHandler(new HttpRequestLogHandler());
    }

    @Override
    public void publish(LogRecord record) {
        // Default ConsoleHandler will print >= INFO to System.err.
        if (record.getLevel().intValue() < Level.INFO.intValue()) {
            System.out.println(record.getMessage());
        }
    }

    @Override
    public void flush() {

    }

    @Override
    public void close() throws SecurityException {

    }
}
