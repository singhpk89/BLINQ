package com.blinq.utils;

import org.apache.log4j.Logger;

import de.mindpipe.android.logging.log4j.LogConfigurator;

/**
 * helper class to configure Log4j settings.
 */
public class Log4jHelper {

    private final static LogConfigurator logConfigurator = new LogConfigurator();

    public static void configure(String fileName, String filePattern,
                                 int maxBackupSize, long maxFileSize) {

        // set the name of the log file
        logConfigurator.setFileName(fileName);
        // set output format of the log line
        logConfigurator.setFilePattern(filePattern);
        // Maximum number of backed up log files
        logConfigurator.setMaxBackupSize(maxBackupSize);
        // Maximum size of log file until rolling
        logConfigurator.setMaxFileSize(maxFileSize);

        // configure
        logConfigurator.configure();

    }

    public static Logger getLogger(String name) {
        Logger logger = Logger.getLogger(name);
        return logger;
    }

}