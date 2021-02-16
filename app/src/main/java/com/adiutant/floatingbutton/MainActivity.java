package com.adiutant.floatingbutton;
import android.Manifest;
import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.adiutant.floatingbutton.services.AutoClickService;
import com.adiutant.floatingbutton.services.AutoServiceHelper;
import com.adiutant.floatingbutton.services.BackgroundService;
import com.adiutant.floatingbutton.services.FloatingService;
import com.adiutant.floatingbutton.services.RecordService;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private Button startService,stopService,recButton;
    public static int ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE= 5469;
    private static final int RECORD_REQUEST_CODE  = 101;
    private static final int STORAGE_REQUEST_CODE = 102;
    private static final int PERMISSION_CODE = 110;

    public static Handler messageHandler;
    private MediaProjectionManager projectionManager;
    private MediaProjection mediaProjection;
    private RecordService recordService;
    private AutoServiceHelper helper;
    Intent fintent;

//    private void checkPermission() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            if (!Settings.canDrawOverlays(this)) {
//                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
//                        Uri.parse("package:" + getPackageName()));
//                startActivityForResult(intent, ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE);
//            }
//        }
//    }
    private Boolean checkAccess() {
        String string = getString(R.string.accessibility_service_id);
       AccessibilityManager manager = (AccessibilityManager) getSystemService(Context.ACCESSIBILITY_SERVICE);
        List<AccessibilityServiceInfo> list = manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC);
        for (AccessibilityServiceInfo id : list
             ) {
            if (string.equals(id.getId())) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        Boolean hasPermission = checkAccess();
        if (!hasPermission) {
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
        }
        if (!Settings.canDrawOverlays(this)) {
            askPermission();
        }
    }
  //  @TargetApi(Build.VERSION_CODES.M)
    private void askPermission() {
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:"+getPackageName()));
        startActivityForResult(intent, PERMISSION_CODE);

    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        helper = new AutoServiceHelper();
        projectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        setContentView(R.layout.activity_main);
        //checkPermission();
        startService = (Button) findViewById(R.id.startService);
        stopService = (Button) findViewById(R.id.stopService);
        recButton = findViewById(R.id.recButton);
        fintent = new Intent(this, BackgroundService.class);
        startForegroundService(fintent);

        recButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                recordService.setMediaProject(mediaProjection);
                recordService.startRecord();
            }
        });

        final Intent mIntent = new Intent(getApplication(),FloatingService.class);
        startService.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
              != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this,
                  new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_REQUEST_CODE);
        }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && Settings.canDrawOverlays(MainActivity.this.getApplicationContext())) {
                    new Handler().postDelayed(
                            new Runnable() {
                                @Override
                                public void run() {
                                    if (recordService.isRunning()) {
                                        recordService.stopRecord();
                                    } else {
                                        Intent captureIntent = projectionManager.createScreenCaptureIntent();
                                        startActivityForResult(captureIntent, RECORD_REQUEST_CODE);
                                    }
                                }
                            },100);
                    mIntent.putExtra("MESSENGER", new Messenger(messageHandler));
                    bindService(mIntent, connectionFloating, BIND_AUTO_CREATE);
                    onBackPressed();
                } else {
                    askPermission();
                }
            }
        });
        stopService.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                unbindService(connectionFloating);
                try
                {
                    stopService(new Intent(getApplication(), FloatingService.class));
                }
                catch (Exception e)
                {

                }
            }
        });
        messageHandler = new MessageHandler(recButton);

        // 绑定服务
        Intent intent = new Intent(this, RecordService.class);
        bindService(intent, connection, BIND_AUTO_CREATE);




    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(connection);
        stopService(fintent);
       // helper.stopckick();
    }

//    @Override
//    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//        if (requestCode == STORAGE_REQUEST_CODE ) {
//            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
//                finish();
//            }
//        }
//    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RECORD_REQUEST_CODE && resultCode == RESULT_OK) {
            mediaProjection = projectionManager.getMediaProjection(resultCode, data);
//            //recordService.setMediaProject(mediaProjection);
           // recordService.startRecord();
        }
    }
    @Override
    public void onBackPressed()
    {
        moveTaskToBack(true);
    }


    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            DisplayMetrics metrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(metrics);
            RecordService.RecordBinder binder = (RecordService.RecordBinder) service;
            recordService = binder.getRecordService();
            recordService.setConfig(metrics.widthPixels, metrics.heightPixels, metrics.densityDpi);

        }

        @Override
        public void onServiceDisconnected(ComponentName argComponentName) {

        }
    };
    private ServiceConnection connectionFloating = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {

        }

        @Override
        public void onServiceDisconnected(ComponentName argComponentName) {

        }
    };



    public void checkStatusService(){
        if(RecordService.serviceStatus!=null){
            if(RecordService.serviceStatus == true){
                //do something
                //textview.text("Service is running");
            }else{
                //do something
                //textview.text("Service is not running");
            }
        }
    }
    public static class MessageHandler extends Handler {
        private Button startB;
        public MessageHandler(Button start) {
            startB = start;
        }
        @Override
        public void handleMessage(Message message) {
            int state = message.arg1;
            switch (state) {
                case 1:
                    startB.performClick();
                    break;
                case 0:
                    break;
            }
        }
    }
}