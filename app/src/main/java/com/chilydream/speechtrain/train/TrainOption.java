package com.chilydream.speechtrain.train;

import com.alibaba.fastjson.JSONObject;
import com.chilydream.speechtrain.utils.ConfigConsts;
import com.chilydream.speechtrain.utils.NetConnection;
import com.chilydream.speechtrain.utils.UserMessage;

public class TrainOption {
    public static final int MODE_TRAIN = 1;
    public static final int MODE_LAR = 11;
    public static final int MODE_RSI = 12;
    public static final int MODE_TEST = 2;
    public static final int OPTION_NOT_AVAIL = 30;
    public static final int OPTION_AVAIL = 31;
    static TrainOption sTrainOption = null;
    int posQuantity;
    int posContent;
    int posReadMode;
    int posReview;
    int posGraph;
    int posRepeat;
    int availQuantity;
    int availContent;
    int availReadMode;
    int availReview;
    int availGraph;
    int availRepeat;
    int trainOrTest;
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
        // todo: 可能需要判断是否是训练模式
        posReview = Integer.parseInt(resultJson.getString("pos_review"));
        posGraph = Integer.parseInt(resultJson.getString("pos_graph"));
        posRepeat = Integer.parseInt(resultJson.getString("pos_repeat"));
        availQuantity = Integer.parseInt(resultJson.getString("avail_quantity"));
        availContent = Integer.parseInt(resultJson.getString("avail_content"));
        availReadMode = Integer.parseInt(resultJson.getString("avail_read_mode"));
        availReview = Integer.parseInt(resultJson.getString("avail_review"));
        availGraph = Integer.parseInt(resultJson.getString("avail_graph"));
        availRepeat = Integer.parseInt(resultJson.getString("avail_repeat"));
        trainOrTest = Integer.parseInt(resultJson.getString("train_or_test"));
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
        posReadMode = (int) jsonObject.get("read_mode");
        posReview = (int) jsonObject.get("review");
        posGraph = (int) jsonObject.get("graph");
        posRepeat = (int) jsonObject.get("repeat");
    }

    public static boolean ifShowGraph() {
        return sTrainOption.posGraph!=0;
    }
}
