package com.example.webrtcservermodulee;

import static android.service.controls.ControlsProviderService.TAG;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Path;
import android.os.Build;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

import androidx.annotation.RequiresApi;

public class Fling extends AccessibilityService {

    public static Fling mFling;
    public static Fling getInstance(){
        return mFling;
    }
    private static final String ACTION_GESTURE = "action_gesture";

    @Override
    public void onAccessibilityEvent(AccessibilityEvent accessibilityEvent) {

    }

    @Override
    public void onInterrupt() {

    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e(ContentValues.TAG, "onStartCommand: " );
        return super.onStartCommand(intent, flags, startId);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void simulateGesture(Integer x1, Integer y1, Integer x2, Integer y2, int duration){
        GestureDescription.Builder mBuilder=new GestureDescription.Builder();
        if(x2==null || y2==null){
            Path clickPath=new Path();
            clickPath.moveTo(x1,y1);
            GestureDescription.StrokeDescription mStrokeDescription=
                    new GestureDescription.StrokeDescription(clickPath,0,duration);
            mBuilder.addStroke(mStrokeDescription);
            Log.e(TAG, "simulateGesture: Gesture simulated : TAP" );
        }else{
            Path clickPath=new Path();
            clickPath.moveTo(x1,y1);
            clickPath.lineTo(x2,y2);
            GestureDescription.StrokeDescription mStrokeDescription=
                    new GestureDescription.StrokeDescription(clickPath,0,duration);
            mBuilder.addStroke(mStrokeDescription);
            Log.e(TAG, "simulateGesture: Gesture simulated : SWIPE" );

        }

            boolean result=dispatchGesture(mBuilder.build(), new GestureResultCallback() {
                @Override
                public void onCompleted(GestureDescription gestureDescription) {
                    Log.e(TAG, "onCompleted: Gesture completed successfully" );
                    super.onCompleted(gestureDescription);
                }

                @Override
                public void onCancelled(GestureDescription gestureDescription) {
                    Log.e(TAG, "onCancelled: Gesture cancelled unsuccessfully" );
                    super.onCancelled(gestureDescription);
                }
            }, null);
        Log.e(TAG, "simulateGesture: Gesture dispatched "+result );

    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        this.mFling=this;
        Log.e(TAG, "onServiceConnected: " );
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }
}



