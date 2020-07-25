package com.uni.julio.supertvplus.view;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.gson.Gson;
import com.uni.julio.supertvplus.LiveTvApplication;
import com.uni.julio.supertvplus.R;
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
import com.uni.julio.supertvplus.utils.networing.WebConfig;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URLEncoder;

public class EmailVerifyActivity extends AppCompatActivity implements StringRequestListener, MessageCallbackListener {

    private CustomProgressDialog customProgressDialog;
    private String pass = "";
    private String email = "";
    private String verifyCationCode = "";

    @Override
    protected void onResume() {
        super.onResume();
        LiveTvApplication.appContext = this;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_email_verify);
        Bundle extras = this.getIntent().getExtras();
        email = "";
        String userName = "";
        if(extras != null) {
           email = extras.getString("email");
           userName = extras.getString("userName");
           pass = extras.getString("password");
        }
        TextView signUpBtn = findViewById(R.id.cv_verifyScreen_signUp);
        final EditText editText = findViewById(R.id.etCode);
        final String finalEmail = email;
        final String finalUserName = userName;
        final String finalPass = pass;
        signUpBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!verifyCationCode.equals("") && verifyCationCode.equals(editText.getText().toString())) {
                    next(finalEmail, finalUserName, finalPass);
                } else {
                    Dialogs.showOneButtonDialog(EmailVerifyActivity.this, "Wrong Code!");
                }
            }
        });

        TextView btnResend = findViewById(R.id.btnResend);
        btnResend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendVerificationCode();
            }
        });

        sendVerificationCode();

        TextView btnSignin = findViewById(R.id.btnSignin);
        btnSignin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(EmailVerifyActivity.this, LoginActivity.class));
                overridePendingTransition(R.anim.right_out, R.anim.left_in);
                finish();
            }
        });

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
                e.printStackTrace();
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

    private void sendVerificationCode() {
        showCustomProgressDialog();
        String url = WebConfig.sendVerificationCode.replace("{EMAIL}", email);
        NetManager.getInstance().makeStringRequest(url, new StringRequestListener() {
            @Override
            public void onCompleted(String response) {
                hideProgressDialog();
                if (!TextUtils.isEmpty(response)) {
                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        int error_code = jsonObject.getInt("error_code");
                        if(error_code == 0) {
                            verifyCationCode = jsonObject.getString("verification_code");
                            Dialogs.showOneButtonDialog(EmailVerifyActivity.this, getString(R.string.we_sent_email_confirm_code));

                        } else {
                            Dialogs.showOneButtonDialog(EmailVerifyActivity.this, getString(R.string.retry));
                        }
                    } catch (Exception e) {
                        Dialogs.showOneButtonDialog(EmailVerifyActivity.this, getString(R.string.retry));

                        e.printStackTrace();
                    }
                }else{
                    Dialogs.showOneButtonDialog(EmailVerifyActivity.this, getString(R.string.retry));
                }
            }

            @Override
            public void onError() {

            }
        });
    }

    @Override
    public void onError() {
        showErrorMessage();
    }


    @Override
    protected void onPause() {
        super.onPause();
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

    private void next(String email, String userName, String password) {
        showProgress(true);
        NetManager.getInstance().performSignUp(email, userName, password, this);
    }


    private void showProgress(final boolean show) {
        if(show)
            showCustomProgressDialog();
        else
            hideProgressDialog();
    }

    public void showErrorMessage() {
        if(Connectivity.isConnected())
            showErrorMessage(getString(R.string.login_error_generic).replace("[CODE]",""));
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
