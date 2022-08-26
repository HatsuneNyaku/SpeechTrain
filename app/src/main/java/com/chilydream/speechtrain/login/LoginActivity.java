package com.chilydream.speechtrain.login;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import com.alibaba.fastjson.JSONObject;
import com.chilydream.speechtrain.R;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        // 所有全局不变的、和 context相关的系统信息都应该在这里更新到 SystemMessage类中
        SystemMessage.rootDir = this.getFilesDir();
        SystemMessage.trainAudioDir = new File(SystemMessage.rootDir, "train_audio");
        SystemMessage.recordDir = new File(SystemMessage.rootDir, "record_audio");
        SystemMessage.imgDir = new File(SystemMessage.rootDir, "img");

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

            Intent tmp = TrainOptionActivity.newIntent(LoginActivity.this);
            startActivity(tmp);
            if (true) return;

            NetConnection netConnection = new NetConnection(ConfigConsts.API_LOGIN);
            netConnection.postJson(idJson);
            JSONObject resultJson = netConnection.getJsonResult();
            String status = resultJson.getString("status");
            String message = resultJson.getString("message");
            String needQues = resultJson.getString("needQues");
            Log.d(TAG, "status:"+status);
            Log.d(TAG, "message:"+message);
            Log.d(TAG, "needQues:"+needQues);

            switch (status) {
                case"mismatch":
                    Interaction.showToast(LoginActivity.this, message);
                case"success":
                    if (needQues.equals("1")) {
                        Intent intent = QuestionnaireActivity.newIntent(LoginActivity.this);
                        startActivity(intent);
                    } else {
                        Intent intent = TrainOptionActivity.newIntent(LoginActivity.this);
                        startActivity(intent);
                    }
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