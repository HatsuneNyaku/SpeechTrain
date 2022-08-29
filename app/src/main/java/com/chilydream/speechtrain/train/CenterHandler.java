package com.chilydream.speechtrain.train;

import android.os.Handler;

import java.lang.ref.WeakReference;

public class CenterHandler extends Handler {
    public static final int STAGE_START = 1;
    public static final int STAGE_NEXT = 2;
    public static final int STAGE_ALL_FINISH = 3;
    final WeakReference<BasicAgent> weakReference;

    public CenterHandler(BasicAgent agent) {
        this.weakReference = new WeakReference<>(agent);
    }
}
