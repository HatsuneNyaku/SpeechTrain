package com.chilydream.speechtrain.train;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import com.alibaba.fastjson.JSONObject;
import com.chilydream.speechtrain.R;
import com.chilydream.speechtrain.utils.ConfigConsts;
import com.chilydream.speechtrain.utils.Interaction;
import com.chilydream.speechtrain.utils.NetConnection;
import com.chilydream.speechtrain.utils.UserMessage;

public class TrainOptionActivity extends AppCompatActivity {
    Spinner mSpQuantity;
    Spinner mSpContent;
    Spinner mSpReadMode;
    Spinner mSpReview;
    Spinner mSpGraph;
    Spinner mSpRepeat;
    Button mBtnConfirm;
    TrainOption trainOption;

    public static Intent newIntent(Context context) {
        Intent intent = new Intent(context, TrainOptionActivity.class);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_train_option);

        mSpQuantity = findViewById(R.id.option_sp_quantity);
        String[] quantity_list = getResources().getStringArray(R.array.option_quantity);
        ArrayAdapter<String> quantityAdapter = new ArrayAdapter<>(this,
                R.layout.support_simple_spinner_dropdown_item, quantity_list);
        mSpQuantity.setAdapter(quantityAdapter);

        mSpContent = findViewById(R.id.option_sp_content);
        String[] content_list = getResources().getStringArray(R.array.option_content);
        ArrayAdapter<String> contentAdapter = new ArrayAdapter<>(this,
                R.layout.support_simple_spinner_dropdown_item, content_list);
        mSpContent.setAdapter(contentAdapter);

        mSpReadMode = findViewById(R.id.option_sp_read_mode);
        String[] read_mode_list = getResources().getStringArray(R.array.option_read_mode);
        ArrayAdapter<String> readModeAdapter = new ArrayAdapter<>(this,
                R.layout.support_simple_spinner_dropdown_item, read_mode_list);
        mSpReadMode.setAdapter(readModeAdapter);

        mSpReview = findViewById(R.id.option_sp_review);
        String[] review_list = getResources().getStringArray(R.array.option_review);
        ArrayAdapter<String> reviewAdapter = new ArrayAdapter<>(this,
                R.layout.support_simple_spinner_dropdown_item, review_list);
        mSpReview.setAdapter(reviewAdapter);

        mSpGraph = findViewById(R.id.option_sp_graph);
        String[] graph_list = getResources().getStringArray(R.array.option_graph);
        ArrayAdapter<String> graphAdapter = new ArrayAdapter<>(this,
                R.layout.support_simple_spinner_dropdown_item, graph_list);
        mSpGraph.setAdapter(graphAdapter);

        mSpRepeat = findViewById(R.id.option_sp_repeat);
        String[] repeat_list = getResources().getStringArray(R.array.option_repeat);
        ArrayAdapter<String> repeatAdapter = new ArrayAdapter<>(
                this, R.layout.support_simple_spinner_dropdown_item, repeat_list);
        mSpRepeat.setAdapter(repeatAdapter);
        // todo: spinner的默认值设置

        mBtnConfirm = findViewById(R.id.option_btn_confirm);
        mBtnConfirm.setOnClickListener(this::optionSubmit);
        trainOption = TrainOption.getTrainOption();

        mSpQuantity.setSelection(trainOption.posQuantity);
        mSpContent.setSelection(trainOption.posContent);
        mSpReadMode.setSelection(trainOption.posReadMode);
        mSpReview.setSelection(trainOption.posReview);
        mSpGraph.setSelection(trainOption.posGraph);
        mSpRepeat.setSelection(trainOption.posRepeat);

        if (trainOption.availQuantity == TrainOption.OPTION_NOT_AVAIL) {
            mSpQuantity.setEnabled(false);
        }
        if (trainOption.availContent == TrainOption.OPTION_NOT_AVAIL) {
            mSpContent.setEnabled(false);
        }
        if (trainOption.availReadMode == TrainOption.OPTION_NOT_AVAIL) {
            mSpReadMode.setEnabled(false);
        }
        if (trainOption.availReview == TrainOption.OPTION_NOT_AVAIL) {
            mSpReview.setEnabled(false);
        }
        if (trainOption.availGraph == TrainOption.OPTION_NOT_AVAIL) {
            mSpGraph.setEnabled(false);
        }
        if (trainOption.availRepeat == TrainOption.OPTION_NOT_AVAIL) {
            mSpRepeat.setEnabled(false);
        }

        askRecordPermission(this);
    }

    public void optionSubmit(View view) {
        // todo: 上传的时候传 position还是 string？

        int posQuantity = mSpQuantity.getSelectedItemPosition();
        int posContent = mSpContent.getSelectedItemPosition();
        int posReadMode = mSpReadMode.getSelectedItemPosition();
        int posReview = mSpReview.getSelectedItemPosition();
        int posGraph = mSpGraph.getSelectedItemPosition();
        int posRepeat = mSpRepeat.getSelectedItemPosition();

        JSONObject trainOptionJson = UserMessage.getIdJson();
        trainOptionJson.put("quantity", posQuantity);
        trainOptionJson.put("content", posContent);
        trainOptionJson.put("read_mode", posReadMode);
        trainOptionJson.put("review", posReview);
        trainOptionJson.put("graph", posGraph);
        trainOptionJson.put("repeat", posRepeat);
        trainOption.updateTrainOption(trainOptionJson);

        Thread thread = new Thread(new TrainOptionActivity.OptionSubmit(trainOptionJson));
        thread.start();
    }

    private class OptionSubmit implements Runnable {
        JSONObject infoJson;

        public OptionSubmit(JSONObject trainOptionJson) {
            this.infoJson = trainOptionJson;
        }

        @Override
        public void run() {
            NetConnection netConnection = new NetConnection(ConfigConsts.API_INFO);
            netConnection.postJson(infoJson);
            JSONObject resultJson = netConnection.getJsonResult();

            String status = resultJson.getString("status");
            if (status.equals("success")) {
                AudioList.initAudioList(resultJson);
                // todo: 在这里弹出窗口，提示正在下载音频，下载完第一个音频才允许进入训练页面
                Intent intent = TrainMainActivity.newIntent(TrainOptionActivity.this);
                startActivity(intent);
            } else {
                // todo: 需要一个值来表示连接的错误代码，判断错误是发生在服务器端还是手机端
                Interaction.showToast(TrainOptionActivity.this, "提交失败，请尝试重新提交");
            }
        }
    }

    public static void askRecordPermission(Context context) {
        if (Build.VERSION.SDK_INT >= 23) {
            int REQUEST_CODE_CONTACT = 101;
            String[] permissions = {
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
            };

            for (String strIter : permissions) {
                if (context.checkSelfPermission(strIter) != PackageManager.PERMISSION_GRANTED) {
                    ((Activity) context).requestPermissions(permissions, REQUEST_CODE_CONTACT);
                }
            }
        }
    }
}