package com.chilydream.speechtrain.train;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.chilydream.speechtrain.utils.ConfigConsts;
import com.chilydream.speechtrain.utils.MediaAgent;
import com.chilydream.speechtrain.utils.NetConnection;
import com.chilydream.speechtrain.utils.UserMessage;

import java.util.ArrayList;
import java.util.List;

public class TrainOption {
    static TrainOption sTrainOption = null;
    int posQuantity;
    int posContent;
    int posReadMode;
    int posReview;
    int posGraph;
    int posRepeat;
    UserMessage userMessage;

    long timeGap;

    private TrainOption() {
        userMessage = UserMessage.getUserMessage();

        JSONObject idJson = UserMessage.getIdJson();
        NetConnection netConnection = new NetConnection(ConfigConsts.API_TRAIN_OPTION);
        netConnection.postJson(idJson);
        JSONObject resultJson = netConnection.getJsonResult();
        posQuantity = Integer.parseInt(resultJson.getString("pos_quantity"));
        posContent = Integer.parseInt(resultJson.getString("pos_content"));
        posReadMode = Integer.parseInt(resultJson.getString("pos_read_mode"));
        posReview = Integer.parseInt(resultJson.getString("pos_review"));
        posGraph = Integer.parseInt(resultJson.getString("pos_graph"));
        posRepeat = Integer.parseInt(resultJson.getString("pos_repeat"));
        // todo: 连接失败怎么办

        timeGap = 1000;
    }

    public static TrainOption getTrainOption() {
        if (sTrainOption==null) {
            sTrainOption = new TrainOption();
        }
        return sTrainOption;
    }

    public void updateTrainOption(JSONObject jsonObject) {
        posQuantity = (int) jsonObject.get("quantity");
        posContent = (int) jsonObject.get("content");
        posReadMode = (int) jsonObject.get("readMode");
        posReview = (int) jsonObject.get("review");
        posGraph = (int) jsonObject.get("graph");
        posRepeat = (int) jsonObject.get("repeat");
    }

    public static boolean ifShowGraph() {
        return sTrainOption.posGraph!=0;
    }
}
