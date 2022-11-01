package com.chilydream.speechtrain.train;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.DialogInterface;
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

import java.util.List;

public class TrainOptionActivity extends AppCompatActivity {
    Spinner mSpCorpusNumber;
    Spinner mSpCorpusLabel;
    Spinner mSpTrainType;
    Spinner mSpReview;
    Spinner mSpGraph;
    Spinner mSpRepeat_0;
    Spinner mSpRepeat_1;
    Button mBtnConfirm;
    TrainOption trainOption;

    Boolean submit_flag;

    public static Intent newIntent(Context context) {
        Intent intent = new Intent(context, TrainOptionActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        // 清楚栈中所有之前的 activity
        // 即不能从这个activity返回到之前的activity
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_train_option);
        trainOption = TrainOption.getTrainOption();
        submit_flag = false;

        mSpCorpusNumber = findViewById(R.id.option_sp_corpus_number);
//        String[] quantity_list = getResources().getStringArray(R.array.option_corpus_number);
        ArrayAdapter<String> quantityAdapter = new ArrayAdapter<>(this,
                R.layout.support_simple_spinner_dropdown_item, trainOption.listCorpusNumber);
        mSpCorpusNumber.setAdapter(quantityAdapter);

        mSpCorpusLabel = findViewById(R.id.option_sp_corpus_label);
        ArrayAdapter<String> contentAdapter = new ArrayAdapter<>(this,
                R.layout.support_simple_spinner_dropdown_item, trainOption.listCorpusLabel);
        mSpCorpusLabel.setAdapter(contentAdapter);

        mSpTrainType = findViewById(R.id.option_sp_read_mode);
        ArrayAdapter<String> trainTypeAdapter = new ArrayAdapter<>(this,
                R.layout.support_simple_spinner_dropdown_item, trainOption.listTrainType);
        mSpTrainType.setAdapter(trainTypeAdapter);

        mSpReview = findViewById(R.id.option_sp_review);
        ArrayAdapter<String> reviewAdapter = new ArrayAdapter<>(this,
                R.layout.support_simple_spinner_dropdown_item, trainOption.listReview);
        mSpReview.setAdapter(reviewAdapter);

        mSpGraph = findViewById(R.id.option_sp_graph);
        ArrayAdapter<String> graphAdapter = new ArrayAdapter<>(this,
                R.layout.support_simple_spinner_dropdown_item, trainOption.listGraph);
        mSpGraph.setAdapter(graphAdapter);

        mSpRepeat_0 = findViewById(R.id.option_sp_repeat_0);
        ArrayAdapter<String> repeat0_Adapter = new ArrayAdapter<>(
                this, R.layout.support_simple_spinner_dropdown_item, trainOption.listRepeat_0);
        mSpRepeat_0.setAdapter(repeat0_Adapter);

        mSpRepeat_1 = findViewById(R.id.option_sp_repeat_1);
        ArrayAdapter<String> repeat1_Adapter = new ArrayAdapter<>(
                this, R.layout.support_simple_spinner_dropdown_item, trainOption.listRepeat_1);
        mSpRepeat_1.setAdapter(repeat1_Adapter);
        // todo: spinner的默认值设置

        mBtnConfirm = findViewById(R.id.option_btn_confirm);
        mBtnConfirm.setOnClickListener(this::optionSubmit);

        // Selection 从0开始
        // pos       从1开始
        mSpCorpusNumber.setSelection(trainOption.posCorpusNumber-1);
        mSpCorpusLabel.setSelection(trainOption.posCorpusLabel-1);
        mSpTrainType.setSelection(trainOption.posTrainType-1);
        mSpReview.setSelection(trainOption.posReview-1);
        mSpGraph.setSelection(trainOption.posGraph-1);
        mSpRepeat_0.setSelection(trainOption.posRepeat_0-1);
        mSpRepeat_1.setSelection(trainOption.posRepeat_1-1);

        if (trainOption.avlCorpusNumber == TrainOption.OPTION_NOT_AVAIL) {
            mSpCorpusNumber.setEnabled(false);
        }
        if (trainOption.avlCorpusLabel == TrainOption.OPTION_NOT_AVAIL) {
            mSpCorpusLabel.setEnabled(false);
        }
        if (trainOption.avlTrainType == TrainOption.OPTION_NOT_AVAIL) {
            mSpTrainType.setEnabled(false);
        }
        if (trainOption.avlReview == TrainOption.OPTION_NOT_AVAIL) {
            mSpReview.setEnabled(false);
        }
        if (trainOption.avlGraph == TrainOption.OPTION_NOT_AVAIL) {
            mSpGraph.setEnabled(false);
        }
        if (trainOption.avlRepeat_0 == TrainOption.OPTION_NOT_AVAIL) {
            mSpRepeat_0.setEnabled(false);
        }
        if (trainOption.avlRepeat_1 == TrainOption.OPTION_NOT_AVAIL) {
            mSpRepeat_1.setEnabled(false);
        }

        askRecordPermission(this);
    }

    public void optionSubmit(View view) {
        // Selection 从0开始
        // pos       从1开始
        int posCorpusNumber = mSpCorpusNumber.getSelectedItemPosition()+1;
        int posCorpusLabel = mSpCorpusLabel.getSelectedItemPosition()+1;
        int posTrainType = mSpTrainType.getSelectedItemPosition()+1;
        int posReview = mSpReview.getSelectedItemPosition()+1;
        int posGraph = mSpGraph.getSelectedItemPosition()+1;
        int posRepeat_0 = mSpRepeat_0.getSelectedItemPosition()+1;
        int posRepeat_1 = mSpRepeat_1.getSelectedItemPosition()+1;

        JSONObject trainOptionJson = UserMessage.getIdJson();
        trainOptionJson.put("pos_corpus_number", posCorpusNumber);
        trainOptionJson.put("pos_corpus_label", posCorpusLabel);
        trainOptionJson.put("pos_train_type", posTrainType);
        trainOptionJson.put("pos_review_percent", posReview);
        trainOptionJson.put("pos_graph_show", posGraph);
        trainOptionJson.put("pos_repeat_0", posRepeat_0);
        trainOptionJson.put("pos_repeat_1", posRepeat_1);
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
            if (submit_flag) {
                if (AudioList.isPrepare()) {
                    Intent intent = TrainMainActivity.newIntent(TrainOptionActivity.this);
                    startActivity(intent);
                } else {
                    runOnUiThread(TrainOptionActivity.this::loadingDialog);
                }
            } else {
                NetConnection netConnection = new NetConnection(ConfigConsts.API_POST_TRAIN_OPTION);
                netConnection.postJson(infoJson);
                JSONObject resultJson = netConnection.getJsonResult();

                String status = resultJson.getString("status");
                if (status.equals("success")) {
                    submit_flag = true;
                    JSONObject dataJson = JSONObject.parseObject(resultJson.getString("data_json"));
                    AudioList.initAudioList(dataJson);
                    if (AudioList.isPrepare()) {
                        Intent intent = TrainMainActivity.newIntent(TrainOptionActivity.this);
                        startActivity(intent);
                    } else {
                        runOnUiThread(TrainOptionActivity.this::loadingDialog);
                    }
                } else {
                    // todo: 需要一个值来表示连接的错误代码，判断错误是发生在服务器端还是手机端
                    Interaction.showToast(TrainOptionActivity.this, "提交失败，请尝试重新提交");
                }
            }
        }
    }


    private void loadingDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle("提示").setMessage("正在下载训练文件，请稍作等待");
        builder.create().show();
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