package com.chilydream.speechtrain.train;

import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.chilydream.speechtrain.utils.DownloadThread;

import java.util.ArrayList;
import java.util.List;

public class AudioList {
    private static final String TAG = "AudioListTag";
    static AudioList sAudioList = null;
    int audioNumber;
    int finishedNumber;
    int trainCursor;
    String sectionName;
    List<AudioUnit> unitList;
    DownloadThread downloadThread;

    private AudioList() {
        unitList = new ArrayList<>();
    }

    public static void initAudioList(JSONObject dataJson) {
        sAudioList = new AudioList();
        sAudioList.sectionName = dataJson.getString("section_name");

        JSONObject unit_json;
        List<String> audioStrList = JSON.parseArray(dataJson.getString("audio_list"), String.class);
        String corpus_id, sentence_content, file_size, finish_status;

        sAudioList.audioNumber = Integer.parseInt(dataJson.getString("corpus_number"));
        for (int i = 0; i < sAudioList.audioNumber; i += 1) {
            unit_json = JSON.parseObject(audioStrList.get(i));
            corpus_id = unit_json.getString("corpus_id");
            sentence_content = unit_json.getString("content_text");
            file_size = unit_json.getString("file_size");
            finish_status = unit_json.getString("finish_status");
            sAudioList.unitList.add(
                    new AudioUnit(corpus_id, sentence_content, file_size, finish_status)
            );


            if (finish_status.equals("finished")) {
                sAudioList.finishedNumber += 1;
            }
        }
        sAudioList.nextCursor();

        sAudioList.downloadThread = new DownloadThread();
        sAudioList.downloadThread.start();
    }

    public static AudioList getAudioList() {
        return sAudioList;
    }

    public List<AudioUnit> getUnitList() {
        return unitList;
    }

    public void nextCursor() {
        sAudioList.finishedNumber = 0;
        sAudioList.trainCursor = 0;
        for (AudioUnit unit : sAudioList.getUnitList()) {
            if (unit.isFinished()) {
                sAudioList.trainCursor += 1;
                finishedNumber += 1;
            } else {
                break;
            }
        }
    }

    public AudioUnit getCurrentUnit() {
        if (isAllFinished()) {
            return null;
        }
        return unitList.get(trainCursor);
    }

    public boolean isAllFinished() {
        Log.d(TAG, "isAllFinished: ??????trainCursor???"+trainCursor);
        return trainCursor==audioNumber;
    }

    public int getFinishedNumber() {
        return finishedNumber;
    }

    public int getAudioNumber() {
        return audioNumber;
    }

    public String getSectionName() {
        return sectionName;
    }

    public static boolean isPrepare() {
        AudioUnit unit = sAudioList.getCurrentUnit();
        return unit.isDownload();
    }
}
