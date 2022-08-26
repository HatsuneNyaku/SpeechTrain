package com.chilydream.speechtrain.train;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.chilydream.speechtrain.utils.DownloadThread;

import java.util.ArrayList;
import java.util.List;

public class AudioList {
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
        String audio_list_str = dataJson.getString("audio_list");

        sAudioList = new AudioList();
        sAudioList.sectionName = dataJson.getString("section_name");

        JSONObject unit_json;
        JSONObject audio_list_json = JSON.parseObject(audio_list_str);
        String download_id, sentence_content, file_size, finish_status, img_download_id, img_file_size;

        sAudioList.audioNumber = Integer.parseInt(audio_list_json.getString("audio_number"));
        for (int i = 0; i < sAudioList.audioNumber; i += 1) {
            unit_json = JSON.parseObject(audio_list_json.getString("" + i));
            download_id = unit_json.getString("download_id");
            sentence_content = unit_json.getString("sentence_content");
            file_size = unit_json.getString("file_size");
            finish_status = unit_json.getString("finish_status");
            if (unit_json.containsKey("img_download_id")) {
                img_download_id = unit_json.getString("img_download_id");
                img_file_size = unit_json.getString("img_file_size");
                sAudioList.unitList.add(
                        new AudioUnit(download_id, sentence_content, file_size,
                                finish_status, img_download_id, img_file_size)
                );
            } else {
                sAudioList.unitList.add(
                        new AudioUnit(download_id, sentence_content, file_size, finish_status)
                );
            }

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
        finishedNumber = 0;
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
}
