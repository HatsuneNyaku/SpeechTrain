package com.chilydream.speechtrain.train;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.res.AssetManager;
import android.os.Handler;
import android.os.Message;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.chilydream.speechtrain.R;
import com.chilydream.speechtrain.utils.MediaAgent;
import com.chilydream.speechtrain.utils.UploadThread;

import java.lang.ref.WeakReference;
import java.util.List;

public class BasicAgent {
    static final String TAG = "BasicAgent";
    Context parentContext;
    SeekBar mSbProgress;
    SeekBar mSbVolume;
    TextView mTvSentenceId;
    TextView mTvSentenceCorpusLabel;
    ImageView mImgPitch;
    Button mBtnNext;
    MediaAgent mMediaAgent;
    AssetManager mAssetMng;
    AudioList audioList;
    AlertDialog.Builder exitBuilder;
    TrainOption trainOption;

    String templateSentenceId;
    int repeatNum;

    public Handler centerHandler;
    public Handler uploadHandler;
    Handler progHandler;
    Thread uploadThread;
    Thread progThread;

    BasicAgent(Context context) {
        parentContext = context;
        mTvSentenceId = ((Activity) context).findViewById(R.id.train_tv_sentence_id);
        mTvSentenceCorpusLabel = ((Activity) context).findViewById(R.id.train_tv_sentence);
        mImgPitch = ((Activity) context).findViewById(R.id.train_graph_img_pitch);
        mBtnNext = ((Activity) context).findViewById(R.id.train_btn_next);
        mBtnNext.setClickable(false);
        mSbProgress = ((Activity) context).findViewById(R.id.train_sb_progress);
        mSbVolume = ((Activity) context).findViewById(R.id.train_sb_volume);
        audioList = AudioList.getAudioList();

        exitBuilder = new AlertDialog.Builder(context)
                .setTitle("提示").setMessage("录音已上传完毕")
                .setPositiveButton("确定退出", (dialog, which) -> {
                    ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
                    List<ActivityManager.AppTask> appTaskList = activityManager.getAppTasks();
                    for (ActivityManager.AppTask appTask : appTaskList) {
                        appTask.finishAndRemoveTask();
                    }
                });

        templateSentenceId = context.getResources().getString(R.string.train_finished_all_number);
        mMediaAgent = new MediaAgent(context, this);
        mAssetMng = context.getAssets();
        trainOption = TrainOption.getTrainOption();
        mMediaAgent.setTrainType(trainOption.flag_train_type);

        progHandler = new ProgressUpdateHandler(this);
        uploadThread = new UploadThread(this);
        uploadThread.start();
//        uploadHandler = ((UploadThread)uploadThread).getUploadHandler();
        // todo: 现在把获取放到了子线程中
    }

    private static class ProgressUpdateHandler extends Handler {
        private final WeakReference<BasicAgent> weakReference;

        public ProgressUpdateHandler(BasicAgent agent) {
            weakReference = new WeakReference<>(agent);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            BasicAgent agent = weakReference.get();
            agent.mSbProgress.setProgress(msg.arg1);
            agent.progHandler.post(agent.progThread);
        }
    }
}
