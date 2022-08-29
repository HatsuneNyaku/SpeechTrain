package com.chilydream.speechtrain.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.os.Build;
import android.util.Log;
import android.widget.SeekBar;

import androidx.core.app.ActivityCompat;

import com.chilydream.speechtrain.R;
import com.chilydream.speechtrain.train.AudioUnit;
import com.chilydream.speechtrain.train.TrainOption;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class MediaAgent {
    private final String TAG = "MediaAgent";

    MediaPlayer mMediaPlayer;
    AudioRecord mAudioRecord;
    SeekBar mSbVolume;
    int bufferSizeInBytes;
    int trainMode;
    int recordFlag;     // 0表示没有录制音频，1表示正在录制音频, 2表示发出停止命令但还没停止
    int recordTime;
    File recordFile;

    public MediaAgent(Context context) {
        mMediaPlayer = new MediaPlayer();
        bufferSizeInBytes = AudioRecord.getMinBufferSize(ConfigConsts.RECORD_SAMPLE_RATE,
                ConfigConsts.RECORD_CHANNEL, ConfigConsts.RECORD_ENCODING) * 2;
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "权限不足");
            return;
        }
        mAudioRecord = new AudioRecord(ConfigConsts.RECORD_SOURCE, ConfigConsts.RECORD_SAMPLE_RATE,
                ConfigConsts.RECORD_CHANNEL, ConfigConsts.RECORD_ENCODING, bufferSizeInBytes);
        mSbVolume = ((Activity) context).findViewById(R.id.train_sb_volume);
    }

    public void prepare(AudioUnit audioUnit) {
        mMediaPlayer.reset();
        try {
            mMediaPlayer.setDataSource(audioUnit.getSaveFile().getAbsolutePath());
            mMediaPlayer.prepare();
            recordFlag = 0;
            if (trainMode == TrainOption.MODE_LAR) {
                recordTime = (int) (mMediaPlayer.getDuration() * 1.2);
                // todo: 不再设计单独间隔，而是改成把原音频拉长？
                // 需要改成从服务器获取吗？
            } else if (trainMode == TrainOption.MODE_RSI) {
                recordTime = (int) (mMediaPlayer.getDuration() * 1.1);
                // todo: 不再设计单独间隔，而是改成把原音频拉长？
                // todo: RSI拉长的话应该不是使用倍数，而是增加一定的时间比较好
            }
            recordFile = audioUnit.getRecordFile();
        } catch (IOException e) {
            Log.e(TAG, e.toString());
        }
    }

    public void setMode(int readMode) {
        trainMode = readMode;
    }

    public void playAudio() {
        mMediaPlayer.start();
    }

    public void startRecord() {
        RecordThread recordThread = new RecordThread();
        recordThread.start();
    }

    public void stopRecord() {
        recordFlag = 2;
    }
    public int getDuration() {
        return mMediaPlayer.getDuration();
    }

    public int getRecordTime() {
        return recordTime;
    }

    class RecordThread extends Thread {
        int bufferReadResult;
        OutputStream outputStream;
        ByteArrayOutputStream byteArrayOutputStream;
        byte[] buffer;

        int last_db, db;

        @Override
        public void run() {
            super.run();
            recordFlag = 1;
            mAudioRecord.startRecording();

            byteArrayOutputStream = new ByteArrayOutputStream();
            buffer = new byte[bufferSizeInBytes];
            while (recordFlag == 1) {
                bufferReadResult = mAudioRecord.read(buffer, 0, bufferSizeInBytes);
                last_db = db;
                db = calculateVolume(buffer);
                mSbVolume.setProgress((db + last_db) / 2);
                if (bufferReadResult > 0) {
                    byteArrayOutputStream.write(buffer, 0, bufferReadResult);
                }
            }
            buffer = byteArrayOutputStream.toByteArray();
            Log.d(TAG, "Stop recording, total length: " + buffer.length);
            try {
                outputStream = new FileOutputStream(recordFile);
                // todo: new一个文件会不会同时创建这个文件？
                outputStream.write(getWavHeader(buffer.length));
                outputStream.write(buffer);
            } catch (IOException e) {
                Log.e(TAG, e.toString());
            } finally {
                mSbVolume.setProgress(0);
                if (outputStream != null) {
                    try {
                        outputStream.close();
                        byteArrayOutputStream.close();
                    } catch (IOException e) {
                        Log.e(TAG, e.toString());
                    }
                }
            }
            recordFlag = 0;
        }
    }

    private static int calculateVolume(byte[] buffer) {
        double sumVolume = 0.0;
        double avgVolume = 0.0;
        double volume = 0.0;

        for (int i = 0; i < buffer.length; i += 2) {
            int v1 = buffer[i] & 0xFF;
            int v2 = buffer[i + 1] & 0xFF;
            int temp = v1 + (v2 << 8);// 小端
            if (temp >= 0x8000) {
                temp = 0xffff - temp;
            }
            sumVolume += Math.abs(temp);
        }
        avgVolume = sumVolume / buffer.length / 2;
        volume = Math.log10((1 + avgVolume) / ConfigConsts.DB_BASE) * 180;

        return (int) volume;
    }

    private static byte[] getWavHeader(long totalAudioLen) {
        int mChannels = 1;
        long totalDataLen = totalAudioLen + 36;
        long longSampleRate = ConfigConsts.RECORD_SAMPLE_RATE;
        long byteRate = ConfigConsts.RECORD_SAMPLE_RATE * 2 * mChannels;

        byte[] header = new byte[44];
        header[0] = 'R';  // RIFF/WAVE header
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        header[12] = 'f';  // 'fmt ' chunk
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        header[16] = 16;  // 4 bytes: size of 'fmt ' chunk
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        header[20] = 1;  // format = 1
        header[21] = 0;
        header[22] = (byte) mChannels;
        header[23] = 0;
        header[24] = (byte) (longSampleRate & 0xff);
        header[25] = (byte) ((longSampleRate >> 8) & 0xff);
        header[26] = (byte) ((longSampleRate >> 16) & 0xff);
        header[27] = (byte) ((longSampleRate >> 24) & 0xff);
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        header[32] = (byte) (2 * mChannels);  // block align
        header[33] = 0;
        header[34] = 16;  // bits per sample
        header[35] = 0;
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (totalAudioLen & 0xff);
        header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
        header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
        header[43] = (byte) ((totalAudioLen >> 24) & 0xff);

        return header;
    }
}
