package com.uni.julio.supertvplus.utils;

import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;
import com.uni.julio.supertvplus.LiveTvApplication;
import com.uni.julio.supertvplus.listeners.MessageCallbackListener;
import com.uni.julio.supertvplus.listeners.StringRequestListener;
import com.uni.julio.supertvplus.model.Subscription;
import com.uni.julio.supertvplus.model.User;
import com.uni.julio.supertvplus.utils.networing.NetManager;
import com.uni.julio.supertvplus.utils.networing.WebConfig;
import com.uni.julio.supertvplus.view.EmailVerifyActivity;
import com.uni.julio.supertvplus.view.LoadingActivity;
import com.uni.julio.supertvplus.view.LoginActivity;
import com.uni.julio.supertvplus.view.MainActivity;
import com.uni.julio.supertvplus.view.SignUp;
import com.uni.julio.supertvplus.view.SplashActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class Tracking implements StringRequestListener {
    private static Tracking mInstance;
    private String action = "IDLE";
    public Handler handler = new Handler();
    private boolean isTracking = false;
    private boolean sleeping = false;
    private User user = null;
    private Runnable trackingThread = new Runnable() {
        public void run() {
            Tracking.this.track();
            Tracking.this.handler.postDelayed(this, 120000);
        }
    };
    public void enableTrack(boolean isTracking){
        this.isTracking = isTracking;
    }
    public void enableSleep(boolean isSleeping){
        this.sleeping = isSleeping;
    }
    public boolean getSleep(){
        return sleeping;
    }
    public static Tracking getInstance() {
        if (mInstance == null) {
            mInstance = new Tracking();
        }

        return mInstance;
    }

    public void onStart() {
        this.isTracking = true;
        this.handler.removeCallbacks(trackingThread);
        this.handler.postDelayed(trackingThread,0);
    }

    public void track() {
        if(this.isTracking && LiveTvApplication.appContext != null && !(LiveTvApplication.appContext instanceof LoadingActivity || LiveTvApplication.appContext instanceof MainActivity  || LiveTvApplication.appContext instanceof SplashActivity || LiveTvApplication.appContext instanceof LoginActivity || LiveTvApplication.appContext instanceof SignUp || LiveTvApplication.appContext instanceof EmailVerifyActivity)){
            try{
                user = LiveTvApplication.getUser();
                if(user != null) {
                    NetManager.getInstance().performLoginCode(user.getEmail(), user.getPassword(), user.getDeviceId(), (URLEncoder.encode(this.action, "UTF-8")).replace("+", "%20"), this);
                }
            }catch (Exception e){
                e.printStackTrace();
            }

        }
    }

    public void setAction(String action2) {
        this.action = action2;
    }

    public void onError() {
        Log.d("DNLS", "Tracking ERROR");
    }

    public void onCompleted(String response) {
        if(!TextUtils.isEmpty(response)) {
            try {
                JSONObject jsonObject = new JSONObject(response);
                if (jsonObject.has("status") && "1".equals(jsonObject.getString("status"))) {
                    String userAgent =  jsonObject.getString("user-agent");
                    if (!jsonObject.isNull("pin")) {
                        DataManager.getInstance().saveData("adultsPassword", jsonObject.getString("pin"));
                    }
                    if(!TextUtils.isEmpty(userAgent)) {

                        String message = jsonObject.getString("message");
                        if(!message.equals("") && !message.equals("null")) {
                            Dialogs.showCustomDialog(LiveTvApplication.appContext, "Atencion", message, new MessageCallbackListener() {
                                @Override
                                public void onDismiss() {

                                }

                                @Override
                                public void onAccept() {

                                }

                                @Override
                                public void onError() {

                                }
                            });
                        }
                        if(LiveTvApplication.saveUser(user.getPassword(), jsonObject)) {
                            return;
                        }
                    }
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        DataManager.getInstance().saveData("theUser", "");

    }

}
