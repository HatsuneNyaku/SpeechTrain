package com.chilydream.speechtrain.utils;

import java.io.File;

// 静态类，用来存储一些需要借助 context获取的但又不会产生变化的系统信息
public class SystemMessage {
    private SystemMessage() {}
    public static File rootDir;
    public static File trainAudioDir;
    public static File recordDir;
    public static File imgDir;
}