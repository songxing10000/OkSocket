package com.xuhao.didi.oksocket;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.xuhao.didi.core.pojo.OriginalData;
import com.xuhao.didi.oksocket.adapter.LogAdapter;
import com.xuhao.didi.oksocket.data.AdminHandShakeBean;
import com.xuhao.didi.oksocket.data.AdminKickOfflineBean;
import com.xuhao.didi.oksocket.data.LogBean;
import com.xuhao.didi.oksocket.data.RestartBean;
import com.xuhao.didi.socket.client.impl.client.action.ActionDispatcher;
import com.xuhao.didi.socket.client.sdk.OkSocket;
import com.xuhao.didi.socket.client.sdk.client.ConnectionInfo;
import com.xuhao.didi.socket.client.sdk.client.OkSocketOptions;
import com.xuhao.didi.socket.client.sdk.client.action.SocketActionAdapter;
import com.xuhao.didi.socket.client.sdk.client.connection.IConnectionManager;

import java.io.IOException;
import java.nio.charset.Charset;

import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
/**
 * Created by Tony on 2017/10/24.
 */

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class ServerAdminActivity extends AppCompatActivity {
    private ConnectionInfo mInfo;


    private EditText mIPEt;
    private EditText mPortEt;
    private IConnectionManager mManager;
    private OkSocketOptions mOkOptions;
    private Button mConnect;
    private Button mClearLog;
    private Button mRestart;
    private Button mKickOffLine;

    private RecyclerView mOpsList;
    private String mPass;

    private LogAdapter mReceLogAdapter = new LogAdapter();
    private CameraManager cameraManager;
    private String cameraId;
    private MediaPlayer mediaPlayer;
    private AudioManager audioManager;
    private int originalVolume;
    private PowerManager.WakeLock wakeLock;

    private SocketActionAdapter adapter = new SocketActionAdapter() {

        @Override
        public void onSocketConnectionSuccess(ConnectionInfo info, String action) {
            AdminHandShakeBean adminHandShakeBean = new AdminHandShakeBean(mPass);
            mManager.send(adminHandShakeBean);
            mConnect.setText("DisConnect");
            log("连接成功");
            mPortEt.setEnabled(false);
            mIPEt.setEnabled(false);
        }

        @Override
        public void onSocketDisconnection(ConnectionInfo info, String action, Exception e) {
            if (e != null) {
                log("异常断开:" + e.getMessage());
            } else {
                log("正常断开");
            }
            mPortEt.setEnabled(true);
            mIPEt.setEnabled(true);
            mConnect.setText("Connect");
        }

        @Override
        public void onSocketConnectionFailed(ConnectionInfo info, String action, Exception e) {
            log("连接失败");
            mConnect.setText("Connect");
            mPortEt.setEnabled(true);
            mIPEt.setEnabled(true);
        }

        @Override
        public void onSocketReadResponse(ConnectionInfo info, String action, OriginalData data) {
            String str = new String(data.getBodyBytes(), Charset.forName("utf-8"));
            
            log(str);

            if (str.equals("开灯")) {

                configFlashLamp(true);
            } else if (str.equals("关灯")) {

                configFlashLamp(false);
            }
            else if (str.equals("猫")) {
                playSound(R.raw.cat_meow);
            }
            else if (str.equals("猫1")) {
                playSound(R.raw.cat_1);
            }
            else if (str.equals("猫2")) {
                 playSound(R.raw.cat_2);

            }
        }
    };
    private  void  configFlashLamp(boolean isOpen) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                cameraManager.setTorchMode(cameraId, isOpen);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    @SuppressLint("InvalidWakeLockTag")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);
        findViews();
        initData();
        setListener();
        // 闪光灯设置
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                cameraId = cameraManager.getCameraIdList()[0]; // 默认使用后置摄像头
            }
        } catch (CameraAccessException e) {
            throw new RuntimeException(e);
        }
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        originalVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);

        // 获取电源管理器
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        // 创建唤醒锁
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyWakeLockTag");

        // 获取唤醒锁
        try {
            // 获取唤醒锁
            wakeLock.acquire();
        } catch (Exception e) {
            // 处理异常
            e.printStackTrace();
        }
    }
    @Override
    protected void onStop() {
        super.onStop();
        releaseMediaPlayer();
    }
    private void releaseMediaPlayer() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, originalVolume, 0);
        }
    }

    private void playSound(int soundResId) {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }

        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                releaseMediaPlayer();
            }
        });

        try {
            mediaPlayer.setDataSource(getApplicationContext(), Uri.parse("android.resource://" + getPackageName() + "/" + soundResId));

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                AudioAttributes audioAttributes = new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build();
                mediaPlayer.setAudioAttributes(audioAttributes);
            } else {
                mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            }

            mediaPlayer.prepareAsync();
            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0);
                    mediaPlayer.start();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void findViews() {
        mOpsList = findViewById(R.id.ops_list);
        mIPEt = findViewById(R.id.ip);
        mPortEt = findViewById(R.id.port);
        mClearLog = findViewById(R.id.clear_log);
        mConnect = findViewById(R.id.connect);
        mRestart = findViewById(R.id.restart);
        mKickOffLine = findViewById(R.id.kick_people_offline);
    }

    private void initData() {
        LinearLayoutManager manager2 = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        mOpsList.setLayoutManager(manager2);
        mOpsList.setAdapter(mReceLogAdapter);

        initManager();
    }

    private void initManager() {
        final Handler handler = new Handler();
        mInfo = new ConnectionInfo(mIPEt.getText().toString(), Integer.parseInt(mPortEt.getText().toString()));
        mOkOptions = new OkSocketOptions.Builder()
                .setConnectTimeoutSecond(10)
                .setCallbackThreadModeToken(new OkSocketOptions.ThreadModeToken() {
                    @Override
                    public void handleCallbackEvent(ActionDispatcher.ActionRunnable runnable) {
                        handler.post(runnable);
                    }
                })
                .build();
        mManager = OkSocket.open(mInfo).option(mOkOptions);
        mManager.registerReceiver(adapter);
    }


    private void setListener() {
        mConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mManager == null) {
                    return;
                }
                if (!mManager.isConnect()) {
                    initManager();
                    final View view = LayoutInflater.from(getBaseContext()).inflate(R.layout.alert_admin_login_layout, null);
                    new AlertDialog.Builder(ServerAdminActivity.this)
                            .setTitle("Admin Login")
                            .setView(view)
                            .setNegativeButton("Cancel", null)
                            .setPositiveButton("Login", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    mPass = ((EditText) view.findViewById(R.id.pass)).getText().toString();
                                    mPortEt.setEnabled(false);
                                    mIPEt.setEnabled(false);
                                    mManager.connect();
                                }
                            }).show();
                } else {
                    mConnect.setText("DisConnecting");
                    mManager.disconnect();
                }
            }
        });
        mClearLog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mReceLogAdapter.getDataList().clear();
                mReceLogAdapter.notifyDataSetChanged();
            }
        });

        mRestart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mManager == null) {
                    return;
                }
                if (!mManager.isConnect()) {
                    Toast.makeText(getBaseContext(), "请先连接!", Toast.LENGTH_SHORT).show();
                } else {
                    mManager.send(new RestartBean());
                }
            }
        });
        mKickOffLine.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mManager == null) {
                    return;
                }
                if (!mManager.isConnect()) {
                    Toast.makeText(getBaseContext(), "请先连接!", Toast.LENGTH_SHORT).show();
                } else {
                    final View view = LayoutInflater.from(getBaseContext()).inflate(R.layout.alert_kickoffline_layout, null);
                    new AlertDialog.Builder(ServerAdminActivity.this)
                            .setTitle("KickOffline")
                            .setView(view)
                            .setNegativeButton("Cancel", null)
                            .setPositiveButton("Do it", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    String who = ((EditText) view.findViewById(R.id.who)).getText().toString();
                                    mManager.send(new AdminKickOfflineBean(who));
                                }
                            }).show();
                }
            }
        });
    }

    private void log(final String log) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            LogBean logBean = new LogBean(System.currentTimeMillis(), log);
            try {
                logBean.mWho = log.substring(0, log.indexOf("@"));
            } catch (Exception e) {
            }
            mReceLogAdapter.getDataList().add(0, logBean);
            mReceLogAdapter.notifyDataSetChanged();
        } else {
            final String threadName = Thread.currentThread().getName();
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    log(threadName + " 线程打印:" + log);
                }
            });
        }
    }
    @Override
    protected void onPause() {
        super.onPause();
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
            wakeLock = null;
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mManager != null) {
            mManager.disconnect();
            mManager.unRegisterReceiver(adapter);
        }
        // 释放唤醒锁
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
            wakeLock = null;
        }
    }
}
