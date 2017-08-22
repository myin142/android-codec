package org.codec.media;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by min on 22/08/2017.
 */

public class FrameGrab {
    static String TAG = "FrameGrab";

    HandlerThread mGLThread = null;
    Handler mGLHandler = null;
    VideoDecoder codec = null;
    CodecOutput output = null;

    int width = -1;
    int height = -1;

    public FrameGrab(){
        // Create Handler Thread
        Log.d(TAG, "Create Handler Thread");
        mGLThread = new HandlerThread("FrameGrab");
        mGLThread.start();
        mGLHandler = new Handler(mGLThread.getLooper());

        output = new CodecOutput();
        codec = new VideoDecoder();
    }

    // Call before seekToFrame() and getFrameAt()
    // Create Decoder
    public void init(){
        codec.init();

        // Set custom frame size if set, else original size
        if(width != -1){
            output.setWidth(width);
            output.setHeight(height);
        }else{
            output.setWidth(codec.getWidth());
            output.setHeight(codec.getHeight());
        }

        output.init();
        codec.setSurface(output.getSurface());
        codec.startDecoder();
    }

    // Decode Frame and wait for frame to be processed
    public void getFrameAt(final int frame){
        mGLHandler.post(new Runnable() {
            @Override
            public void run() {
                long startTime = System.currentTimeMillis();
                codec.getFrameAt(frame); // Has to be in a different thread than framelistener
                long endTime = System.currentTimeMillis();
                long totalTime = (endTime - startTime) / 1000;
                Log.d(TAG, "Total Time: " + totalTime + "s");
            }
        });
    }

    // Call before getFrameAt() and after init()
    // Go to previous frame of framenumber
    public void seekToFrame(int frame){
        codec.seekTo(frame);
    }

    public void setTargetSize(int width, int height){
        this.width = width;
        this.height = height;
    }

    public void setSource(String path){
        codec.setSource(path);
    }

    // Cleaning everything up
    public void release(){
        output.release();
        codec.release();
        mGLThread.quit();
    }

    /* 2 Possible Frame Processes */
    public Bitmap getBitmap(){
        Bitmap frame = null;
        while(frame == null){
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            frame = output.getBitmap();
        }
        frameProcessed();
        return frame;
    }

    public void saveBitmap(String location){
        Bitmap frame = getBitmap();
        bmToFile(frame, location);
    }
    /* END OF FRAME PROCESSES */

    // Save Bitmap to File
    // Default JPG, Quality 100
    private void bmToFile(Bitmap bm, String location){
        Log.d(TAG, "Frame saved");
        try {
            FileOutputStream out = new FileOutputStream(location);
            bm.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.close();
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    // Notify Codec To Continue
    private void frameProcessed(){
        output.frameProcessed();
    }

}
