package com.chilydream.speechtrain.train;

import com.chilydream.speechtrain.utils.SystemMessage;
import com.chilydream.speechtrain.utils.UserMessage;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AudioUnit {
    String corpus_id;
    String sentenceCorpusLabel;
    Long fileSize;
    String finishStatus;
    File saveFile;
    File imgSaveFile;
    int cur_cnt;
    Map<Integer, File> cnt_2_recordFile;

    public AudioUnit(
            String corpus_id,
            String sentenceCorpusLabel,
            String fileSize,
            String finishStatus
    ) {
      this.corpus_id = corpus_id;
      this.sentenceCorpusLabel = sentenceCorpusLabel;
      this.fileSize = Long.parseLong(fileSize);
      this.finishStatus = finishStatus;
      cnt_2_recordFile = new HashMap<>();
      cur_cnt = 0;

      saveFile = new File(SystemMessage.trainAudioDir, corpus_id+".wav");
      imgSaveFile = new File(SystemMessage.imgDir, corpus_id+".png");
    }

    public String getCorpusId() {
        return corpus_id;
    }

    public File getImgSaveFile() {
        return imgSaveFile;
    }

    public File getSaveFile() {
        return saveFile;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public boolean isFinished() {
        return finishStatus.equals("finished");
    }

    public void setFinished() {
        finishStatus = "finished";
    }

    public void addCnt() {
        cur_cnt += 1;
    }

    public File getRecordFile() {
        if (!cnt_2_recordFile.containsKey(cur_cnt)) {
            UserMessage userMessage = UserMessage.getUserMessage();
            Date cur_time = new Date();
            SimpleDateFormat df = new SimpleDateFormat("(yyyy-MM-dd-HH-mm-ss)", Locale.getDefault());
            String strTime = df.format(cur_time);
            String filename = userMessage.getAccount()+"_"+strTime+"_"+ corpus_id +".wav";

            File recordFile = new File(SystemMessage.recordDir, filename);
            cnt_2_recordFile.put(cur_cnt, recordFile);
        }
        return cnt_2_recordFile.get(cur_cnt);
    }

    public boolean isDownload() {
        if (TrainOption.ifShowGraph()) {

            if (!saveFile.exists() || !saveFile.isFile()) {
                return false;
            }
            if (saveFile.length() != fileSize) {
                return false;
            }
            return imgSaveFile.exists();
        }
        if (!saveFile.exists() || !saveFile.isFile()) {
            return false;
        } else return saveFile.length() == fileSize;
    }
}
