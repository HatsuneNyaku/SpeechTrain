package com.chilydream.speechtrain.train;

import com.chilydream.speechtrain.utils.SystemMessage;
import com.chilydream.speechtrain.utils.UserMessage;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AudioUnit {
    String downloadId;
    String imgDownloadId;
    String sentenceContent;
    Long fileSize;
    Long imgFileSize;
    String finishStatus;
    File saveFile;
    File imgFile;
    File recordFile;

    public AudioUnit(
            String downloadId,
            String sentenceContent,
            String fileSize,
            String finishStatus
    ) {
      this.downloadId = downloadId;
      this.sentenceContent = sentenceContent;
      this.fileSize = Long.parseLong(fileSize);
      this.finishStatus = finishStatus;

      saveFile = new File(SystemMessage.trainAudioDir, ""+downloadId+".wav");
    }

    public AudioUnit(
            String downloadId,
            String sentenceContent,
            String fileSize,
            String finishStatus,
            String imgDownloadId,
            String imgFileSize
    ) {
        this.downloadId = downloadId;
        this.sentenceContent = sentenceContent;
        this.fileSize = Long.parseLong(fileSize);
        this.finishStatus = finishStatus;
        this.imgDownloadId = imgDownloadId;
        this.imgFileSize = Long.parseLong(imgFileSize);

        saveFile = new File(SystemMessage.trainAudioDir, ""+downloadId+".wav");
        imgFile = new File(SystemMessage.imgDir, ""+imgDownloadId+"png");
    }

    public String getDownloadId() {
        return downloadId;
    }

    public String getImgDownloadId() {
        return imgDownloadId;
    }

    public File getImgFile() {
        return imgFile;
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

    public File getRecordFile() {
        UserMessage userMessage = UserMessage.getUserMessage();
        Date cur_time = new Date();
        SimpleDateFormat df = new SimpleDateFormat("(yyyy-MM-dd-HH-mm-ss)", Locale.getDefault());
        String strTime = df.format(cur_time);
        String filename = userMessage.getAccount()+"_"+strTime+"_"+downloadId+".wav";

        recordFile = new File(SystemMessage.recordDir, filename);
        return recordFile;
    }
}
