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
import com.google.gson.Gson;
import com.uni.julio.supertvplus.LiveTvApplication;
import com.uni.julio.supertvplus.R;
import com.uni.julio.supertvplus.listeners.DialogListener;
import com.uni.julio.supertvplus.listeners.MessageCallbackListener;
import com.uni.julio.supertvplus.listeners.StringRequestListener;
import com.uni.julio.supertvplus.model.Subscription;
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

public class SignUp extends AppCompatActivity implements StringRequestListener, MessageCallbackListener {
    private EditText mUserNameView;
    private EditText mEmailView;
    private EditText mPassView;
    private EditText mPassConfirmView;
    private String pass = "";
    private CustomProgressDialog customProgressDialog;
    private GoogleSignInClient googleSignInClient;
    private int RC_GOOGLE = 1001;
    private int SIGN_UP_NORMAL = 0;
    private int SIGN_UP_GOOGLE = 2;
    private AdView mAdView;
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
    public void onCompleted(String response) {
        hideProgressDialog();
        if (!TextUtils.isEmpty(response)) {
            try {
                JSONObject jsonObject = new JSONObject(response);
                if ("1".equals(jsonObject.getString("status"))) {
                    String userAgent = jsonObject.getString("user-agent");
                    if (!TextUtils.isEmpty(userAgent)) {
                        if(LiveTvApplication.saveUser(pass, jsonObject)) {
                            startMain();
                            return;
                        }

                    }
                } else {
                    DataManager.getInstance().saveData("theUser", "");
                    String errorFound = jsonObject.getString("error_found");
                    switch (errorFound) {
                        case "105":
                            mPassView.setText("");
                            mPassView.requestFocus();
                            showErrorMessage(getString(R.string.sign_incorrect_character));
                            break;
                        case "202": {
                            Dialogs.showOneButtonDialog(this, getString(R.string.email_duplication));
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
                String userName = "";
                String email = "";
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if(account != null) {
                    userName = account.getDisplayName();
                    email = account.getEmail();
                    pass = account.getId();
                }

                if(TextUtils.isEmpty(email) || TextUtils.isEmpty(pass)) {
                    Dialogs.showOneButtonDialog(this, R.string.cast, R.string.cast, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    });
                } else{
                    next(email, userName, SIGN_UP_GOOGLE);
                }


            } catch (ApiException e) {
                Dialogs.showOneButtonDialog(this, R.string.generic_error_message, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
                Log.w("TAG", "Google sign in failed", e);
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

        setContentView(R.layout.activity_signup);

        User user = LiveTvApplication.getUser();
        if(user == null || user.getSubscription().showAds()) {
            mAdView = findViewById(R.id.adView);
            if(mAdView != null) {
                AdRequest adRequest = new AdRequest.Builder().build();
                mAdView.loadAd(adRequest);
            }
        }
       /* mUserNameView = findViewById(R.id.edit_username);
        mEmailView =  findViewById(R.id.edit_email);
        mPassView =  findViewById(R.id.edit_password);
        mPassConfirmView =  findViewById(R.id.edit_confirm_password);

        TextView mNextButton =  findViewById(R.id.next);*/
        LinearLayout mGoogleSignUpButton = findViewById(R.id.ll_GoogleSignUp);
        TextView mSignInButton = findViewById(R.id.ll_SignIn);

        mGoogleSignUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent signIntent = googleSignInClient.getSignInIntent();
                startActivityForResult(signIntent, RC_GOOGLE);
                googleSignInClient.signOut();
            }
        });

        mSignInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(SignUp.this, LoginActivity.class));
                overridePendingTransition(R.anim.right_out, R.anim.left_in);
                finish();
            }
        });

        /*mNextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SignUpNormal();
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


    private void SignUpNormal(){
        mEmailView.setError(null);
        mUserNameView.setError(null);
        mPassView.setError(null);
        mPassConfirmView.setError(null);

        if(!checkEditText(mUserNameView)){
            mUserNameView.setError(getString(R.string.error_field_required));
            mUserNameView.requestFocus();
            return;
        }

        if(!checkEmail(mEmailView)) {
            mEmailView.setError(getString(R.string.error_invalid_email));
            mEmailView.requestFocus();
            return;
        }

        if(!isPasswordValid(mPassView.getText().toString())){
            mPassView.setError(getString(R.string.error_invalid_password));
            mPassView.requestFocus();
            return;
        }

        pass = mPassView.getText().toString();
        String passwordConfirm = mPassConfirmView.getText().toString();
        if(!pass.equals(passwordConfirm)) {
            mPassConfirmView.setError(getString(R.string.pass_no_match));
            mPassConfirmView.requestFocus();
            return;
        }
        String userName = mUserNameView.getText().toString();
        String email = mEmailView.getText().toString();
        next(email, userName, SIGN_UP_NORMAL);
    }

    private void next(String email, String userName, int type) {
        if(type == SIGN_UP_NORMAL) {
            Intent intent = new Intent(this, EmailVerifyActivity.class);
            intent.putExtra("email", email);
            intent.putExtra("userName", userName);
            intent.putExtra("password", pass);
            startActivity(intent);
            finish();
        }else{
            showProgress(true);
            NetManager.getInstance().performSignUp(email, userName, pass, this);
        }

    }

    private boolean isPasswordValid(String password) {
        //TODO: Replace this with your own logic
        return password.length() > 3;
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
