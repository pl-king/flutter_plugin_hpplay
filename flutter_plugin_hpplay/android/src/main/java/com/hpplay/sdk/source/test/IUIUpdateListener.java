//package com.hpplay.sdk.source.test;
//
//import com.hpplay.sdk.source.test.bean.MessageDeatail;
//
///**
// * Created by Zippo on 2018/5/16.
// * Date: 2018/5/16
// * Time: 14:44:20
// */
//public interface IUIUpdateListener {
//
//    int STATE_INIT = 0;//初始胡
//
//
//    int STATE_SEARCH_SUCCESS = 1;//搜索完成
//    int STATE_SEARCH_ERROR = 2;
//    int STATE_SEARCH_NO_RESULT = 3;
//    int STATE_SEARCH_RESULT = 4;//搜索结果
//    int STATE_SEARCH_ING = 5;//搜索中
//
//
//    int STATE_CONNECT_VIDEO_PLAY = 6;//视频播放中
//    int STATE_CONNECT_VIDEO_PAUSE = 7;//视频播放中
////    int STATE_CONNECT_VIDEO = 8;//视频播放中
//
//    int STATE_CONNECT_PIC = 9;//图片播放中
//    int STATE_CONNECT_SUCCESS = 10;
//    int STATE_DISCONNECT = 11;// 连接断开
//    int STATE_CONNECT_FAILURE = 12;// 连接失败
//    int STATE_PLAY = 20;
//    int STATE_PAUSE = 21;
//    int STATE_COMPLETION = 22;
//    int STATE_STOP = 23;
//    int STATE_SEEK = 24;
//    int STATE_POSITION_UPDATE = 25;
//    int STATE_PLAY_ERROR = 26;
//    int STATE_LOADING = 27;
//    int STATE_INPUT_SCREENCODE = 28;
//    int RELEVANCE_DATA_UNSUPPORT = 29;
//    int STATE_SCREENSHOT = 30;
//
//    void onUpdate(int what, MessageDeatail deatail);
//
//}
