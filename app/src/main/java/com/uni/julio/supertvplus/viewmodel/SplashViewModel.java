package com.uni.julio.supertvplus.viewmodel;

import android.app.ProgressDialog;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;
import com.google.gson.Gson;
import com.uni.julio.supertvplus.LiveTvApplication;
import com.uni.julio.supertvplus.R;
import com.uni.julio.supertvplus.listeners.DownloaderListener;
import com.uni.julio.supertvplus.listeners.StringRequestListener;
import com.uni.julio.supertvplus.model.Subscription;
import com.uni.julio.supertvplus.model.User;
import com.uni.julio.supertvplus.utils.DataManager;
import com.uni.julio.supertvplus.utils.Device;
import com.uni.julio.supertvplus.utils.networing.Downloader;
import com.uni.julio.supertvplus.utils.networing.NetManager;

import org.json.JSONException;
import org.json.JSONObject;

public class SplashViewModel implements SplashViewModelContract.ViewModel, StringRequestListener, DownloaderListener {

    private NetManager netManager;
    private SplashViewModelContract.View viewCallback;
    private User user;
     public SplashViewModel(SplashViewModelContract.View splash) {
        this.viewCallback= splash;
        netManager = NetManager.getInstance();
    }

    @Override
    public void onViewResumed() {

    }

    @Override
    public void onViewAttached(@NonNull Lifecycle.View viewCallback) {
        this.viewCallback = (SplashViewModelContract.View) viewCallback;
    }

    @Override
    public void onViewDetached() {
        this.viewCallback = null;
    }

    @Override
    public void login() {
        String email = "";
        String password = "";
        String id = "";
        user = LiveTvApplication.getUser();
        if(user != null) {
            email = user.getEmail();
            password = user.getPassword();
            id = user.getDeviceId();
        }
        if(TextUtils.isEmpty(email) || TextUtils.isEmpty(password) || TextUtils.isEmpty(id)) {

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    viewCallback.onLoginCompleted(false);
                }
            }, 2000);
        }
        else {
            netManager.performLoginCode(email, password, id, "", this);
        }
    }

    @Override
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
                       if(LiveTvApplication.saveUser(user.getPassword(), jsonObject)) {
                           viewCallback.onLoginCompleted(true);
                           return;
                       }
                    }
                }
                if (jsonObject.has("android_version")) {
                    Log.d("version",Device.getVersionInstalled());
                    String a=Device.getVersionInstalled().replaceAll("\\.", "");
                    if (!jsonObject.getString("android_version").equals("")&&!Device.getVersionInstalled().replaceAll("\\.", "").equals(jsonObject.getString("android_version"))) {
                        this.viewCallback.onCheckForUpdateCompleted(true, jsonObject.getString("link_android") + "/android" + jsonObject.getString("android_version") + ".apk");
                        return;
                    }
                    this.viewCallback.onCheckForUpdateCompleted(false, null);
                    return;
                }
            } catch (JSONException e) {
//                e.printStackTrace();
            }
        }
        DataManager.getInstance().saveData("theUser","");
        viewCallback.onLoginCompleted(false);
    }

    @Override
    public void onError() {
        viewCallback.onLoginCompleted(false);
    }
    public void checkForUpdate() {
        this.netManager.performCheckForUpdate(this);
    }

    public void downloadUpdate(String location, ProgressDialog progress) {
        Downloader.getInstance().performDownload(location, progress, this);
    }
    public void onDownloadError(int error) {
        this.viewCallback.onDownloadUpdateError(error);
    }

    public void onDownloadComplete(String location) {
        this.viewCallback.onDownloadUpdateCompleted(location);
    }
}