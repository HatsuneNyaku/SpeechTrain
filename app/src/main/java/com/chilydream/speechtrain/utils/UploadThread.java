package com.chilydream.speechtrain.utils;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;

import com.alibaba.fastjson.JSONObject;
import com.chilydream.speechtrain.train.AudioUnit;
import com.chilydream.speechtrain.train.BasicAgent;
import com.chilydream.speechtrain.train.CenterHandler;
import com.chilydream.speechtrain.train.TrainAgent;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class UploadThread extends Thread{
    private static final String TAG = "UploadThread";
    public static final int COMMAND_UPLOAD = 1;
    public static final int COMMAND_FINISH_TRAIN = 2;
    Handler uploadHandler;
    WeakReference<BasicAgent> agentWeakReference;

    public UploadThread(BasicAgent agent) {
        agentWeakReference = new WeakReference<>(agent);
    }

    private static class UploadHandler extends Handler {
        WeakReference<BasicAgent> weakReference;
        int upload_count;
        boolean finish_train;

        UploadHandler(BasicAgent agent) {
            weakReference = new WeakReference<>(agent);
            upload_count = 0;
            finish_train = false;
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            if (msg.what == COMMAND_UPLOAD) {
                upload_count += 1;

                NetConnection netConnection = new NetConnection(ConfigConsts.API_UPLOAD);
                AudioUnit unit = (AudioUnit) msg.obj;
                File record_file = unit.getRecordFile();
                InputStream input_stream;
                byte[] byte_wav;

                try {
                    input_stream = new FileInputStream(record_file);
                    byte_wav = new byte[(int) record_file.length()];
                    input_stream.read(byte_wav);
                    input_stream.close();
                    netConnection.postFile(record_file.getName(), byte_wav);
                } catch (IOException e) {
                    Log.e(TAG, "handleMessage: " + e.toString());
                }

                upload_count -= 1;
                if (upload_count==0 && finish_train) {
                    BasicAgent agent = weakReference.get();
                    agent.centerHandler.sendEmptyMessage(CenterHandler.STAGE_ALL_FINISH);
                }
                // todo: 这里是否需要判断有无上传成功？
            } else if (msg.what==COMMAND_FINISH_TRAIN) {
                finish_train = true;
                if (upload_count==0) {
                    BasicAgent agent = weakReference.get();
                    agent.centerHandler.sendEmptyMessage(CenterHandler.STAGE_ALL_FINISH);
                }
            }
        }
    }

    @Override
    public void run() {
        super.run();
        Looper.prepare();
        uploadHandler = new UploadHandler(agentWeakReference.get());
        // todo: Handler是必须要在两个loop命令中间定义才起作用吗
        Looper.loop();
        Log.d(TAG, "run: upload thread run finish.");
    }

    public Handler getUploadHandler() {
        return uploadHandler;
    }
}
