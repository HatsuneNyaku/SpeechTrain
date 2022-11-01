package com.chilydream.speechtrain.login;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import com.alibaba.fastjson.JSONObject;
import com.chilydream.speechtrain.R;
import com.chilydream.speechtrain.train.TrainOption;
import com.chilydream.speechtrain.train.TrainOptionActivity;
import com.chilydream.speechtrain.utils.ConfigConsts;
import com.chilydream.speechtrain.utils.Interaction;
import com.chilydream.speechtrain.utils.NetConnection;
import com.chilydream.speechtrain.utils.SystemMessage;
import com.chilydream.speechtrain.utils.UserMessage;

import java.io.File;

public class LoginActivity extends AppCompatActivity {
    final String TAG = "LogTagLoginActivity";
    EditText mEtAccount;
    EditText mEtPassword;
    CheckBox mCbRememberPwd;
    Button mBtnLogin;
    TextView mTvVersion;

    UserMessage userMessage;
    TrainOption trainOption;

    boolean login_flag = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        // 所有全局不变的、和 context相关的系统信息都应该在这里更新到 SystemMessage类中
//        SystemMessage.rootDir = this.getFilesDir();
        SystemMessage.rootDir = this.getExternalFilesDir("");
        SystemMessage.trainAudioDir = new File(SystemMessage.rootDir, "train_audio");
        SystemMessage.recordDir = new File(SystemMessage.rootDir, "record_audio");
        SystemMessage.imgDir = new File(SystemMessage.rootDir, "img");
        Log.d(TAG, "onCreate: " + SystemMessage.imgDir);
        if (!SystemMessage.trainAudioDir.exists()) {
            boolean mkdir_flag = SystemMessage.trainAudioDir.mkdirs();
            if (!mkdir_flag) {
                Interaction.showToast(this, "创建音频文件夹失败");
            }
        }
        if (!SystemMessage.recordDir.exists()) {
            boolean mkdir_flag = SystemMessage.recordDir.mkdirs();
            if (!mkdir_flag) {
                Interaction.showToast(this, "创建录音文件夹失败");
            }
        }
        if (!SystemMessage.imgDir.exists()) {
            boolean mkdir_flag = SystemMessage.imgDir.mkdirs();
            if (!mkdir_flag) {
                Interaction.showToast(this, "创建图像文件夹失败");
            }
        }

        mEtAccount = findViewById(R.id.login_et_account);
        mEtPassword = findViewById(R.id.login_et_pwd);
        mCbRememberPwd = findViewById(R.id.login_cb_remember);
        mBtnLogin = findViewById(R.id.login_btn_login);
        mTvVersion = findViewById(R.id.login_tv_version);

        // 填入之前记住的账号密码
        userMessage = UserMessage.getUserMessage();
        mEtAccount.setText(userMessage.getAccount());
        mEtPassword.setText(userMessage.getPassword());

        // 显示当前版本号
        String versionName;
        PackageManager pm = getPackageManager();
        try {
            PackageInfo packageInfo = pm.getPackageInfo(getPackageName(), 0);
            versionName = packageInfo.versionName;
            mTvVersion.setText(
                    String.format(
                            getResources().getString(R.string.login_tv_version),
                            versionName
                    )
            );
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        mBtnLogin.setOnClickListener(this::btnLogin);
    }

    private class LoginCheck implements Runnable {
        @Override
        public void run() {
            JSONObject idJson = UserMessage.getIdJson();
            Log.d(TAG, "run: id json:" + idJson.toString());

            NetConnection netConnection = new NetConnection(ConfigConsts.API_LOGIN);
            netConnection.postJson(idJson);
            JSONObject resultJson = netConnection.getJsonResult();
            String auth_result = resultJson.getString("auth_result");
            String ques_result = resultJson.getString("ques_result");
            Log.d(TAG, "auth_result:" + auth_result);
            Log.d(TAG, "ques_result:" + ques_result);

            switch (auth_result) {
                case ConfigConsts.RESULT_AUTH_FAIL:
                    Interaction.showToast(LoginActivity.this, "账号或密码错误");
                    break;
                case ConfigConsts.RESULT_AUTH_SUCCESS:
                    login_flag = true;
                    TrainOption.initTrainOption();
                    trainOption = TrainOption.getTrainOption();
                    if (ques_result.equals(ConfigConsts.RESULT_QUES_NOT_EXIST)) {
                        Intent intent = QuestionnaireActivity.newIntent(LoginActivity.this);
                        startActivity(intent);
                    } else if (ques_result.equals(ConfigConsts.RESULT_QUES_EXIST)) {
                        Intent intent = TrainOptionActivity.newIntent(LoginActivity.this);
                        startActivity(intent);
                    }
                    break;
                case ConfigConsts.RESULT_AUTH_FINISH:
                    Interaction.showToast(LoginActivity.this, "你已完成所有训练");
                    break;
            }
        }
    }


    public void btnLogin(View view) {
        if (mEtAccount.getText().toString().equals("")) {
            Interaction.showToast(this, "账号不可为空");
            return;
        }
        if (mEtPassword.getText().toString().equals("")) {
            Interaction.showToast(this, "密码不可为空");
            return;
        }
//        if (!login_flag) {
//
//        }
        userMessage.setAccount(mEtAccount.getText().toString());
        userMessage.setPassword(mEtPassword.getText().toString());

        if (mCbRememberPwd.isChecked()) {
            userMessage.saveUserInfo();
        } else {
            userMessage.deleteUserInfo();
        }

        Thread thread = new Thread(new LoginCheck());
        thread.start();
    }
}