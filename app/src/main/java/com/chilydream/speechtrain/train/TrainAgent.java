package com.chilydream.speechtrain.train;

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.chilydream.speechtrain.R;
import com.chilydream.speechtrain.utils.MediaAgent;
import com.chilydream.speechtrain.utils.UploadThread;

import java.io.IOException;
import java.io.InputStream;

public class TrainAgent {
    private static final String TAG = "TrainAgent";
    SeekBar mSbProgress;
    SeekBar mSbVolume;
    TextView mTvSentenceId;
    TextView mTvSentenceContent;
    ImageView mImgPitch;
    Button mBtnNext;
    MediaAgent mMediaAgent;
    AssetManager mAssetMng;
    AudioUnit audioUnit;
    AudioList audioList;

    String templateSentenceId;
    TrainOption trainOption;
    TrainThread trainThread;
    UploadThread uploadThread;

    int repeatNum;

    public TrainAgent(Context context) {
        mTvSentenceId = ((Activity) context).findViewById(R.id.train_tv_sentence_id);
        mTvSentenceContent = ((Activity) context).findViewById(R.id.train_tv_sentence);
        mImgPitch = ((Activity) context).findViewById(R.id.train_graph_img_pitch);
        mBtnNext = ((Activity) context).findViewById(R.id.train_btn_next);
        mSbProgress = ((Activity) context).findViewById(R.id.train_sb_progress);
        mSbVolume = ((Activity) context).findViewById(R.id.train_sb_volume);
        audioList = AudioList.getAudioList();

        mBtnNext.setOnClickListener(view -> {
            mBtnNext.setClickable(false);
            nextClick(view);
        });

        templateSentenceId = context.getResources().getString(R.string.train_finished_all_number);
        mMediaAgent = new MediaAgent(context);
        mAssetMng = context.getAssets();
        trainOption = TrainOption.getTrainOption();
        uploadThread = new UploadThread();

        switch (trainOption.posRepeat) {
            case 0:
                repeatNum = 3;
                break;
            case 1:
                repeatNum = 5;
                break;
            case 2:
                repeatNum = 7;
                break;
            default:
                repeatNum = 6;
                break;
        }
    }

    void nextClick(View view) {
        if (!audioList.isAllFinished()) {
            runTrain();
            audioList.nextCursor();
            audioUnit = audioList.getCurrentUnit();
            mBtnNext.setClickable(true);
        }
        if (audioList.isAllFinished()) {
            mTvSentenceContent.setText(R.string.train_tv_finish_train);
            uploadThread.finishTrain();
            try {
                uploadThread.join();
            } catch (InterruptedException e) {
                Log.e(TAG, "nextClick: " + e.toString());
            }
        }
    }

    public void setAudioUnit(AudioUnit audioUnit) {
        this.audioUnit = audioUnit;
    }

    public void runTrain() {
        mTvSentenceId.setText(
                String.format(
                        templateSentenceId,
                        audioList.getFinishedNumber(), audioList.getAudioNumber()
                )
        );
        mTvSentenceContent.setText(audioUnit.sentenceContent);
        if (TrainOption.ifShowGraph()) {
            try {
                InputStream inputStream = mAssetMng.open(audioUnit.getImgFile().getAbsolutePath());
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                mImgPitch.setImageBitmap(bitmap);
            } catch (IOException e) {
                Log.e(TAG, "runTrain: " + e.toString());
            }
        }

        mMediaAgent.setMode(trainOption.posReadMode);
        mMediaAgent.prepare(audioUnit);
        trainThread = new TrainThread();
        trainThread.start();
        try {
            trainThread.join();
            // todo: 需要使用其他方法来同步线程吗
        } catch (InterruptedException e) {
            Log.e(TAG, e.toString());
        }
        audioUnit.finishStatus = "finished";
        UploadThread.addUploadAudio(audioUnit);
    }

    class TrainThread extends Thread {
        @Override
        public void run() {
            super.run();
            if (trainOption.posReadMode == 0) {
                listenAndRead();
            } else {
                readSychroImmediate();
            }
        }
    }


    private void readSychroImmediate() {
        ProgressTrackThread recordProgThread;
        recordProgThread = new ProgressTrackThread(mMediaAgent.getRecordTime());
        recordProgThread.set_record_flag(1);

        for (int i = 0; i < repeatNum; i += 1) {
            mMediaAgent.playAudio();
            mMediaAgent.startRecordAudio();
            recordProgThread.start();
            try {
                recordProgThread.join();
            } catch (InterruptedException e) {
                Log.e(TAG, "listenAndRead: " + e.toString());
            }
        }
    }

    private void listenAndRead() {
        ProgressTrackThread playingProgThread;
        ProgressTrackThread recordProgThread;

        playingProgThread = new ProgressTrackThread(mMediaAgent.getDuration());
        recordProgThread = new ProgressTrackThread(mMediaAgent.getRecordTime());
        recordProgThread.set_record_flag(1);
        for (int i = 0; i < repeatNum; i += 1) {
            mMediaAgent.playAudio();
            playingProgThread.start();
            try {
                playingProgThread.join();
            } catch (InterruptedException e) {
                Log.e(TAG, e.toString());
            }

            mMediaAgent.startRecordAudio();
            recordProgThread.start();
            try {
                recordProgThread.join();
            } catch (InterruptedException e) {
                Log.e(TAG, "listenAndRead: " + e.toString());
            }
        }
    }

    class ProgressTrackThread extends Thread {
        int progressSbLength;
        long start_time;
        int record_flag = 0;

        ProgressTrackThread(int SbLength) {
            this.progressSbLength = SbLength;
            mSbProgress.setMax(SbLength);
            start_time = System.currentTimeMillis();
        }

        void set_record_flag(int record_flag) {
            this.record_flag = record_flag;
        }

        @Override
        public void run() {
            super.run();
            long cur_time;
            while ((cur_time = System.currentTimeMillis()) - start_time < progressSbLength) {
                mSbProgress.setProgress((int) (cur_time - start_time));
            }
            mSbProgress.setProgress(0);

            if (record_flag != 0) {
                mMediaAgent.stopRecordAudio();
            }
        }
    }
}
