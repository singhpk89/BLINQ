package com.blinq.utils;


import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;
import android.content.Context;

public class ManageKeyguard {

    private static KeyguardManager myKM = null;
    private static KeyguardLock myKL = null;

    public static synchronized void initialize(Context context) {
        if (myKM == null) {
            myKM = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        }
    }

    public static synchronized boolean inKeyguardRestrictedInputMode() {
        if (myKM != null) {
            return myKM.inKeyguardRestrictedInputMode();
        }
        return false;
    }

    public static synchronized void reenableKeyguard() {
        if (myKM != null) {
            if (myKL != null) {
                myKL.reenableKeyguard();
                myKL = null;
            }
        }
    }
}