package com.uni.julio.supertvplus.view;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;

import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.SkuDetails;
import com.google.gson.Gson;
import com.uni.julio.supertvplus.Constants;
import com.uni.julio.supertvplus.LiveTvApplication;
import com.uni.julio.supertvplus.R;
import com.uni.julio.supertvplus.listeners.StringRequestListener;
import com.uni.julio.supertvplus.model.User;
import com.uni.julio.supertvplus.utils.BillingClientLifecycle;
import com.uni.julio.supertvplus.utils.BillingViewModel;
import com.uni.julio.supertvplus.utils.DataManager;
import com.uni.julio.supertvplus.utils.networing.NetManager;
import com.uni.julio.supertvplus.utils.networing.WebConfig;

import java.util.List;
import java.util.Map;
import java.util.Objects;


public class FreeFragment extends Fragment {

    private int planId = 0;
    private String currencyCode = "USD";
    private String planAmount = "0";
    private String service1 = "";
    private String service2 = "";
    private String service3 = "";
    private String planDetail = "";
    private TextView planPrice;
    private TextView text_currency;
    private TextView currentPlan;
    private TextView textView_service1;
    private TextView textView_service2;
    private TextView textView_service3;
    private TextView plan_detail;
    private TextView choose_plan;
    private MutableLiveData<Map<String, SkuDetails>> skusWithSkuDetails;
    private BillingViewModel billingViewModel;
    private static final String ARG_SECTION_NUMBER = "section_number";


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        planId = getArguments().getInt(ARG_SECTION_NUMBER);

        switch (planId) {
            case 1:
                planAmount = "5";
                service1 = "Movies, Series, Live";
                service2 = "Only 1 device";
                service3 = "No ads";
                break;
            case 2:
                planAmount = "14";
                service1 = "All Contents";
                service2 = "3 devices";
                service3 = "No ads";
                break;
            default:
                service1 = "Movies, Series.";
                service2 = "Only 1 device";
                service3 = "Contains ads";
                planAmount = "0";
                planDetail = "You will not charge in this plan.";
        }
        BillingClientLifecycle billingClientLifecycle = ((LiveTvApplication)getActivity().getApplication()).getBillingClientLifecycle();
        skusWithSkuDetails = billingClientLifecycle.skusWithSkuDetails;

        skusWithSkuDetails.observe(getViewLifecycleOwner(), new Observer<Map<String, SkuDetails>>() {
            @Override
            public void onChanged(Map<String, SkuDetails> stringSkuDetailsMap) {
                if(planId == 1) {
                    SkuDetails skuDetails = stringSkuDetailsMap.get("premimum_membership");
                    if(skuDetails != null) {
                        currencyCode = skuDetails.getPriceCurrencyCode();
                        planAmount = String.valueOf(skuDetails.getPriceAmountMicros()/1000000);
                        planDetail = skuDetails.getDescription();
                    }
                } else if(planId == 2) {
                    SkuDetails skuDetails = stringSkuDetailsMap.get("premium_plus_memberhship");
                    if(skuDetails != null) {
                        currencyCode = skuDetails.getPriceCurrencyCode();
                        planAmount = String.valueOf(skuDetails.getPriceAmountMicros()/1000000);
                        planDetail = skuDetails.getDescription();
                    }
                }
                setDetail();
            }
        });

        billingViewModel = new ViewModelProvider(this).get(BillingViewModel.class);

        billingViewModel.buyEvent.observe(getViewLifecycleOwner(), new Observer<BillingFlowParams>() {
            @Override
            public void onChanged(BillingFlowParams billingFlowParams) {
                if (billingFlowParams != null) {
                    ((LiveTvApplication) Objects.requireNonNull(getActivity()).getApplication()).getBillingClientLifecycle()
                            .launchBillingFlow(getActivity(), billingFlowParams);
                }
            }
        });

        billingViewModel.openPlayStoreSubscriptionsEvent.observe(getViewLifecycleOwner(), new Observer<String>() {
            @Override
            public void onChanged(String sku) {
                Log.i("TAG", "Viewing subscriptions on the Google Play Store");
                String url;
                if (sku == null) {
                    // If the SKU is not specified, just open the Google Play subscriptions URL.
                    url = Constants.PLAY_STORE_SUBSCRIPTION_URL;
                } else {
                    // If the SKU is specified, open the deeplink for this SKU on Google Play.
                    url = String.format(Constants.PLAY_STORE_SUBSCRIPTION_DEEPLINK_URL,
                            sku, getActivity().getApplicationContext().getPackageName());
                }
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(url));
                startActivity(intent);
            }
        });


        View rootPlayerView= inflater.inflate(R.layout.fragment_free, container, false);
        planPrice = rootPlayerView.findViewById(R.id.text_price_plan);
        text_currency = rootPlayerView.findViewById(R.id.currency_code);
        currentPlan = rootPlayerView.findViewById(R.id.text_current_plan);
        textView_service1 = rootPlayerView.findViewById(R.id.text_service1);
        textView_service2 = rootPlayerView.findViewById(R.id.text_service2);
        textView_service3 = rootPlayerView.findViewById(R.id.text_service3);
         choose_plan = rootPlayerView.findViewById(R.id.choose_plan);
        plan_detail = rootPlayerView.findViewById(R.id.plan_detail);
        choose_plan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(skusWithSkuDetails.getValue() != null) {
                    if(planId == 1) {
                        billingViewModel.buyPremium();
                    } else if(planId == 2) {
                        billingViewModel.buyPremiumPlus();
                    } else
                        billingViewModel.openPlayStoreSubscriptions();
                }
            }
        });
        setDetail();
        return rootPlayerView;
    }

    private void setDetail() {
        if(planPrice == null) return;
        planPrice.setText(planAmount);
        text_currency.setText(currencyCode);
        textView_service1.setText(service1);
        textView_service2.setText(service2);
        textView_service3.setText(service3);
        plan_detail.setText(planDetail);
        User user = LiveTvApplication.getUser();
        if(user.getSubscription().getMembership() == planId) {
            currentPlan.setVisibility(View.VISIBLE);
            choose_plan.setText(R.string.manage_subscription_on_play_store);
        }

        if(user.getSubscription().getMembership() == 0 && planId == 0)
            choose_plan.setVisibility(View.GONE);
        else if(planId == 0)
            choose_plan.setText(R.string.cancel_subscriptions_play_store);
    }

    public static FreeFragment newInstance(int sectionNumber) {
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        FreeFragment freeFragment = new FreeFragment();
        freeFragment.setArguments(args);
        return freeFragment;
    }
}
