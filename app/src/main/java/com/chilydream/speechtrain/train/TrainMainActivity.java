package com.chilydream.speechtrain.train;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.widget.SeekBar;
import android.widget.TextView;

import com.chilydream.speechtrain.R;

public class TrainMainActivity extends AppCompatActivity {
    TextView mTvSection;
    AudioList audioList;
    TrainOption trainOption;
    SeekBar mSbProg;

    BasicAgent agent;

    public static Intent newIntent(Context context) {
        Intent intent = new Intent(context, TrainMainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
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

        audioList = AudioList.getAudioList();

        mTvSection = findViewById(R.id.train_tv_section_name);
        mTvSection.setText(audioList.getSectionName());
        trainOption = TrainOption.getTrainOption();

        if (trainOption.trainOrTest==TrainOption.MODE_TRAIN) {
            agent = new TrainAgent(this);
        } else if (trainOption.trainOrTest == TrainOption.MODE_TEST) {
            agent = new TestAgent(this);
        }

        mSbProg = findViewById(R.id.train_sb_progress);
//        mSbProg.setEnabled(false);
        // todo: 这里可以设置进度条禁止拖动
        mSbProg.setClickable(false);
        mSbProg.setFocusable(false);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}