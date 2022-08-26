package com.chilydream.speechtrain.utils;

import android.util.Log;

import com.alibaba.fastjson.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

// 单例类，用于保存当前用户信息
// todo: 上传信息时除了同时上传账号密码信息，有什么办法可以确认用户的登录状态？ cookie吗？
// todo: 密码目前是明文上传，后续如果允许用户自定义密码，需要加密之后上传
public class UserMessage {
    private static final String TAG = "LogTagUserMessage";

    String account;
    String password;
    String instruction_show;
    static UserMessage sUserMessage = null;

    private UserMessage() {
        File file = new File(SystemMessage.rootDir, "info.properties");
        if (file.exists()) {
            try {
                FileInputStream fis = new FileInputStream(file);
                Properties pro = new Properties();
                pro.load(fis);
                fis.close();
                account = pro.getProperty("account");
                password = pro.getProperty("password");
                instruction_show = pro.getProperty("instruction");
                if (instruction_show == null) instruction_show = "true";
            } catch (Exception e) {
                Log.e(TAG, e.toString() + "【in construct UserMessage】");
                // todo: 需要增加一个错误信息收集功能
            }
        } else {
            account = null;
            password = null;
            instruction_show = "true";
        }
    }

    public static UserMessage getUserMessage() {
        if (sUserMessage == null) {
            sUserMessage = new UserMessage();
        }
        return sUserMessage;
    }

    public static JSONObject getIdJson() {
        JSONObject idJson = new JSONObject();
        idJson.put("account", sUserMessage.account);
        idJson.put("password", sUserMessage.password);
        return idJson;
    }

    public String getAccount() {
        return account;
    }

    public String getPassword() {
        return password;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void saveUserInfo() {
        File file = new File(SystemMessage.rootDir, "info.properties");
        FileOutputStream fos;
        try {
            fos = new FileOutputStream(file);
        } catch (Exception e) {
            Log.e(TAG, e.toString() + "【in saveUserInfo】");
            throw new RuntimeException();
        }
        Properties pro = new Properties();
        pro.setProperty("account", account);
        pro.setProperty("password", password);
        pro.setProperty("instruction", instruction_show);
        try {
            pro.store(fos, "info.properties");
            fos.close();
        } catch (IOException e) {
            Log.e(TAG, e.toString() + "【in saveUserInfo】");
        }
    }

    public void deleteUserInfo() {
        File file = new File(SystemMessage.rootDir, "info.properties");
        if (file.exists()) {
            file.delete();
        }
    }
}
