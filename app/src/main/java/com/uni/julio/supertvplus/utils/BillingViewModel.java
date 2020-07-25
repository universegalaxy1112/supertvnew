/*
 * Copyright 2020 Google LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.uni.julio.supertvplus.utils;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.SkuDetails;
import com.uni.julio.supertvplus.Constants;
import com.uni.julio.supertvplus.LiveTvApplication;
import com.uni.julio.supertvplus.helper.SingleLiveEvent;
import com.uni.julio.supertvplus.model.SubscriptionStatus;


import java.util.List;
import java.util.Map;

public class BillingViewModel extends AndroidViewModel {

    /**
     * Local billing purchase data.
     */
    private MutableLiveData<List<Purchase>> purchases;

    /**
     * SkuDetails for all known SKUs.
     */
    private MutableLiveData<Map<String, SkuDetails>> skusWithSkuDetails;

    /**
     * Subscriptions record according to the server.
     */

    /**
     * Send an event when the Activity needs to buy something.
     */
    public SingleLiveEvent<BillingFlowParams> buyEvent = new SingleLiveEvent<>();

    /**
     * Send an event when the UI should open the Google Play
     * Store for the user to manage their subscriptions.
     */
    public SingleLiveEvent<String> openPlayStoreSubscriptionsEvent = new SingleLiveEvent<>();

    public BillingViewModel(@NonNull Application application) {
        super(application);
        LiveTvApplication superApp = ( (LiveTvApplication)application);
        purchases = superApp.getBillingClientLifecycle().purchases;
        skusWithSkuDetails = superApp.getBillingClientLifecycle().skusWithSkuDetails;
        // subscriptions = subApp.getRepository().getSubscriptions();
    }


    /**
     * Open the Play Store subscription center. If the user has exactly one SKU,
     * then open the deeplink to the specific SKU.
     */
    public void openPlayStoreSubscriptions() {
        boolean hasBasic = BillingUtilities.deviceHasGooglePlaySubscription(
                purchases.getValue(), Constants.PREMIUM);
        boolean hasPremium = BillingUtilities.deviceHasGooglePlaySubscription
                (purchases.getValue(), Constants.PREMIUM_PLUS);
        Log.d("Billing", "hasBasic: $hasBasic, hasPremium: $hasPremium");

        if (hasBasic && !hasPremium) {
            // If we just have a basic subscription, open the basic SKU.
            openPlayStoreSubscriptionsEvent.postValue(Constants.PREMIUM);
        } else if (!hasBasic && hasPremium) {
            // If we just have a premium subscription, open the premium SKU.
            openPlayStoreSubscriptionsEvent.postValue(Constants.PREMIUM_PLUS);
        } else {
            // If we do not have an active subscription,
            // or if we have multiple subscriptions, open the default subscription center.
            openPlayStoreSubscriptionsEvent.call();
        }
    }

    /**
     * Open account hold subscription.
     * <p>
     * We need to use the server data to understand account hold.
     * Most of the other deeplinks are based on the purchase tokens returned on the local device.
     * Since the purchase tokens will not be returned when the subscription is in account hold,
     * we look at the server data to determine the deeplink.
     */
    public void openAccountHoldSubscription() {
        boolean isPremiumOnServer = BillingUtilities
                .serverHasSubscription(Constants.PREMIUM_PLUS);
        boolean isBasicOnServer = BillingUtilities
                .serverHasSubscription(Constants.PREMIUM);
        if (isPremiumOnServer) {
            openPremiumPlayStoreSubscriptions();
        }

        if (isBasicOnServer) {
            openBasicPlayStoreSubscriptions();
        }
    }

    /**
     * Open the Play Store basic subscription.
     */
    public void openBasicPlayStoreSubscriptions() {
        openPlayStoreSubscriptionsEvent.postValue(Constants.PREMIUM);
    }

    /**
     * Open the Play Store premium subscription.
     */
    public void openPremiumPlayStoreSubscriptions() {
        openPlayStoreSubscriptionsEvent.postValue(Constants.PREMIUM_PLUS);
    }

    /**
     * Buy a basic subscription.
     */
    public void buyPremium() {
        boolean hasPremium = BillingUtilities
                .deviceHasGooglePlaySubscription(purchases.getValue(), Constants.PREMIUM);
        boolean hasPremiumPlus = BillingUtilities
                .deviceHasGooglePlaySubscription(purchases.getValue(), Constants.PREMIUM_PLUS);
        Log.d("Billing", "hasBasic: " + hasPremium + ", hasPremium: " + hasPremiumPlus);
        if (hasPremium && hasPremiumPlus) {
            // If the user has both subscriptions, open the basic SKU on Google Play.
            openPlayStoreSubscriptionsEvent.postValue(Constants.PREMIUM);
        } else if (hasPremium && !hasPremiumPlus) {
            // If the user just has a basic subscription, open the basic SKU on Google Play.
            openPlayStoreSubscriptionsEvent.postValue(Constants.PREMIUM);
        } else if (!hasPremium && hasPremiumPlus) {
            // If the user just has a premium subscription, downgrade.
            buy(Constants.PREMIUM, Constants.PREMIUM_PLUS);
        } else {
            // If the user dooes not have a subscription, buy the basic SKU.
            buy(Constants.PREMIUM, null);
        }
    }

    /**
     * Buy a premium subscription.
     */
    public void buyPremiumPlus() {
        boolean hasBasic = BillingUtilities
                .deviceHasGooglePlaySubscription(purchases.getValue(), Constants.PREMIUM);
        boolean hasPremium = BillingUtilities
                .deviceHasGooglePlaySubscription(purchases.getValue(), Constants.PREMIUM_PLUS);
        Log.d("Billing", "hasBasic: " + hasBasic + ", hasPremium: " + hasPremium);
        if (hasBasic && hasPremium) {
            // If the user has both subscriptions, open the premium SKU on Google Play.
            openPlayStoreSubscriptionsEvent.postValue(Constants.PREMIUM_PLUS);
        } else if (!hasBasic && hasPremium) {
            // If the user just has a premium subscription, open the premium SKU on Google Play.
            openPlayStoreSubscriptionsEvent.postValue(Constants.PREMIUM_PLUS);
        } else if (hasBasic && !hasPremium) {
            // If the user just has a basic subscription, upgrade.
            buy(Constants.PREMIUM_PLUS, Constants.PREMIUM);
        } else {
            // If the user does not have a subscription, buy the premium SKU.
            buy(Constants.PREMIUM_PLUS, null);
        }
    }

    /**
     * Upgrade to a premium subscription.
     */
    public void buyUpgrade() {
        buy(Constants.PREMIUM_PLUS, Constants.PREMIUM);
    }

    /**
     * Use the Google Play Billing Library to make a purchase.
     */
    private void buy(String sku, @Nullable String oldSku) {

        // First, determine whether the new SKU can be purchased.
        /*boolean isSkuOnServer = BillingUtilities
                .serverHasSubscription(LiveTvApplication.getUser().getSubscription().getPurchaseToken(), sku);
        boolean isSkuOnDevice = BillingUtilities
                .deviceHasGooglePlaySubscription(purchases.getValue(), sku);
        Log.d("Billing", sku + " - isSkuOnServer: " + isSkuOnServer +
                ", isSkuOnDevice: " + isSkuOnDevice);
        if (isSkuOnDevice && isSkuOnServer) {
            Log.e("Billing", "You cannot buy a SKU that is already owned: " + sku +
                    "This is an error in the application trying to use Google Play Billing.");
        } else if (isSkuOnDevice && !isSkuOnServer) {
            Log.e("Billing", "The Google Play Billing Library APIs indicate that" +
                    "this SKU is already owned, but the purchase token is not registered " +
                    "with the server. There might be an issue registering the purchase token.");
        } else if (!isSkuOnDevice && isSkuOnServer) {
            Log.w("Billing", "WHOA! The server says that the user already owns " +
                    "this item: $sku. This could be from another Google account. " +
                    "You should warn the user that they are trying to buy something " +
                    "from Google Play that they might already have access to from " +
                    "another purchase, possibly from a different Google account " +
                    "on another device.\n" +
                    "You can choose to block this purchase.\n" +
                    "If you are able to cancel the existing subscription on the server, " +
                    "you should allow the user to subscribe with Google Play, and then " +
                    "cancel the subscription after this new subscription is complete. " +
                    "This will allow the user to seamlessly transition their payment " +
                    "method from an existing payment method to this Google Play account.");
        } else {
            // Second, determine whether the old SKU can be replaced.
            // If the old SKU cannot be used, set this value to null and ignore it.*/

            String oldSkuToBeReplaced = null;
            if (isOldSkuReplaceable(purchases.getValue(), oldSku)) {
                oldSkuToBeReplaced = oldSku;
            }

            // Third, create the billing parameters for the purchase.
            if (sku.equals(oldSkuToBeReplaced)) {
                Log.i("Billing", "Re-subscribe.");
            } else if (Constants.PREMIUM_PLUS.equals(sku)
                    && Constants.PREMIUM.equals(oldSkuToBeReplaced)) {
                Log.i("Billing", "Upgrade!");
            } else if (Constants.PREMIUM.equals(sku)
                    && Constants.PREMIUM_PLUS.equals(oldSkuToBeReplaced)) {
                Log.i("Billing", "Downgrade...");
            } else {
                Log.i("Billing", "Regular purchase.");
            }

            SkuDetails skuDetails = null;
            // Create the parameters for the purchase.
            if (skusWithSkuDetails.getValue() != null) {
                skuDetails = skusWithSkuDetails.getValue().get(sku);
            }

            if (skuDetails == null) {
                Log.e("Billing", "Could not find SkuDetails to make purchase.");
                return;
            }

            BillingFlowParams.Builder billingBuilder =
                    BillingFlowParams.newBuilder().setSkuDetails(skuDetails);
            // Only set the old SKU parameter if the old SKU is already owned.
            if (oldSkuToBeReplaced != null && !oldSkuToBeReplaced.equals(sku)) {
                Purchase oldPurchase = BillingUtilities
                        .getPurchaseForSku(purchases.getValue(), oldSku);
                billingBuilder.setOldSku(oldSkuToBeReplaced, oldPurchase.getPurchaseToken());
            }

            BillingFlowParams billingParams = billingBuilder.build();

            // Send the parameters to the Activity in order to launch the billing flow.
            buyEvent.postValue(billingParams);
        //}
    }

    /**
     * Determine if the old SKU can be replaced.
     */
    private boolean isOldSkuReplaceable(
            List<Purchase> purchases,
            String oldSku) {
        if (oldSku == null) return false;
        boolean isOldSkuOnServer = BillingUtilities.serverHasSubscription(oldSku);
        boolean isOldSkuOnDevice = BillingUtilities.deviceHasGooglePlaySubscription(purchases, oldSku);

        if (!isOldSkuOnDevice) {
            Log.e("Billing", "You cannot replace a SKU that is NOT already owned: " + oldSku
                    + "This is an error in the application trying to use Google Play Billing.");
            return false;
        } else if (!isOldSkuOnServer) {
            Log.i("Billing", "Refusing to replace the old SKU because it is " +
                    "not registered with the server. Instead just buy the new SKU as an " +
                    "original purchase. The old SKU might already " +
                    "be owned by a different app account, and we should not transfer the " +
                    "subscription without user permission.");
            return false;
        } else {
           return true;
        }
    }
}
