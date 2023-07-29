package com.xuhao.didi.oksocket;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class MyBackgroundService extends Service {

    @Override
    public void onCreate() {
        super.onCreate();

        // 在 Service 创建时执行初始化操作
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // 处理服务启动命令和参数

        // 返回 START_STICKY，确保当服务被异常终止后会自动重新启动
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // 如果不提供绑定支持，返回 null 即可
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // 在 Service 销毁时执行清理操作
    }
}
