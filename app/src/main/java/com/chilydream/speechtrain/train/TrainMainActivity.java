package com.chilydream.speechtrain.train;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.widget.TextView;

import com.chilydream.speechtrain.R;

public class TrainMainActivity extends AppCompatActivity {
    TextView mTvSection;
    AudioList audioList;
    TrainOption trainOption;

    BasicAgent agent;

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
        mTvSection.setText(audioList.getSectionName());
        trainOption = TrainOption.getTrainOption();

        if (trainOption.trainOrTest==TrainOption.MODE_TRAIN) {
            agent = new TrainAgent(this);
        } else if (trainOption.trainOrTest == TrainOption.MODE_TEST) {
            agent = new TestAgent(this);
        }
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