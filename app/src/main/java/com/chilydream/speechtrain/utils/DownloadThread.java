package com.chilydream.speechtrain.utils;

import android.util.Log;

import com.alibaba.fastjson.JSONObject;
import com.chilydream.speechtrain.train.AudioList;
import com.chilydream.speechtrain.train.AudioUnit;
import com.chilydream.speechtrain.train.TrainOption;

import java.io.File;

public class DownloadThread extends Thread {
    static final String TAG = "DownloadThreadTag";
    AudioList audioList;
    TrainOption trainOption;

    public DownloadThread() {
        audioList = AudioList.getAudioList();
        trainOption = TrainOption.getTrainOption();
    }

    boolean checkFile(AudioUnit audioUnit, Long fileSize) {
        File saveFile = audioUnit.getSaveFile();

        if (!saveFile.exists() || !saveFile.isFile()) {
            return false;
        } else return saveFile.length() == fileSize;
    }

    private void downloadFile(AudioUnit audioUnit) {
        NetConnection netConnection = new NetConnection(ConfigConsts.API_AUDIO_DOWNLOAD);
        JSONObject downloadJson = UserMessage.getIdJson();
        File saveFile = audioUnit.getSaveFile();
        downloadJson.put("corpus_id", audioUnit.getCorpusId());
        netConnection.postJson(downloadJson);
        netConnection.downloadFile(saveFile.getAbsolutePath());
        // todo: 如果文件存在，进行下载有没有问题？
        Log.d(TAG, "downloadFile: 已下载音频");
        if (TrainOption.ifShowGraph()) {
            Log.d(TAG, "downloadFile: 已下载图片");
            netConnection = new NetConnection(ConfigConsts.API_GRAPH_DOWNLOAD);
            downloadJson = UserMessage.getIdJson();
            File imgSaveFile = audioUnit.getImgSaveFile();
            downloadJson.put("corpus_id", audioUnit.getCorpusId());
            netConnection.postJson(downloadJson);
            netConnection.downloadFile(imgSaveFile.getAbsolutePath());
        }
    }

    @Override
    public void run() {
        super.run();
        for (AudioUnit unit:audioList.getUnitList()) {
//            while (!checkFile(unit, unit.getFileSize())) {
//                downloadFile(unit);
//                // todo: 是否需要考虑网络出现波动时，这里有可能频繁重新下载导致的卡顿？
//            }
            while (!unit.isDownload()) {
                downloadFile(unit);
                // todo: 是否需要考虑网络出现波动时，这里有可能频繁重新下载导致的卡顿？
            }
        }
    }
}
