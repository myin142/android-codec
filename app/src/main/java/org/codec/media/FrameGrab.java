package org.codec.media;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import java.io.FileOutputStream;
import java.io.IOException;

public class FrameGrab {
    String TAG = "FrameGrab";

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

    public void release(){
        codec.release();
        output.release();
        mGLThread.quit();
    }
    public void setTargetSize(int width, int height){
        this.width = width;
        this.height = height;
    }

    // Call before init()
    public void setSource(String path){
        codec.setSource(path);
    }

    // Call before seekToFrame() and getFrameAt()
    // Create Decoder
    public void init(){
        Log.d(TAG, "Initializing Codec");
        codec.init();

        // Run OnFrameAvailable on different thread
        mGLHandler.post(new Runnable() {
            @Override
            public void run() {

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
        });
    }

    // Call before getFrameAt() and after init()
    // Go to previous frame of framenumber
    public void seekToFrame(int frame){
        codec.seekTo(frame);
    }

    // Decode Frame and wait for frame to be processed
    public void getFrameAt(final int frame){
        mGLHandler.post(new Runnable() {
            @Override
            public void run() {
                codec.getFrameAt(frame); // Has to be in a different thread than framelistener
            }
        });
        output.awaitFrame(); // Wait for Frame to be available
    }

    // Call FrameProcess before another getFrameAt(), otherwise Bitmap will be overwritten
    /*** 2 Possible Frame Processes ***/

    public Bitmap getBitmap(){
        return output.getBitmap();
    }

    public void saveBitmap(String location){
        Bitmap frame = getBitmap();
        bmToFile(frame, location, Bitmap.CompressFormat.JPEG, 100);
    }
    public void saveBitmap(String location, Bitmap.CompressFormat format, int quality){
        Bitmap frame = getBitmap();
        bmToFile(frame, location, format, quality);
    }

    /*** END OF FRAME PROCESSES ***/

    // Save Bitmap to File, Default: JPG, Quality 100
    private void bmToFile(Bitmap bm, String location, Bitmap.CompressFormat format, int quality){
        Log.d(TAG, "Frame saved");
        try {
            FileOutputStream out = new FileOutputStream(location);
            bm.compress(format, quality, out);
            out.close();
        }catch(IOException e){
            e.printStackTrace();
        }
    }

}