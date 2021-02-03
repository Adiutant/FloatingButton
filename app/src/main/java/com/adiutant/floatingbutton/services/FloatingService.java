package com.adiutant.floatingbutton.services;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.hardware.display.VirtualDisplay;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;


import com.adiutant.floatingbutton.ExtensionsKt;
import com.adiutant.floatingbutton.R;
import com.adiutant.floatingbutton.TouchAndDragListener;

import java.util.Timer;
import java.util.TimerTask;

import kotlin.Unit;
import kotlin.jvm.functions.Function0;

public class FloatingService extends Service {

   // private WindowManager windowManager;
    private ImageView chatHead;
    private WindowManager.LayoutParams params;
    private  String CHANNEL_ID = "channelID";
    private MediaProjection sMediaProjection;
    private ImageReader mImageReader;
    private VirtualDisplay mVirtualDisplay;
    private Messenger messageHandler;
    private WindowManager manager;
    private View view;
    private int xForRecord = 0;
    private int yForRecord = 0;
    private int[] location = new int[2];
    private Integer startDragDistance=0;
    private Timer timer= null;
    private TextView click;
    private AutoServiceHelper helper;


//    public void torchMode()
//    {
//        CameraManager camManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
//        String cameraId = null; // Usually back camera is at 0 position.
//        try {
//            cameraId = camManager.getCameraIdList()[0];
//            camManager.setTorchMode(cameraId, isTorch);   //Turn ON
//            isTorch =!isTorch;
//        } catch (CameraAccessException e) {
//            e.printStackTrace();
//        }
//    }





    @Override
    public void onCreate() {
        super.onCreate();
        startDragDistance = ExtensionsKt.dp2px(this,10f);
        view = LayoutInflater.from(this).inflate(R.layout.widget, null);
        click = view.findViewById(R.id.button);
        //mana = (WindowManager) getSystemService(WINDOW_SERVICE);
        //chatHead = new ImageView(this);
        //chatHead.setImageResource(R.drawable.face1);
        helper = new AutoServiceHelper();
        int overlayParam;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
             overlayParam = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
             overlayParam = WindowManager.LayoutParams.TYPE_PHONE;
        }
        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                overlayParam,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        manager = (WindowManager) getSystemService(Context.WINDOW_SERVICE) ;
        manager.addView(view, params);

        view.setOnTouchListener( new TouchAndDragListener(params, startDragDistance,
                new Function0<Unit>() {
                    @Override
                    public Unit invoke() {
                        startCheck();
                        return null;
                    }
                },
                new Function0<Unit>() {
                    @Override
                    public Unit invoke() {
                        manager.updateViewLayout(view, params);
                        return null;
                    }
                }
                ));
       // windowManager.addView(chatHead, params);
    }

    private boolean isOn = false;


    private void startCheck()
    {
        if (isOn) {
            timer.cancel();
        } else {
            timer = new Timer();
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    sendMessage(1);
                }
            },0,2000);
        }
        isOn = !isOn;
        if (isOn) {
            click.setText("ON");
        }
        else
        {
            click.setText("OFF");
        }
    }

    @SuppressLint("SetTextI18n")
    private void viewOnClick() {
        if (isOn) {
            timer.cancel();
        } else {
            timer = new Timer();
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                   view.getLocationOnScreen(location);
                   helper.click(location[0] + click.getRight() + 10,location[1] + click.getBottom() + 10);
                }
            },0,1000);
        }
        isOn = !isOn;
        if (isOn) {
            click.setText("ON");
        }
           else
        {
            click.setText("OFF");
        }

    }
    public void sendMessage(int i) {
        Message message = Message.obtain();
        switch (i) {
            case 1:
                message.arg1 = 1;
                break;
            case 0 :
                message.arg1 = 0;
                break;
        }
        try {
            messageHandler.send(message);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        timer.cancel();
        manager.removeView(view);

    }

    @Override
    public IBinder onBind(Intent intent) {
        Bundle extras = intent.getExtras();
        messageHandler = (Messenger) extras.get("MESSENGER");
        return null;
    }

@Override
public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
  //  "FloatingClickService onConfigurationChanged".logd()
   int x = params.x;
    int y = params.y;
    params.x = xForRecord;
    params.y = yForRecord;
    xForRecord = x;
    yForRecord = y;
    manager.updateViewLayout(view, params);
}
}
