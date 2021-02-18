package com.adiutant.floatingbutton.services;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.os.Binder;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Process;
import android.util.DisplayMetrics;
import android.util.Log;

import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;

public class RecordService extends Service {


    public static Boolean serviceStatus;
    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;
    private ImageReader mImageReader;
    private AutoServiceHelper helper;

    private boolean running;
    private int width = 720;
    private int height = 1080;
    private int dpi;

    final String TAG = "test";

    @Override
    public IBinder onBind(Intent intent) {
        return new RecordBinder();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        serviceStatus = true;
        HandlerThread serviceThread = new HandlerThread("service_thread", Process.THREAD_PRIORITY_BACKGROUND);
        serviceThread.start();
        running = false;
        helper = new AutoServiceHelper();
    }

    public void setMediaProject(MediaProjection project) {
        mediaProjection = project;
    }

    public boolean isRunning() {
        return running;
    }

    public void setConfig(int width,int height,int dpi){
        this.width = width;
        this.height = height;
        this.dpi = dpi;
    }

    @SuppressLint("WrongConstant")
    public boolean startRecord(){
        if(mediaProjection == null || running){
            return false;
        }

        mImageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 1);
        createVirtualDisplay();
        mImageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader argImageReader) {
                try{
                    initRecorder(mImageReader);
                }catch (IllegalStateException argE){
                }
               stopRecord();
                if( virtualDisplay!=null) {
                    virtualDisplay.release();
                }
            }
        },new Handler());

       // running = true;
        return true;
    }

    public boolean stopRecord() {
        if (!running) {
            return false;
        }
        running = false;

        if( virtualDisplay!=null){
            virtualDisplay.release();
        }

        mediaProjection.stop();

        return true;
    }

    private void createVirtualDisplay() {
        virtualDisplay = mediaProjection.createVirtualDisplay("MainScreen", width, height, dpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, mImageReader.getSurface(), null, null);
    }
   // public static int pxToDp(int px) { return (int) (px / Resources.getSystem().getDisplayMetrics().density); }
   public int pxToDp(int px) { return (int) (px / dpi); }

   private boolean neighbors(Bitmap b, int pixel,int[] pixelLocation)
   {
       int x = pixelLocation[0];
       int y = pixelLocation[1];
       return b.getPixel(x + 1, y) == pixel && b.getPixel(x - 1, y) == pixel && b.getPixel(x, y + 1) == pixel && b.getPixel(x, y - 1) == pixel;


   }
   private boolean testBronze(int curPix)
   {
       int redPix = Color.red(curPix);
       int greenPix = Color.green(curPix);
       int bluePix = Color.blue(curPix);
       return redPix> 120&&greenPix>50&&greenPix<80&&bluePix>20&&bluePix<60;
   }
    private boolean goldChest(int curPix)
    {
        int redPix = Color.red(curPix);
        int greenPix = Color.green(curPix);
        int bluePix = Color.blue(curPix);
        return redPix< 50&&greenPix>120&&greenPix<150&&bluePix>120&&bluePix<160;
    }

    private int[] findPixels(Bitmap bitmap)
    {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        int s = w * h;
        int currentPix=0;

        for (int i = (int) (w*0.6); i<w*0.9; i++)
        {
           for (int j = (int) (h*0.8); j<h*0.95; j++) {
                currentPix = bitmap.getPixel(i,j);
               int redPix = Color.red(currentPix);
               int greenPix = Color.green(currentPix);
               int bluePix = Color.blue(currentPix);
               //System.out.println(bluePix);
                if (testBronze(currentPix))
                {
                    System.out.println(i);
                    System.out.println(j);
                    System.out.println(currentPix);
                    return new int[] {i,j};
                }
            }
        }
        return null;
    }

    private void initRecorder(ImageReader argImageReader) {
        helper = new AutoServiceHelper();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd_hh_mm_ss");
        String strDate = dateFormat.format(new java.util.Date());
        String pathImage = Environment.getExternalStorageDirectory().getPath()+"/Pictures/";

//        //检测目录是否存在
//        File localFileDir = new File(pathImage);
//        if(!localFileDir.exists())
//        {
//            localFileDir.mkdirs();
//            Log.d("DaemonService","创建Pictures目录成功");
//        }

        String nameImage = pathImage+strDate+".png";

        Image localImage = argImageReader.acquireLatestImage();

        // 4.1 获取图片信息，转换成bitmap
        final int width = argImageReader.getWidth();
        final int height = argImageReader.getHeight();


        final Image.Plane[] localPlanes = localImage.getPlanes();
      //  localImage.close();
        final ByteBuffer localBuffer = localPlanes[0].getBuffer();
        int pixelStride = localPlanes[0].getPixelStride();
        int rowStride = localPlanes[0].getRowStride();
        int rowPadding = rowStride - pixelStride * width;

        // 4.1 Image对象转成bitmap
        Bitmap localBitmap = Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888);
        localBitmap.copyPixelsFromBuffer(localBuffer);
        int w = localBitmap.getWidth();
        int h = localBitmap.getHeight();
        int s = w * h;
        //int[] pixels = new int[s];
        //localBitmap.getPixels(pixels, 0, w, 0, 0, w, h);
//
//        for (int i =0;i<pixels.length;i++)
//        {
//            if (pixels[i] == 12566335)
//            {
//
//                break;
//            }
//        }
        localBitmap.createBitmap(localBitmap, 0, 0, width, height);
        final int[] locationClick = findPixels(localBitmap);
        if (locationClick != null) {
            new Thread(
                    new Runnable() {
                        @Override
                        public void run() {
                            helper.click(locationClick[0], locationClick[1]);



                        }
                    }).start();
            try {
                Thread.sleep(400);
                new Thread(
                        new Runnable() {
                            @Override
                            public void run() {
                                helper.click((int)(locationClick[0]+ width * 0.2), (int) (locationClick[1] - height * 0.52));
                                helper.click((int)(locationClick[0]+ width * 0.22), (int) (locationClick[1] - height * 0.5));
                                helper.click((int)(locationClick[0]+ width * 0.23), (int) (locationClick[1] - height * 0.53));
                                helper.click((int)(locationClick[0]+ width * 0.21), (int) (locationClick[1] - height * 0.51));
                            }
                        }).start();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }


        }
//        if (localBitmap != null) {
//            File f = new File(nameImage);
//            if (f.exists()) {
//                f.delete();
//            }
//            try {
//                FileOutputStream out = new FileOutputStream(f);
//                localBitmap.compress(Bitmap.CompressFormat.PNG, 90, out);
//                out.flush();
//                out.close();
//                Log.d("DaemonService", "startCapture-> 保存文件成功："+nameImage);
//
//
//            } catch (FileNotFoundException e) {
//                // TODO Auto-generated catch block
//                e.printStackTrace();
//            } catch (IOException e) {
//                // TODO Auto-generated catch block
//                e.printStackTrace();
//            }
//        }
argImageReader.close();
    }

    public class RecordBinder extends Binder {
        public RecordService getRecordService() {
            return RecordService.this;
        }
    }
}
