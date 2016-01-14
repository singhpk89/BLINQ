package com.blinq.utils;


public class Debug {

    private static final boolean DEBUG = true;

    public static final void delayExecution(int delay) {
        if (!DEBUG)
            return;
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {}
    }

    public static final void startRecordingTrace() {
        if (!DEBUG)
            return;
        android.os.Debug.startMethodTracing();
    }

    public static final void stopRecordingTrace() {
        if (!DEBUG)
            return;
        android.os.Debug.stopMethodTracing();
    }

    public static final void waitForDebugger() {
        if (!DEBUG)
            return;
        android.os.Debug.waitForDebugger();
        doNothing();
    }

    private static void doNothing() {}

}