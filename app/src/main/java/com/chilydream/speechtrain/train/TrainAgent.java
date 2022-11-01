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

public class TrainAgent extends BasicAgent {
    private static final String TAG = "TrainAgent";
    Handler singleTrainHandler;

    public TrainAgent(Context context) {
        super(context);
        mBtnNext.setOnClickListener(view -> {
            mBtnNext.setClickable(false);
            if (!audioList.isAllFinished()) {
                centerHandler.sendEmptyMessage(TrainCenterHandler.STAGE_START);
            }
        });
        num_repeat_1 = TrainOption.getTrainOption().num_repeat_1;

        switch (trainOption.flag_train_type) {
            case TrainOption.MODE_LAR:
                singleTrainHandler = new LARHandler(this);
                break;
            case TrainOption.MODE_RSI:
                singleTrainHandler = new RSIHandler(this);
                break;
            default:
                singleTrainHandler = new RSIHandler(this);
                break;
        }

        centerHandler = new TrainCenterHandler(this);
        centerHandler.sendEmptyMessage(CenterHandler.STAGE_START);
    }

    public static class TrainCenterHandler extends CenterHandler {
        private TrainCenterHandler(TrainAgent agent) {
            super(agent);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            TrainAgent agent = (TrainAgent) weakReference.get();
            AudioList audioList = agent.audioList;
            AudioUnit unit = audioList.getCurrentUnit();

            if (msg.what == STAGE_START) {
                agent.mTvSentenceId.setText(
                        String.format(
                                agent.templateSentenceId,
                                audioList.getFinishedNumber() + 1, audioList.getAudioNumber()
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
                agent.singleTrainHandler.sendEmptyMessage(LARHandler.STAGE_PLAY);
                // RSIHandler不关注 msg.what
                // LARHandler则需要输入 STAGE_PLAY进行启动

            } else if (msg.what == STAGE_NEXT) {
                Handler uploadHandler = agent.uploadHandler;

                unit.setFinished();
                audioList.nextCursor();

                if (audioList.isAllFinished()) {
                    agent.mTvSentenceCorpusLabel.setText("训练结束，正在上传录音，请不要关闭应用");
                    uploadHandler.sendEmptyMessage(UploadThread.COMMAND_FINISH_TRAIN);
                } else {
                    agent.mBtnNext.setClickable(true);
                }
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

    private static class LARHandler extends Handler {
        static final int STAGE_PLAY = 1;
        static final int STAGE_RECORD = 2;
        private final WeakReference<TrainAgent> weakReference;
        private int train_count, repeat_num;

        public LARHandler(TrainAgent agent) {
            weakReference = new WeakReference<>(agent);
            train_count = 0;
            repeat_num = agent.num_repeat_1;
            // 0表示一次都没做，1表示正在第一次训练
            // 假设总共训练5次，那么rc=6才意味着不进入训练流程
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            TrainAgent agent = weakReference.get();
            int time_length;
            SeekBar bar = agent.mSbProgress;
            bar.setProgress(0);

            Log.d(TAG, "handleMessage: what:"+msg.what);
            Log.d(TAG, "handleMessage: now "+train_count+" tgt "+repeat_num);
            if (train_count < repeat_num) {
                // todo：更新 unit的 cnt
                if (msg.what == STAGE_PLAY) {
                    Log.d(TAG, "handleMessage: STAGE_PLAY");
                    time_length = agent.mMediaAgent.getDuration();
                    bar.setMax(time_length);
                    agent.progThread = new ProgressUpdateThread(agent,
                            ProgressUpdateThread.MODE_AUDIO, time_length);
                    agent.progThread.start();
                } else if (msg.what == STAGE_RECORD) {
                    train_count += 1;
                    // 开始录音才增加计数
                    time_length = agent.mMediaAgent.getRecordTime();
                    bar.setMax(time_length);
                    agent.progThread = new ProgressUpdateThread(agent,
                            ProgressUpdateThread.MODE_RECORD, time_length);
                    agent.progThread.start();
                }
            } else {
                train_count = 0;
                Log.d(TAG, "handleMessage: train_count计数清零");
                agent.centerHandler.sendEmptyMessage(CenterHandler.STAGE_NEXT);
            }
        }
    }

    private static class RSIHandler extends Handler {
        private final WeakReference<TrainAgent> weakReference;
        int train_count, repeat_num;

        public RSIHandler(TrainAgent agent) {
            weakReference = new WeakReference<>(agent);
            repeat_num = agent.num_repeat_1;
            train_count = 0;
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            TrainAgent agent = weakReference.get();
            int time_length;
            SeekBar bar = agent.mSbProgress;
            bar.setProgress(0);

            train_count += 1;
            if (train_count <= repeat_num) {
                time_length = agent.mMediaAgent.getRecordTime();
                bar.setMax(time_length);
                agent.progThread = new ProgressUpdateThread(agent,
                        ProgressUpdateThread.MODE_RSI, time_length);

                agent.progThread.start();
            } else {
                train_count = 0;
                agent.centerHandler.sendEmptyMessage(CenterHandler.STAGE_NEXT);
            }
        }
    }

    private static class ProgressUpdateThread extends Thread {
        static final int MODE_AUDIO = 1;
        static final int MODE_RECORD = 2;
        static final int MODE_RSI = 3;
        boolean send_flag = false;
        private final WeakReference<TrainAgent> weakReference;
        long start_time, time_length;
        int prog_mode;

        ProgressUpdateThread(TrainAgent agent, int prog_mode, int time_length) {
            weakReference = new WeakReference<>(agent);
            this.prog_mode = prog_mode;
            this.time_length = time_length;
        }

        @Override
        public synchronized void start() {
            super.start();
            TrainAgent agent = weakReference.get();
            start_time = System.currentTimeMillis();
            send_flag = false;

            if (prog_mode == MODE_AUDIO) {
                agent.mMediaAgent.playAudio();
            } else if (prog_mode == MODE_RECORD) {
                agent.mMediaAgent.startRecord();
            } else if (prog_mode == MODE_RSI) {
                agent.mMediaAgent.playAudio();
                agent.mMediaAgent.startRecord();
            }
        }

        @Override
        public void run() {
            super.run();
            TrainAgent agent = weakReference.get();
            long cur_time = System.currentTimeMillis();
            Handler progHandler = agent.progHandler;
            Message message = progHandler.obtainMessage();

            if (cur_time - start_time >= time_length) {
                message.arg1 = (int) time_length;
                progHandler.sendMessage(message);
                progHandler.removeCallbacks(agent.progThread);
                if (prog_mode == MODE_RECORD) {
                    if (!send_flag) {
                        send_flag = true;
                        agent.mMediaAgent.stopRecord();
                        agent.singleTrainHandler.sendEmptyMessage(LARHandler.STAGE_PLAY);
                        // 如果是RSI模式，那么这里的 msg.what就没有任何影响和意义
                        // 如果是LAR模式，这里的 PLAY就代表着录音阶段结束，接下来是 PLAY阶段
                    }
                } else if (prog_mode == MODE_AUDIO) {
                    if (!send_flag) {
                        send_flag = true;
                        agent.singleTrainHandler.sendEmptyMessage(LARHandler.STAGE_RECORD);
                        // 如果是RSI模式，那么这里的 msg.what就没有任何影响和意义
                        // 如果是LAR模式，这里的 RECORD就代表着播放阶段结束，接下来是 RECORD阶段
                    }
                } else if (prog_mode == MODE_RSI) {
                    if (!send_flag) {
                        send_flag = true;
                        agent.mMediaAgent.stopRecord();
                        agent.singleTrainHandler.sendEmptyMessage(0);
                        // 如果是RSI模式，那么这里的 msg.what就没有任何影响和意义
                    }
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
