package com.chilydream.speechtrain.train;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.SeekBar;

import androidx.annotation.NonNull;

import com.chilydream.speechtrain.utils.UploadThread;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.List;

public class TestAgent extends BasicAgent {
    private static final String TAG = "TestAgent";

    Handler singleTestHandler;

    public TestAgent(Context context) {
        super(context);
        mBtnNext.setOnClickListener(view -> {
            mBtnNext.setClickable(false);
            if (!audioList.isAllFinished()) {
                centerHandler.sendEmptyMessage(TestCenterHandler.STAGE_START);
            }
        });

        singleTestHandler = new SingleTestHandler(this);
        centerHandler = new TestCenterHandler(this);
        centerHandler.sendEmptyMessage(CenterHandler.STAGE_START);
    }

    public static class TestCenterHandler extends CenterHandler {
        private TestCenterHandler(TestAgent agent) {
            super(agent);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            TestAgent agent = (TestAgent) weakReference.get();
            AudioList audioList = agent.audioList;
            AudioUnit unit = audioList.getCurrentUnit();

            if (msg.what == STAGE_START) {
                // 负责更新界面，并开始录制音频
                agent.mTvSentenceId.setText(
                        String.format(
                                agent.templateSentenceId,
                                audioList.getFinishedNumber(), audioList.getAudioNumber()
                        )
                );
                agent.mTvSentenceContent.setText(unit.sentenceContent);
                if (TrainOption.ifShowGraph()) {
                    try {
                        InputStream inputStream =
                                agent.mAssetMng.open(unit.getImgFile().getAbsolutePath());
                        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                        agent.mImgPitch.setImageBitmap(bitmap);
                    } catch (IOException e) {
                        Log.e(TAG, "runTrain: " + e.toString());
                    }
                }

                agent.mMediaAgent.prepare(unit);
                agent.singleTestHandler.sendEmptyMessage(SingleTestHandler.STAGE_TEST);
                // RSIHandler不关注 msg.what
                // LARHandler则需要输入 STAGE_PLAY进行启动

            } else if (msg.what == STAGE_NEXT) {
                // 负责将文件加入上传列表，并更新指针
                Handler uploadHandler = agent.uploadHandler;

                unit.setFinished();
                Message message = uploadHandler.obtainMessage();
                message.what = UploadThread.COMMAND_UPLOAD;
                message.obj = unit;
                uploadHandler.sendMessage(message);

                audioList.nextCursor();

                if (audioList.isAllFinished()) {
                    agent.mTvSentenceContent.setText("训练结束，正在上传录音，请不要关闭应用");
                    uploadHandler.sendEmptyMessage(UploadThread.COMMAND_FINISH_TRAIN);
                } else {
                    agent.mBtnNext.setClickable(true);
                }
            } else if (msg.what == STAGE_ALL_FINISH) {
                agent.exitBuilder.create().show();
                agent.mTvSentenceContent.setText("录音已上传完毕\n点击此处即可退出");
                agent.mTvSentenceContent.setOnClickListener(v -> {
                    ActivityManager activityManager = (ActivityManager) agent.parentContext.getSystemService(Context.ACTIVITY_SERVICE);
                    List<ActivityManager.AppTask> appTaskList = activityManager.getAppTasks();
                    for (ActivityManager.AppTask appTask : appTaskList) {
                        appTask.finishAndRemoveTask();
                    }
                });
                agent.mTvSentenceContent.setClickable(true);

                agent.mBtnNext.setText("退出");
                agent.mBtnNext.setOnClickListener(view -> {
                    ActivityManager activityManager = (ActivityManager) agent.parentContext.getSystemService(Context.ACTIVITY_SERVICE);
                    List<ActivityManager.AppTask> appTaskList = activityManager.getAppTasks();
                    for (ActivityManager.AppTask appTask : appTaskList) {
                        appTask.finishAndRemoveTask();
                    }
                });
            }
        }
    }


    private static class SingleTestHandler extends Handler {
        static final int STAGE_FINISH = 0;
        static final int STAGE_TEST = 1;
        private final WeakReference<TestAgent> weakReference;

        public SingleTestHandler(TestAgent agent) {
            weakReference = new WeakReference<>(agent);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            TestAgent agent = weakReference.get();
            int time_length;
            SeekBar bar = agent.mSbProgress;
            bar.setProgress(0);

            if (msg.what == STAGE_TEST) {
                time_length = agent.mMediaAgent.getRecordTime();
                bar.setMax(time_length);
                agent.progThread = new ProgressUpdateThread(agent, time_length);
                agent.progThread.start();
            } else if (msg.what == STAGE_FINISH) {
                agent.centerHandler.sendEmptyMessage(CenterHandler.STAGE_NEXT);
            }
        }
    }

    private static class ProgressUpdateThread extends Thread {
        private final WeakReference<TestAgent> weakReference;
        long start_time, time_length;

        ProgressUpdateThread(TestAgent agent, int time_length) {
            weakReference = new WeakReference<>(agent);
            this.time_length = time_length;
        }

        @Override
        public synchronized void start() {
            super.start();
            TestAgent agent = weakReference.get();
            start_time = System.currentTimeMillis();
            agent.mMediaAgent.startRecord();
        }

        @Override
        public void run() {
            super.run();
            TestAgent agent = weakReference.get();
            long cur_time = System.currentTimeMillis();
            Handler progHandler = agent.progHandler;
            Message message = progHandler.obtainMessage();

            if (cur_time - start_time >= time_length) {
                message.arg1 = (int) time_length;
                progHandler.sendMessage(message);
                progHandler.removeCallbacks(agent.progThread);

                agent.mMediaAgent.stopRecord();
                agent.singleTestHandler.sendEmptyMessage(SingleTestHandler.STAGE_FINISH);
            } else {
                message.arg1 = (int) (cur_time - start_time);
                // todo: 这里需要加入睡眠来减少资源消耗吗？例如改成过 10ms更新一次进度条
                progHandler.sendMessage(message);
            }
        }
    }
}
