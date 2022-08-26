package com.chilydream.speechtrain.login;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

import com.alibaba.fastjson.JSONObject;
import com.chilydream.speechtrain.R;
import com.chilydream.speechtrain.train.TrainOptionActivity;
import com.chilydream.speechtrain.utils.ConfigConsts;
import com.chilydream.speechtrain.utils.Interaction;
import com.chilydream.speechtrain.utils.NetConnection;
import com.chilydream.speechtrain.utils.UserMessage;

public class QuestionnaireActivity extends AppCompatActivity {

    EditText mEtName;
    EditText mEtTele;
    EditText mEtWeixin;
    Button mBtnSubmit;
    CheckBox mCbAgree;
    UserMessage userMessage;

    public static Intent newIntent(Context context) {
        Intent intent = new Intent(context, QuestionnaireActivity.class);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_questionnaire);

        mEtName = findViewById(R.id.ques_et_name);
        mEtTele = findViewById(R.id.ques_et_tele);
        mEtWeixin = findViewById(R.id.ques_et_weixin);
        mBtnSubmit = findViewById(R.id.ques_btn_submit);
        mBtnSubmit.setOnClickListener(this::btnSubmit);
        mCbAgree = findViewById(R.id.ques_cb_agree);

        userMessage = UserMessage.getUserMessage();
    }

    public void btnSubmit(View view) {
        String name = mEtName.getText().toString();
        String tele = mEtTele.getText().toString();
        String weixin = mEtWeixin.getText().toString();
        boolean ifAgree = mCbAgree.isChecked();

        if (name.equals("")) {
            Interaction.showToast(QuestionnaireActivity.this, "姓名不可为空");
            return;
        } else if (tele.equals("")) {
            Interaction.showToast(QuestionnaireActivity.this, "手机号不可为空");
            return;
        } else if (weixin.equals("")) {
            Interaction.showToast(QuestionnaireActivity.this, "微信号不可为空");
            return;
        } else if (!ifAgree) {
            Interaction.showToast(QuestionnaireActivity.this, "请勾选同意blabla");
            // todo： 需要修改这里的用语
            return;
        }

        JSONObject infoJson = UserMessage.getIdJson();
        infoJson.put("name", name);
        infoJson.put("tele", tele);
        infoJson.put("weixin", weixin);
        Thread thread = new Thread(new QuestionnaireActivity.InfoSubmit(infoJson));
        thread.start();
    }

    private class InfoSubmit implements Runnable {
        JSONObject infoJson;

        public InfoSubmit(JSONObject infoJson) {
            this.infoJson = infoJson;
        }

        @Override
        public void run() {
            NetConnection netConnection = new NetConnection(ConfigConsts.API_INFO);
            netConnection.postJson(infoJson);
            JSONObject resultJson = netConnection.getJsonResult();

            String status = resultJson.getString("status");
            if (status.equals("success")) {
                Intent intent = TrainOptionActivity.newIntent(QuestionnaireActivity.this);
                startActivity(intent);
            } else {
                // todo: 需要一个值来表示连接的错误代码，判断错误是发生在服务器端还是手机端
                Interaction.showToast(QuestionnaireActivity.this, "提交失败，请尝试重新提交");
            }
        }
    }
}