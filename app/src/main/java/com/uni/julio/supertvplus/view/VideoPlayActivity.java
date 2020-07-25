package com.uni.julio.supertvplus.view;

import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.uni.julio.supertvplus.LiveTvApplication;
import com.uni.julio.supertvplus.R;
import com.uni.julio.supertvplus.listeners.DialogListener;
import com.uni.julio.supertvplus.listeners.LiveTVToggleUIListener;
import com.uni.julio.supertvplus.model.User;
import com.uni.julio.supertvplus.utils.Dialogs;
import com.uni.julio.supertvplus.view.exoplayer.VideoPlayFragment;
import com.uni.julio.supertvplus.viewmodel.Lifecycle;

public class VideoPlayActivity extends BaseActivity implements LiveTVToggleUIListener {


    private VideoPlayFragment videoPlayFragment;
    @Override
    protected Lifecycle.ViewModel getViewModel() {
        return null;
    }

    @Override
    protected Lifecycle.View getLifecycleView() {
        return null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_play);
        Intent intent = getIntent();

        mainCategoryId = intent.getIntExtra("mainCategoryId",0);
        User user = LiveTvApplication.getUser();
        if(user != null) {
            if(!user.getSubscription().hasAccessPlus() && mainCategoryId != 0 &&  mainCategoryId != 1 ) {
                    Dialogs.showTwoButtonsDialog(this,R.string.accept ,  (R.string.cancel),  R.string.membership_permission,  new DialogListener() {
                        public void onAccept() {
                            startActivity(new Intent(VideoPlayActivity.this, SubscribeActivity.class));
                            finishActivity();
                        }

                        public void onCancel() {
                            finishActivity();
                        }

                        @Override
                        public void onDismiss() {
                            finishActivity();
                        }
                    });
                    return;
            }
        }else
            return;
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        videoPlayFragment = new VideoPlayFragment();
        if(mainCategoryId == 4 || intent.getIntExtra("type", 0) == 2 )
            videoPlayFragment.hidePlayBack();
        videoPlayFragment.setLiveTVToggleListener(this);
        getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.video_container,videoPlayFragment).commit();
    }
    @Override
    public void onStart(){
        super.onStart();

    }
    @Override
    public void onResume(){
        super.onResume();
        ((LiveTvApplication)getApplication()).getBillingClientLifecycle().queryPurchases();
    }
    @Override
    public void onDestroy(){
        super.onDestroy();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onPause(){
        super.onPause();
    }
    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        videoPlayFragment.onNewIntent(intent);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_BACK){
            finishActivity();
            return false;
        }
        if(keyCode==KeyEvent.KEYCODE_DPAD_LEFT){
            videoPlayFragment.dispatchKeyEvent();
            return false;
        }
        if(keyCode==KeyEvent.KEYCODE_DPAD_UP){
            sendBroadcast(new Intent("toggle"));

            videoPlayFragment.dispatchKeyEvent();
            return false;
        }
        if(keyCode==KeyEvent.KEYCODE_DPAD_RIGHT){
            videoPlayFragment.dispatchKeyEvent();
            return false;
        }
        if(keyCode==KeyEvent.KEYCODE_DPAD_CENTER){
            videoPlayFragment.toggleTitle();
            videoPlayFragment.dispatchKeyEvent();
            return false;
        }
        if(keyCode==KeyEvent.KEYCODE_MEDIA_FAST_FORWARD||keyCode==KeyEvent.KEYCODE_FORWARD||keyCode==272||keyCode==274||keyCode==KeyEvent.KEYCODE_BUTTON_R1||keyCode==KeyEvent.KEYCODE_BUTTON_R2||keyCode==KeyEvent.KEYCODE_RIGHT_BRACKET ){
            videoPlayFragment.doForwardVideo();
            return true;
        }
        if(keyCode==KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE){
            videoPlayFragment.playPause();
            return true;
        }
        if(keyCode==KeyEvent.KEYCODE_MEDIA_REWIND){
            videoPlayFragment.doRewindVideo();
            return true;
        }
        return false;
    }
    @Override
    public void onToggleUI(boolean show) {
        if(mainCategoryId == 4 || getIntent().getIntExtra("type", 0) == 2)
            videoPlayFragment.toggleTitle();
    }
}
