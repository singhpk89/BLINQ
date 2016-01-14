package com.blinq.utils;

import org.apache.log4j.Logger;

/**
 * Encapsulate Log4j functionalities to override android Log utility.
 *
 * @author Johan Hansson.
 */
public class Log {

    public static void v(String tag, String message) {
        Logger log = Logger.getLogger(tag);
        log.debug(message);
    }

    public static void e(String tag, String message) {
        Logger log = Logger.getLogger(tag);
        log.error(message);
    }

    public static void i(String tag, String message) {
        Logger log = Logger.getLogger(tag);
        log.info(message);
    }

    public static void d(String tag, String message) {
        Logger log = Logger.getLogger(tag);
        log.debug(message);
    }

    public static void e(String tag, String message, Throwable ex) {
        Logger log = Logger.getLogger(tag);
        log.error(message, ex);
    }

    public static void w(String tag, String message) {
        Logger log = Logger.getLogger(tag);
        log.warn(message);
    }
}
