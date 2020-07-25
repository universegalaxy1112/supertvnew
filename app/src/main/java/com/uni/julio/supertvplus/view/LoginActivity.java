package com.uni.julio.supertvplus.view;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.uni.julio.supertvplus.LiveTvApplication;
import com.uni.julio.supertvplus.R;
import com.uni.julio.supertvplus.listeners.MessageCallbackListener;
import com.uni.julio.supertvplus.listeners.StringRequestListener;
import com.uni.julio.supertvplus.model.User;
import com.uni.julio.supertvplus.utils.Connectivity;
import com.uni.julio.supertvplus.utils.DataManager;
import com.uni.julio.supertvplus.utils.Device;
import com.uni.julio.supertvplus.utils.Dialogs;
import com.uni.julio.supertvplus.utils.Tracking;
import com.uni.julio.supertvplus.utils.library.CustomProgressDialog;
import com.uni.julio.supertvplus.utils.networing.NetManager;

import org.json.JSONException;
import org.json.JSONObject;

public class LoginActivity extends AppCompatActivity implements StringRequestListener, MessageCallbackListener {
private EditText mEmailView;
private EditText mPassView;
private String password = "";
private CustomProgressDialog customProgressDialog;
private AdView mAdView;
private GoogleSignInClient googleSignInClient;
private int RC_GOOGLE = 1001;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestProfile()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);
        setupUI();
    }

    @Override
    protected void onResume() {
        super.onResume();
        LiveTvApplication.appContext = this;
        if (mAdView != null) {
            mAdView.resume();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mAdView != null) {
            mAdView.destroy();
        }
    }

    public void onCompleted(String response) {
        hideProgressDialog();
        if (!TextUtils.isEmpty(response)) {
                try {
                    JSONObject jsonObject = new JSONObject(response);
                    if ("1".equals(jsonObject.getString("status"))) {
                        String userAgent = jsonObject.getString("user-agent");
                        if (!TextUtils.isEmpty(userAgent)) {
                            if(LiveTvApplication.saveUser(password, jsonObject)) {
                                startMain();
                                return;
                            }
                         }
                    } else {
                        DataManager.getInstance().saveData("theUser", "");
                        String errorFound = jsonObject.getString("error_found");
                        switch (errorFound) {
                            case "103":
                            case "104":
                                Dialogs.showOneButtonDialog(this, getString(R.string.attention), getString(R.string.login_error_change_device).replace("{ID}", Device.getIdentifier()), new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        finish();
                                        android.os.Process.killProcess(android.os.Process.myPid());
                                    }
                                });
//
                                break;
                            case "105":
                                mPassView.setText("");
                                mPassView.requestFocus();
                                showErrorMessage(getString(R.string.login_error_usr_pss_incorrect));
                                break;
                            case "107":
                                showErrorMessage(getString(R.string.login_error_expired));
                                break;
                            case "108": {
                                Dialogs.showOneButtonDialog(this, getString(R.string.attention), getString(R.string.login_error_change_account).replace("{ID}", Device.getIdentifier()), new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        finish();
                                        android.os.Process.killProcess(android.os.Process.myPid());
                                    }
                                });

                            }
                            break;
                            case "109": {
                                Dialogs.showOneButtonDialog(this, R.string.login_error_demo, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        finish();
                                        android.os.Process.killProcess(android.os.Process.myPid());
                                    }
                                });
                            }
                            break;
                            default:
                                showErrorMessage(getString(R.string.login_error_generic).replace("CODE", errorFound));
                                break;
                        }
                        return;
                    }
                } catch (JSONException e) {
//                e.printStackTrace();
                }
            }
        DataManager.getInstance().saveData("theUser", "");
    }

    private void startMain(){
        Intent launchIntent = new Intent(this, MainActivity.class);
        startActivity(launchIntent);
        overridePendingTransition(R.anim.right_in, R.anim.left_out);
        finish();
    }
    @Override
    public void onError() {
        showErrorMessage();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_GOOGLE) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                String email = "";
                password = "";
                if(account != null) {
                    email = account.getEmail();
                    password = account.getId();
                }

                if(TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
                    Dialogs.showOneButtonDialog(this, R.string.cast, R.string.cast, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    });
                } else{
                    signInWidthGoogle(email, password);
                }


            } catch (ApiException e) {
                Log.w("TAG", "Google sign in failed", e);
                Dialogs.showOneButtonDialog(this, R.string.generic_error_message, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
                // ...
            }
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mAdView != null) {
            mAdView.pause();
        }
    }

    private void setupUI() {

        setContentView(R.layout.activity_login);
        User user = LiveTvApplication.getUser();
        if(user == null || user.getSubscription().showAds()) {
            mAdView = findViewById(R.id.adView);
            if(mAdView != null) {
                AdRequest adRequest = new AdRequest.Builder().build();
                mAdView.loadAd(adRequest);
            }
        }
/*
        mEmailView =  findViewById(R.id.edit_email);
        mPassView =  findViewById(R.id.edit_password);
        mPassView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });*/

       /* TextView mEmailSignInButton =  findViewById(R.id.cv_loginScreen_login);*/
        LinearLayout mGoogleSignInButton = findViewById(R.id.ll_GoogleSignin);
        TextView mSignUpButton = findViewById(R.id.ll_SignUp);
        mSignUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoginActivity.this, SignUp.class));
                overridePendingTransition(R.anim.right_in, R.anim.left_out);
            }
        });
        mGoogleSignInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent signIntent = googleSignInClient.getSignInIntent();
                startActivityForResult(signIntent, RC_GOOGLE);
                googleSignInClient.signOut();
            }
        });

       /* mEmailSignInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

        mEmailSignInButton.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus)
                    v.setSelected(true);
                else
                    v.setSelected(false);
            }
        });*/
    }

    public boolean checkEditText(EditText edit) {
        if (edit == null) return false;
        edit.setError(null);
        String string = edit.getText().toString();
        if (TextUtils.isEmpty(string)) {
            edit.requestFocus();
            return false;
        }
        return true;
    }

    public boolean checkEmail(EditText edit) {
        if (!checkEditText(edit)) return false;
        String string = edit.getText().toString();
        boolean res = !TextUtils.isEmpty(string) && android.util.Patterns.EMAIL_ADDRESS.matcher(string).matches();
        if (!res) {
            edit.requestFocus();
            return false;
        }
        return true;
    }

    private void attemptLogin(){
        mEmailView.setError(null);
        mPassView.setError(null);
        String email=mEmailView.getText().toString();
        password = mPassView.getText().toString();

        if(!checkEmail(mEmailView)) {
            mEmailView.setError(getString(R.string.error_invalid_email));
            mEmailView.requestFocus();
            return;
        }
        if(!isPasswordValid(password)){
            mPassView.setError(getString(R.string.error_invalid_password));
            mPassView.requestFocus();
            return;
        }
         showProgress(true);
         NetManager.getInstance().performLogin(email, password, this);
    }

    private void signInWidthGoogle(String email, String password) {
        showProgress(true);
        NetManager.getInstance().performLogin(email, password, this);
    }

    private boolean isPasswordValid(String password) {
        //TODO: Replace this with your own logic
        return password.length() > 8;
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
    public void hideProgressDialog(){
        if(customProgressDialog != null) customProgressDialog.dismiss();
     }

    @Override
    public void onDismiss() {
        startMain();
    }

    @Override
    public void onAccept() {

    }
}
