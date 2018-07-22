package com.example.kyle0.babyready;

import android.app.Activity;

public final class Threadings {
    private Threadings() {
        // Empty private constructor
    }

    public static void runInBackgroundThread(Runnable runnable) {
        Thread thread = new Thread(runnable);
        thread.start();
    }

    public static void runInMainThread(Activity activity, Runnable runnable) {
        activity.runOnUiThread(runnable);
    }
}
