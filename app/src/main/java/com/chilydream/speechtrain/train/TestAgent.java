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

import java.io.FileInputStream;
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
            Log.d(TAG, "TestAgent: 按钮被点击了");
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
                agent.mBtnNext.setClickable(false);
                // 负责更新界面，并开始录制音频
                agent.mTvSentenceId.setText(
                        String.format(
                                agent.templateSentenceId,
                                audioList.getFinishedNumber()+1, audioList.getAudioNumber()
                        )
                );
                agent.mTvSentenceCorpusLabel.setText(unit.sentenceCorpusLabel);
                if (TrainOption.ifShowGraph()) {
                    try {
                        InputStream inputStream = new FileInputStream(unit.getImgSaveFile());
                        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                        agent.mImgPitch.setImageBitmap(bitmap);
                    } catch (IOException e) {
                        Log.e(TAG, "runTrain: " + e.toString());
                    }
                }

                agent.mMediaAgent.prepare(unit);
                agent.singleTestHandler.sendEmptyMessage(SingleTestHandler.STAGE_TEST);
            } else if (msg.what == STAGE_NEXT) {
                // 负责将文件加入上传列表，并更新指针
                unit.setFinished();
                audioList.nextCursor();

                if (audioList.isAllFinished()) {
                    agent.mTvSentenceCorpusLabel.setText("训练结束，正在上传录音，请不要关闭应用");
                    Handler uploadHandler = agent.uploadHandler;
                    uploadHandler.sendEmptyMessage(UploadThread.COMMAND_FINISH_TRAIN);
                } else {
                    agent.mBtnNext.setClickable(true);
                }
                Log.d(TAG, "handleMessage: 成功收尾，准备进入下一句");
            } else if (msg.what == STAGE_ALL_FINISH) {
                agent.exitBuilder.create().show();
                agent.mTvSentenceCorpusLabel.setText("录音已上传完毕\n点击此处即可退出");
                agent.mTvSentenceCorpusLabel.setOnClickListener(v -> {
                    ActivityManager activityManager = (ActivityManager) agent.parentContext.getSystemService(Context.ACTIVITY_SERVICE);
                    List<ActivityManager.AppTask> appTaskList = activityManager.getAppTasks();
                    for (ActivityManager.AppTask appTask : appTaskList) {
                        appTask.finishAndRemoveTask();
                    }
                });
                agent.mTvSentenceCorpusLabel.setClickable(true);

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
        boolean finish_flag;

        ProgressUpdateThread(TestAgent agent, int time_length) {
            weakReference = new WeakReference<>(agent);
            this.time_length = time_length;
            Log.d(TAG, "ProgressUpdateThread: "+time_length);
            finish_flag = false;
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

            if (finish_flag) {
                return;
            }
            if (cur_time - start_time >= time_length) {
                message.arg1 = (int) time_length;
                progHandler.sendMessage(message);
                progHandler.removeCallbacks(agent.progThread);

                if (!finish_flag) {
                    finish_flag = true;
                    agent.mMediaAgent.stopRecord();
                    agent.singleTestHandler.sendEmptyMessage(SingleTestHandler.STAGE_FINISH);
                }
            } else {
                message.arg1 = (int) (cur_time - start_time);
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                progHandler.sendMessage(message);
            }
        }
    }
}
