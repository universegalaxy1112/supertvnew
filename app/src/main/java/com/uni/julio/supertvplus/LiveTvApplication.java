package com.uni.julio.supertvplus;

import android.content.Context;
import android.os.Handler;
import android.text.TextUtils;

import androidx.multidex.MultiDexApplication;

import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.gson.Gson;
import com.uni.julio.supertvplus.model.Subscription;
import com.uni.julio.supertvplus.model.User;
import com.uni.julio.supertvplus.utils.BillingClientLifecycle;
import com.uni.julio.supertvplus.utils.DataManager;
import com.uni.julio.supertvplus.utils.Device;

import org.json.JSONException;
import org.json.JSONObject;

public class LiveTvApplication extends MultiDexApplication {

    private static Context applicationContext;
    public Handler handler;
    public static User user = null;
    public static Context appContext = null;

    @Override
    public void onCreate() {
        super.onCreate();
        handler=new Handler();
        applicationContext = getApplicationContext();
        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {

            }
        });
    }

    public BillingClientLifecycle getBillingClientLifecycle() {

        return BillingClientLifecycle.getInstance(this);

    }

    public static boolean saveUser(String password, JSONObject jsonObject) {

        try{
            User user = new User();
            user.setName(jsonObject.getString("user"));
            user.setEmail(jsonObject.getString("email"));
            user.setPassword(password);
            user.setUser_agent(jsonObject.getString("user-agent"));
            user.setDevice(Device.getModel() + " - " + Device.getFW());
            user.setVersion(Device.getVersion());
            user.setDeviceId(Device.getIdentifier());
            user.setAdultos(jsonObject.getInt("adultos"));
            user.setDevice_num(jsonObject.getInt("device_num"));
            if (!jsonObject.isNull("pin")) {
                DataManager.getInstance().saveData("adultsPassword", jsonObject.getString("pin"));
            }
            Subscription subscription = new Subscription();
            try{
                JSONObject subscriptionObject = new JSONObject(jsonObject.getString("subscription"));
                subscription.setIsActive(subscriptionObject.getInt("active"));
                subscription.setExpiration_date(subscriptionObject.getString("expiration_date"));
                subscription.setIsCancelled(subscriptionObject.getInt("cancelled"));
                subscription.setIsOnHold(subscriptionObject.getInt("onhold"));
                subscription.setIsExpired(subscriptionObject.getInt("expired"));
                subscription.setPurchaseToken(subscriptionObject.getString("purchaseToken"));
                subscription.setMembership(subscriptionObject.getInt("membership"));
            }catch (JSONException e) {
                e.printStackTrace();
            }
            user.setSubscription(subscription);
            DataManager.getInstance().saveData("theUser", new Gson().toJson(user));
            LiveTvApplication.user = user;
        }catch (JSONException e) {
            e.printStackTrace();
            return false;
        }
        return true;

    }

    public static User getUser(){

        if(LiveTvApplication.user == null){

            String theUser = DataManager.getInstance().getString("theUser","");
            if(!TextUtils.isEmpty(theUser)) {
                LiveTvApplication.user = new Gson().fromJson(theUser, User.class);
            }

        }

        return LiveTvApplication.user;

    }

    public static Context getAppContext() {
        return applicationContext;

    }

    public DataSource.Factory buildDataSourceFactory(DefaultBandwidthMeter bandwidthMeter) {
        return new DefaultDataSourceFactory(this, bandwidthMeter,
                buildHttpDataSourceFactory(bandwidthMeter));
    }

    public HttpDataSource.Factory buildHttpDataSourceFactory(DefaultBandwidthMeter bandwidthMeter) {
       String userAgent = LiveTvApplication.getUser().getUser_agent();
        return new DefaultHttpDataSourceFactory(userAgent,bandwidthMeter);
    }

   /* public RenderersFactory buildRenderersFactory(boolean preferExtensionRenderer) {
        @DefaultRenderersFactory.ExtensionRendererMode
        int extensionRendererMode =
                useExtensionRenderers()
                        ? (preferExtensionRenderer
                        ? DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER
                        : DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
                        : DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF;
        return new DefaultRenderersFactory( this)
                .setExtensionRendererMode(extensionRendererMode);
    }*/

    public boolean useExtensionRenderers() {
        return BuildConfig.FLAVOR.equals("withExtensions");
    }

}
