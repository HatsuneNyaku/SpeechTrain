package com.chilydream.speechtrain.train;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ActivityManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.chilydream.speechtrain.R;
import com.chilydream.speechtrain.utils.MediaAgent;

import java.util.List;

public class TrainMainActivity extends AppCompatActivity {
    TextView mTvSection;
    TextView mTvSentenceContent;
    AudioList audioList;
    AudioUnit audioUnit;

    TrainAgent trainAgent;

    public static Intent newIntent(Context context) {
        Intent intent = new Intent(context, TrainMainActivity.class);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (TrainOption.ifShowGraph()) {
            setContentView(R.layout.activity_train_graph);
        } else {
            setContentView(R.layout.activity_train_nograph);
        }
        mTvSection = findViewById(R.id.train_tv_section_name);
        mTvSentenceContent = findViewById(R.id.train_tv_sentence);

        audioUnit = audioList.getCurrentUnit();
        mTvSection.setText(audioList.getSectionName());

        trainAgent = new TrainAgent(this);
        trainAgent.setAudioUnit(audioUnit);
    }

    private void exitDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle("提示").setMessage("录音已上传完毕")
                .setPositiveButton("确定退出", new DialogInterface.OnClickListener() {
                    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ActivityManager activityManager = (ActivityManager) TrainMainActivity.this.getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);
                        List<ActivityManager.AppTask> appTaskList = activityManager.getAppTasks();
                        for (ActivityManager.AppTask appTask : appTaskList) {
                            appTask.finishAndRemoveTask();
                        }
                    }
                });
        builder.create().show();
    }
}