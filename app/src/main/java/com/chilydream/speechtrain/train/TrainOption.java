package com.chilydream.speechtrain.train;

import android.util.Log;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.chilydream.speechtrain.utils.ConfigConsts;
import com.chilydream.speechtrain.utils.NetConnection;
import com.chilydream.speechtrain.utils.UserMessage;

import java.util.List;
import java.util.Locale;

public class TrainOption {
    static final String TAG = "TrainOptionTag";
    public static final int MODE_TRAIN = 1;
    public static final int MODE_LAR = 11;
    public static final int MODE_RSI = 12;
    public static final int MODE_TEST = 2;
    public static final int OPTION_NOT_AVAIL = 0;
    public static final int OPTION_AVAIL = 1;
    static TrainOption sTrainOption = null;
    int posCorpusNumber;
    int posCorpusLabel;
    int posTrainType;
    int posReview;
    int posGraph;
    int posRepeat_0;
    int posRepeat_1;
    int avlCorpusNumber;
    int avlCorpusLabel;
    int avlTrainType;
    int avlReview;
    int avlGraph;
    int avlRepeat_0;
    int avlRepeat_1;
    List<String> listCorpusNumber;
    List<String> listCorpusLabel;
    List<String> listTrainType;
    List<String> listReview;
    List<String> listGraph;
    List<String> listRepeat_0;
    List<String> listRepeat_1;
    int trainOrTest;
    int flag_train_type;
    UserMessage userMessage;

    long timeGap;

    private TrainOption() {
        userMessage = UserMessage.getUserMessage();

        JSONObject idJson = UserMessage.getIdJson();
        NetConnection netConnection = new NetConnection(ConfigConsts.API_GET_TRAIN_OPTION);
        netConnection.postJson(idJson);
        JSONObject resultJson = netConnection.getJsonResult();
        posCorpusNumber = Integer.parseInt(resultJson.getString("pos_corpus_number"));
        posCorpusLabel = Integer.parseInt(resultJson.getString("pos_corpus_label"));
        posTrainType = Integer.parseInt(resultJson.getString("pos_train_type"));
        posReview = Integer.parseInt(resultJson.getString("pos_review_percent"));
        posGraph = Integer.parseInt(resultJson.getString("pos_graph_show"));
        posRepeat_0 = Integer.parseInt(resultJson.getString("pos_repeat_0"));
        posRepeat_1 = Integer.parseInt(resultJson.getString("pos_repeat_1"));
        avlCorpusNumber = Integer.parseInt(resultJson.getString("avl_corpus_number"));
        avlCorpusLabel = Integer.parseInt(resultJson.getString("avl_corpus_label"));
        avlTrainType = Integer.parseInt(resultJson.getString("avl_train_type"));
        avlReview = Integer.parseInt(resultJson.getString("avl_review_percent"));
        avlGraph = Integer.parseInt(resultJson.getString("avl_graph_show"));
        avlRepeat_0 = Integer.parseInt(resultJson.getString("avl_repeat_0"));
        avlRepeat_1 = Integer.parseInt(resultJson.getString("avl_repeat_1"));
        listCorpusNumber = JSONArray.parseArray(resultJson.getString("list_corpus_number"), String.class);
        listCorpusLabel  = JSONArray.parseArray(resultJson.getString("list_corpus_label"), String.class);
        listTrainType = JSONArray.parseArray(resultJson.getString("list_train_type"), String.class);
        listReview   = JSONArray.parseArray(resultJson.getString("list_review_percent"), String.class);
        listGraph    = JSONArray.parseArray(resultJson.getString("list_graph_show"), String.class);
        listRepeat_0   = JSONArray.parseArray(resultJson.getString("list_repeat_0"), String.class);
        listRepeat_1   = JSONArray.parseArray(resultJson.getString("list_repeat_1"), String.class);
        String  tmp_flag = resultJson.getString("train_or_test");
        if (tmp_flag.equals("train")) {
            trainOrTest = TrainOption.MODE_TRAIN;
        } else if (tmp_flag.equals("test")) {
            trainOrTest = TrainOption.MODE_TEST;
        }
        if (listTrainType.get(posTrainType-1).equalsIgnoreCase("rsi")) {
            flag_train_type = TrainOption.MODE_RSI;
        } else if (listTrainType.get(posTrainType-1).equalsIgnoreCase("lar")) {
            flag_train_type = TrainOption.MODE_LAR;
        } else if (listTrainType.get(posTrainType-1).equalsIgnoreCase("test")) {
            flag_train_type = TrainOption.MODE_TEST;
        }
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
        Log.d(TAG, "updateTrainOption: "+jsonObject.toString());
        posCorpusNumber = (int) jsonObject.get("pos_corpus_number");
        posCorpusLabel = (int) jsonObject.get("pos_corpus_label");
        posTrainType = (int) jsonObject.get("pos_train_type");
        posReview = (int) jsonObject.get("pos_review_percent");
        posGraph = (int) jsonObject.get("pos_graph_show");
        posRepeat_0 = (int) jsonObject.get("pos_repeat_0");
        posRepeat_1 = (int) jsonObject.get("pos_repeat_1");
    }

    public static boolean ifShowGraph() {
        // posGraph=2, 不显示图像，等价于 selection=1
        // posGraph=1, 显示图像，  等价于 selection=0
        Log.d(TAG, "ifShowGraph: "+(sTrainOption.posGraph==1));
        return sTrainOption.posGraph==1;
    }
}
