package com.uni.julio.supertvplus.view;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.viewpager.widget.ViewPager;

import com.android.billingclient.api.BillingFlowParams;
import com.google.android.material.tabs.TabLayout;
import com.uni.julio.supertvplus.Constants;
import com.uni.julio.supertvplus.LiveTvApplication;
import com.uni.julio.supertvplus.R;
import com.uni.julio.supertvplus.adapter.MypagerAdpater;
import com.uni.julio.supertvplus.utils.BillingViewModel;
import com.uni.julio.supertvplus.utils.Connectivity;
import com.uni.julio.supertvplus.utils.Dialogs;
import com.uni.julio.supertvplus.utils.Tracking;
import com.uni.julio.supertvplus.utils.library.CustomProgressDialog;

public class SubscribeActivity extends AppCompatActivity {

    private CustomProgressDialog customProgressDialog;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subscribe);
        ViewPager viewPager = findViewById(R.id.pager);
        viewPager.setAdapter(new MypagerAdpater(getSupportFragmentManager()));
        TabLayout tabLayout = findViewById(R.id.detail_tab);
        tabLayout.setupWithViewPager(viewPager);

    }

    @Override
    protected void onResume() {
        super.onResume();
        ((LiveTvApplication)getApplication()).getBillingClientLifecycle().queryPurchases();
        LiveTvApplication.appContext = this;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Context appCompatActivity= LiveTvApplication.appContext;
        if(this.equals(appCompatActivity))
            LiveTvApplication.appContext = null;

    }

    private void showProgress(final boolean show) {
        if(show){
            showCustomProgressDialog();
        }
        else{
            hideProgressDialog();
        }
    }
    public void showErrorMessage() {
        if(Connectivity.isConnected()) {
            showErrorMessage(getString(R.string.login_error_generic).replace("[CODE]",""));
        }
        else {
            showProgress(false);
            Dialogs.showOneButtonDialog(this, R.string.no_connection_title,  R.string.no_connection_message);
        }
    }
    public void showErrorMessage(String message) {
        showProgress(false);
        Dialogs.showOneButtonDialog(this, message);
    }
    public void showCustomProgressDialog(){
        if(customProgressDialog == null) customProgressDialog = new CustomProgressDialog(this, getString(R.string.wait));
        customProgressDialog.show();
    }
    public void hideProgressDialog() {
        if (customProgressDialog != null) customProgressDialog.dismiss();
    }

}
