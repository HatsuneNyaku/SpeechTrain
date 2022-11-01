package com.chilydream.speechtrain.utils;

import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class NetConnection {
    //todo: 如果连接失败应该怎么处理
    private static final String TAG = "NetConnection";
    private final HttpURLConnection mHttpURLConnection;
    private boolean connectionFlag;

    public NetConnection(String api) {
        StringBuilder SBURL = new StringBuilder(ConfigConsts.BASE_URL + api);
        Log.d(TAG, SBURL.toString());
        connectionFlag = false;
        URL mURL;
        try {
            mURL = new URL(SBURL.toString());
        } catch (MalformedURLException e) {
            Log.e(TAG, e.toString());
            throw new RuntimeException();
        }

        try {
            mHttpURLConnection = (HttpURLConnection) mURL.openConnection();
            mHttpURLConnection.setReadTimeout(5000);
            mHttpURLConnection.setConnectTimeout(5000);
        } catch (IOException e) {
            Log.e(TAG, e.toString());
            throw new RuntimeException();
        }
    }

    public void postJson(JSONObject jsonObject) {
        Log.d(TAG, jsonObject.toString());
        if (connectionFlag) {
            Log.e(TAG, "Trying to connect to a exist connection.");
            throw new RuntimeException();
        } else {
            connectionFlag = true;
        }
        try {
            mHttpURLConnection.setRequestMethod("POST");
            mHttpURLConnection.setRequestProperty("Content-Type", "application/json");
            mHttpURLConnection.setRequestProperty("connection", "keep-alive");
            mHttpURLConnection.setDoInput(true);
            mHttpURLConnection.setDoOutput(true);
            mHttpURLConnection.connect();

            OutputStream outputStream = mHttpURLConnection.getOutputStream();
            outputStream.write(jsonObject.toString().getBytes());
            outputStream.flush();
            outputStream.close();

        } catch (IOException e) {
            Log.e(TAG, e.toString());
            throw new RuntimeException();
        }
    }

    public void postFile(String fileName, byte[] bytesData) {
        String clear_name = fileName.trim().substring(fileName.lastIndexOf("/") + 1);

        if (connectionFlag) {
            Log.e(TAG, "Trying to connect to a exist connection.");
            throw new RuntimeException();
        } else {
            connectionFlag = true;
        }
        try {
            final String newLine = "\r\n"; // 换行符
            final String boundaryPrefix = "--"; //边界前缀
            // 定义数据分隔线
            final String boundary = String.format("=========%s", System.currentTimeMillis());

            mHttpURLConnection.setRequestMethod("POST");
            // 发送POST请求必须设置如下两行
            mHttpURLConnection.setDoOutput(true);
            mHttpURLConnection.setDoInput(true);
            mHttpURLConnection.setUseCaches(false);
            // 设置请求头参数
            mHttpURLConnection.setRequestProperty("connection", "Keep-Alive");
            mHttpURLConnection.setRequestProperty("Charsert", "UTF-8");
            mHttpURLConnection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

            OutputStream out = new DataOutputStream(mHttpURLConnection.getOutputStream());

            StringBuilder sb = new StringBuilder();
            sb.append(boundaryPrefix);
            sb.append(boundary);
            sb.append(newLine);
            // 文件参数
            sb.append("Content-Disposition: form-data;name=\"file\";filename=\"")
                    .append(clear_name)
                    .append("\"")
                    .append(newLine);
            sb.append("Content-Type:application/octet-stream");
            // sb.append("CorpusLabel-Type:audio/mpeg");
            sb.append("Content-Transfer-Encoding: binary");
            // 参数头设置完以后需要两个换行，然后才是参数内容
            sb.append(newLine);
            sb.append(newLine);
            // 将参数头的数据写入到输出流中
            out.write(sb.toString().getBytes());
            out.write(bytesData);
            // 最后添加换行
            out.write(newLine.getBytes());
            // 定义最后数据分隔线，即--加上boundary再加上--。
            byte[] end_data = (newLine + boundaryPrefix + boundary + boundaryPrefix + newLine).getBytes();
            // 写上结尾标识
            out.write(end_data);
            out.flush();
            out.close();

            // 定义BufferedReader输入流来读取URL的响应
            StringBuilder sbOutPut = new StringBuilder();
            BufferedReader reader = new BufferedReader(new InputStreamReader(mHttpURLConnection.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                sbOutPut.append(line);
            }

        } catch (Exception e) {
            Log.e(TAG, "发送POST请求出现异常！" + e.toString());
            throw new RuntimeException();
        }
    }


    public JSONObject getJsonResult() {
        if (!connectionFlag) {
            Log.e(TAG, "Connection is not built.");
            throw new RuntimeException();
        } else {
            connectionFlag = false;
        }
        String lines;
        StringBuilder stringBuilder = new StringBuilder();
        BufferedReader bufferedReader;
        try {
            bufferedReader = new BufferedReader(
                    new InputStreamReader(mHttpURLConnection.getInputStream()));
            while ((lines = bufferedReader.readLine()) != null) {
                lines = new String(lines.getBytes(), StandardCharsets.UTF_8);
                stringBuilder.append(lines);
            }
            bufferedReader.close();
            mHttpURLConnection.disconnect();
        } catch (IOException e) {
            Log.e(TAG, e.toString());
            throw new RuntimeException();
        }
        return JSON.parseObject(stringBuilder.toString());
    }

    public void downloadFile(String save_path) {
        if (!connectionFlag) {
            Log.d(TAG, "Connection is not built.");
            return;
        } else {
            connectionFlag = false;
        }

        InputStream inputStream;
        BufferedInputStream bufferedInputStream;
        BufferedOutputStream bufferedOutputStream;
        FileOutputStream fileOutputStream;
        byte[] buffer = new byte[1024];
        int len;

        try {
            inputStream = mHttpURLConnection.getInputStream();
            bufferedInputStream = new BufferedInputStream(inputStream);
            fileOutputStream = new FileOutputStream(save_path);
            bufferedOutputStream = new BufferedOutputStream(fileOutputStream);

            while ((len = bufferedInputStream.read(buffer)) != -1) {
                bufferedOutputStream.write(buffer, 0, len);
            }
            bufferedInputStream.close();
            bufferedOutputStream.close();
        } catch (IOException e) {
            Log.e(TAG, e.toString());
        }
        mHttpURLConnection.disconnect();
    }
}
