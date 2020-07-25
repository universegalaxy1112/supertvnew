package com.uni.julio.supertvplus.utils;

import android.app.Activity;
import android.util.Log;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.OnLifecycleEvent;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.AcknowledgePurchaseResponseListener;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.android.billingclient.api.SkuDetailsResponseListener;
import com.google.gson.Gson;
import com.uni.julio.supertvplus.Constants;
import com.uni.julio.supertvplus.LiveTvApplication;
import com.uni.julio.supertvplus.helper.SingleLiveEvent;
import com.uni.julio.supertvplus.listeners.StringRequestListener;
import com.uni.julio.supertvplus.model.User;
import com.uni.julio.supertvplus.utils.networing.NetManager;
import com.uni.julio.supertvplus.utils.networing.WebConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BillingClientLifecycle implements LifecycleObserver, PurchasesUpdatedListener,
        BillingClientStateListener, SkuDetailsResponseListener {

    private static final String TAG = "BillingLifecycle";

    /**
     * The purchase event is observable. Only one observer will be notified.
     */

    /**
     * Purchases are observable. This list will be updated when the Billing Library
     * detects new or existing purchases. All observers will be notified.
     */
    public MutableLiveData<List<Purchase>> purchases = new MutableLiveData<>();

    /**
     * SkuDetails for all known SKUs.
     */
    public MutableLiveData<Map<String, SkuDetails>> skusWithSkuDetails = new MutableLiveData<>();

    private static volatile BillingClientLifecycle INSTANCE;
    public SingleLiveEvent<List<Purchase>> purchaseUpdateEvent = new SingleLiveEvent<>();

    private LiveTvApplication app;
    private BillingClient billingClient;

    private BillingClientLifecycle(LiveTvApplication app) {
        this.app = app;
    }

    public static BillingClientLifecycle getInstance(LiveTvApplication app) {
        if (INSTANCE == null) {
            synchronized (BillingClientLifecycle.class) {
                if (INSTANCE == null) {
                    INSTANCE = new BillingClientLifecycle(app);
                }
            }
        }
        return INSTANCE;
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    public void create() {
        Log.d(TAG, "ON_CREATE");
        // Create a new BillingClient in onCreate().
        // Since the BillingClient can only be used once, we need to create a new instance
        // after ending the previous connection to the Google Play Store in onDestroy().
        billingClient = BillingClient.newBuilder(app)
                .setListener(this)
                .enablePendingPurchases() // Not used for subscriptions.
                .build();
        if (!billingClient.isReady()) {
            Log.d(TAG, "BillingClient: Start connection...");
            billingClient.startConnection(this);
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    public void destroy() {
       /* Log.d(TAG, "ON_DESTROY");
        if (billingClient.isReady()) {
            Log.d(TAG, "BillingClient can only be used once -- closing connection");
            // BillingClient can only be used once.
            // After calling endConnection(), we must create a new BillingClient.
            billingClient.endConnection();
        }*/
    }

    @Override
    public void onBillingSetupFinished(BillingResult billingResult) {
        int responseCode = billingResult.getResponseCode();
        String debugMessage = billingResult.getDebugMessage();
        Log.d(TAG, "onBillingSetupFinished: " + responseCode + " " + debugMessage);
        if (responseCode == BillingClient.BillingResponseCode.OK) {
            // The billing client is ready. You can query purchases here.
            querySkuDetails();
            queryPurchases();
        } else {
            Dialogs.showOneButtonDialog(LiveTvApplication.appContext, debugMessage + ". You can't use Google Play In-app Billing.");
        }
    }

    @Override
    public void onBillingServiceDisconnected() {
        Log.d(TAG, "onBillingServiceDisconnected");
        // TODO: Try connecting again with exponential backoff.
    }

    /**
     * Receives the result from {@link #querySkuDetails()}}.
     * <p>
     * Store the SkuDetails and post them in the {@link #skusWithSkuDetails}. This allows other
     * parts of the app to use the {@link SkuDetails} to show SKU information and make purchases.
     */
    @Override
    public void onSkuDetailsResponse(BillingResult billingResult, List<SkuDetails> skuDetailsList) {
        if (billingResult == null) {
            Log.wtf(TAG, "onSkuDetailsResponse: null BillingResult");
            return;
        }

        int responseCode = billingResult.getResponseCode();
        String debugMessage = billingResult.getDebugMessage();
        switch (responseCode) {
            case BillingClient.BillingResponseCode.OK:
                Log.i(TAG, "onSkuDetailsResponse: " + responseCode + " " + debugMessage);
                if (skuDetailsList == null) {
                    Log.w(TAG, "onSkuDetailsResponse: null SkuDetails list");
                    skusWithSkuDetails.postValue(Collections.<String, SkuDetails>emptyMap());
                } else {
                    Map<String, SkuDetails> newSkusDetailList = new HashMap<String, SkuDetails>();
                    for (SkuDetails skuDetails : skuDetailsList) {
                        newSkusDetailList.put(skuDetails.getSku(), skuDetails);
                    }
                    skusWithSkuDetails.postValue(newSkusDetailList);
                    Log.i(TAG, "onSkuDetailsResponse: count " + newSkusDetailList.size());
                }
                break;
            case BillingClient.BillingResponseCode.SERVICE_DISCONNECTED:
            case BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE:
            case BillingClient.BillingResponseCode.BILLING_UNAVAILABLE:
            case BillingClient.BillingResponseCode.ITEM_UNAVAILABLE:
            case BillingClient.BillingResponseCode.DEVELOPER_ERROR:
            case BillingClient.BillingResponseCode.ERROR:
                Log.e(TAG, "onSkuDetailsResponse: " + responseCode + " " + debugMessage);
                break;
            case BillingClient.BillingResponseCode.USER_CANCELED:
                Log.i(TAG, "onSkuDetailsResponse: " + responseCode + " " + debugMessage);
                break;
            // These response codes are not expected.
            case BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED:
            case BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED:
            case BillingClient.BillingResponseCode.ITEM_NOT_OWNED:
            default:
                Log.wtf(TAG, "onSkuDetailsResponse: " + responseCode + " " + debugMessage);
        }
    }

    /**
     * Query Google Play Billing for existing purchases.
     * <p>
     * New purchases will be provided to the PurchasesUpdatedListener.
     * You still need to check the Google Play Billing API to know when purchase tokens are removed.
     */
    public void queryPurchases() {
        if (!billingClient.isReady()) {
            Log.e(TAG, "queryPurchases: BillingClient is not ready");
        }

        Log.d(TAG, "queryPurchases: SUBS");
        Purchase.PurchasesResult result = billingClient.queryPurchases(BillingClient.SkuType.SUBS);
        if (result == null) {
            Log.i(TAG, "queryPurchases: null purchase result");
            processPurchases(null);
        } else {
            if (result.getPurchasesList() == null) {
                Log.i(TAG, "queryPurchases: null purchase list");
                processPurchases(null);
            } else {
                processPurchases(result.getPurchasesList());
            }
        }
    }

    private void registerPurchases(List<Purchase> purchases) {

        for(Purchase purchase: purchases) {
            if(purchases.size() > 0) {
                purchase = purchases.get(0);
                int sku = purchase.getSku().equals(Constants.PREMIUM) ? 1: 2;
                User user = LiveTvApplication.getUser();
                String purchaseToken = purchase.getPurchaseToken();
                if(purchase.isAutoRenewing() && !purchase.getPurchaseToken().equals(LiveTvApplication.getUser().getSubscription().getPurchaseToken())) {
                    String update_subscriptionRl =
                            WebConfig.update_subscriptionURl.replace("{EMAIL}", LiveTvApplication.getUser().getEmail())
                                    .replace("{SKU}", String.valueOf(sku))
                                    .replace("{TOKEN}", purchaseToken);

                    NetManager.getInstance().makeStringRequest(update_subscriptionRl, new StringRequestListener() {

                        @Override
                        public void onCompleted(String response) {

                        }

                        @Override
                        public void onError() {

                        }
                    });
                    user.getSubscription().setMembership(sku);
                    user.getSubscription().setPurchaseToken(purchaseToken);
                    user.getSubscription().setIsExpired(0);
                    user.getSubscription().setIsActive(1);
                    user.getSubscription().setIsOnHold(0);
                    user.getSubscription().setIsCancelled(0);
                    DataManager.getInstance().saveData("theUser", new Gson().toJson(user));
                    LiveTvApplication.user = user;
                } else if(!purchase.isAutoRenewing()) {
                    user.getSubscription().setIsActive(0);
                    DataManager.getInstance().saveData("theUser", new Gson().toJson(user));
                    LiveTvApplication.user = user;
                }
            }
        }


    }
    /**
     * Called by the Billing Library when new purchases are detected.
     */
    public void onPurchasesUpdated(BillingResult billingResult, List<Purchase> purchases) {
        if (billingResult == null) {
            Log.wtf(TAG, "onPurchasesUpdated: null BillingResult");
            return;
        }
        int responseCode = billingResult.getResponseCode();
        String debugMessage = billingResult.getDebugMessage();
        Log.d(TAG, "onPurchasesUpdated: $responseCode $debugMessage");
        switch (responseCode) {
            case BillingClient.BillingResponseCode.OK:
                if (purchases == null) {
                    Log.d(TAG, "onPurchasesUpdated: null purchase list");
                    processPurchases(null);
                } else {
                    processPurchases(purchases);
                }
                break;
            case BillingClient.BillingResponseCode.USER_CANCELED:
                Log.i(TAG, "onPurchasesUpdated: User canceled the purchase");
                break;
            case BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED:
                Log.i(TAG, "onPurchasesUpdated: The user already owns this item");
                break;
            case BillingClient.BillingResponseCode.DEVELOPER_ERROR:
                Log.e(TAG, "onPurchasesUpdated: Developer error means that Google Play " +
                        "does not recognize the configuration. If you are just getting started, " +
                        "make sure you have configured the application correctly in the " +
                        "Google Play Console. The SKU product ID must match and the APK you " +
                        "are using must be signed with release keys."
                );
                break;
        }
    }

    /**
     * Send purchase SingleLiveEvent and update purchases LiveData.
     * <p>
     * The SingleLiveEvent will trigger network call to verify the subscriptions on the sever.
     * The LiveData will allow Google Play settings UI to update based on the latest purchase data.
     */
    private void processPurchases(List<Purchase> purchasesList) {
        if (purchasesList != null) {
            Log.d(TAG, "processPurchases: " + purchasesList.size() + " purchase(s)");
        } else {
            Log.d(TAG, "processPurchases: with no purchases");
        }
        if (isUnchangedPurchaseList(purchasesList)) {
            Log.d(TAG, "processPurchases: Purchase list has not changed");
            return;
        }
        purchaseUpdateEvent.postValue(purchasesList);
        purchases.postValue(purchasesList);
        if (purchasesList != null) {
            logAcknowledgementStatus(purchasesList);
            registerPurchases(purchasesList);
        }
    }

    /**
     * Log the number of purchases that are acknowledge and not acknowledged.
     * <p>
     * https://developer.android.com/google/play/billing/billing_library_releases_notes#2_0_acknowledge
     * <p>
     * When the purchase is first received, it will not be acknowledge.
     * This application sends the purchase token to the server for registration. After the
     * purchase token is registered to an account, the Android app acknowledges the purchase token.
     * The next time the purchase list is updated, it will contain acknowledged purchases.
     */
    private void logAcknowledgementStatus(List<Purchase> purchasesList) {
        int ack_yes = 0;
        int ack_no = 0;
        for (Purchase purchase : purchasesList) {
            if (purchase.isAcknowledged()) {
                ack_yes++;
            } else {
                acknowledgePurchase(purchase.getPurchaseToken());
                ack_no++;
            }
        }
        Log.d(TAG, "logAcknowledgementStatus: acknowledged=" + ack_yes +
                " unacknowledged=" + ack_no);
    }

    /**
     * Check whether the purchases have changed before posting changes.
     */
    private boolean isUnchangedPurchaseList(List<Purchase> purchasesList) {
        // TODO: Optimize to avoid updates with identical data.
        return false;
    }

    /**
     * In order to make purchases, you need the {@link SkuDetails} for the item or subscription.
     * This is an asynchronous call that will receive a result in {@link #onSkuDetailsResponse}.
     */
    public void querySkuDetails() {
        Log.d(TAG, "querySkuDetails");

        List<String> skus = new ArrayList<>();
        skus.add("premium_plus_memberhship");
        skus.add("premimum_membership");
        SkuDetailsParams params = SkuDetailsParams.newBuilder()
                .setType(BillingClient.SkuType.SUBS)
                .setSkusList(skus)
                .build();

        Log.i(TAG, "querySkuDetailsAsync");
        billingClient.querySkuDetailsAsync(params, this);
    }


    /**
     * Launching the billing flow.
     * <p>
     * Launching the UI to make a purchase requires a reference to the Activity.
     */
    public int launchBillingFlow(Activity activity, BillingFlowParams params) {
        String sku = params.getSku();
        String oldSku = params.getOldSku();
        Log.i(TAG, "launchBillingFlow: sku: " + sku + ", oldSku: " + oldSku);
        if (!billingClient.isReady()) {
            Log.e(TAG, "launchBillingFlow: BillingClient is not ready");
        }
        BillingResult billingResult = billingClient.launchBillingFlow(activity, params);
        int responseCode = billingResult.getResponseCode();
        String debugMessage = billingResult.getDebugMessage();
        Log.d(TAG, "launchBillingFlow: BillingResponse " + responseCode + " " + debugMessage);
        return responseCode;
    }

    /**
     * Acknowledge a purchase.
     * <p>
     * https://developer.android.com/google/play/billing/billing_library_releases_notes#2_0_acknowledge
     * <p>
     * Apps should acknowledge the purchase after confirming that the purchase token
     * has been associated with a user. This app only acknowledges purchases after
     * successfully receiving the subscription data back from the server.
     * <p>
     * Developers can choose to acknowledge purchases from a server using the
     * Google Play Developer API. The server has direct access to the user database,
     * so using the Google Play Developer API for acknowledgement might be more reliable.
     * TODO(134506821): Acknowledge purchases on the server.
     * <p>
     * If the purchase token is not acknowledged within 3 days,
     * then Google Play will automatically refund and revoke the purchase.
     * This behavior helps ensure that users are not charged for subscriptions unless the
     * user has successfully received access to the content.
     * This eliminates a category of issues where users complain to developers
     * that they paid for something that the app is not giving to them.
     */
    public void acknowledgePurchase(String purchaseToken) {
        Log.d(TAG, "acknowledgePurchase");
        AcknowledgePurchaseParams params = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchaseToken)
                .build();
        billingClient.acknowledgePurchase(params, new AcknowledgePurchaseResponseListener() {
            @Override
            public void onAcknowledgePurchaseResponse(BillingResult billingResult) {
                int responseCode = billingResult.getResponseCode();
                String debugMessage = billingResult.getDebugMessage();
                Log.d(TAG, "acknowledgePurchase: " + responseCode + " " + debugMessage);
            }
        });
    }
}
