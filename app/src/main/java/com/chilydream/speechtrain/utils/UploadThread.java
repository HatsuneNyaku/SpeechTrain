package com.chilydream.speechtrain.utils;

import android.util.Log;

import com.alibaba.fastjson.JSONObject;
import com.chilydream.speechtrain.train.AudioUnit;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class UploadThread extends Thread{
    private static final String TAG = "UploadThread";
    static Queue<AudioUnit> sUploadQueue;
    int uploadFlag;

    public UploadThread() {
        sUploadQueue = new LinkedList<>();
        uploadFlag = 1; // 0表示不需要上传了，1表示需要上传
    }

    public void finishTrain() {
        uploadFlag = 0;
    }

    @Override
    public void run() {
        AudioUnit unit;
        NetConnection netConnection;
        InputStream input_stream;
        byte[] byte_wav;
        File audio_file;

        super.run();
        while (uploadFlag==1 || !sUploadQueue.isEmpty()) {
            if (sUploadQueue.isEmpty()) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Log.e(TAG, "run: "+e.toString());
                }
                continue;
            }

            unit = sUploadQueue.poll();
            netConnection = new NetConnection(ConfigConsts.API_UPLOAD);
            assert unit != null;
            audio_file = unit.getRecordFile();
            try {
                input_stream = new FileInputStream(audio_file);
                byte_wav = new byte[(int) audio_file.length()];
                input_stream.read(byte_wav);
                input_stream.close();
                netConnection.postFile(audio_file.getName(), byte_wav);
            } catch (IOException e) {
                Log.e(TAG, "run: "+e.toString());
            }
        }
    }

    public static void addUploadAudio(AudioUnit unit) {
        sUploadQueue.add(unit);
    }
}
