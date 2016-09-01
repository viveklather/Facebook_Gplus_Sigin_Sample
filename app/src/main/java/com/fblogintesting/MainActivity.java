package com.fblogintesting;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;

import org.json.JSONException;
import org.json.JSONObject;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;


public class MainActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener {
    private CallbackManager callbackManager;
    //Signin button
    private SignInButton signInButton;
    //Signing Options
    private GoogleSignInOptions gso;
    //google api client
    private GoogleApiClient mGoogleApiClient;
    //Signin constant to check the activity result
    private int RC_SIGN_IN = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //initializing facebook SDK
        FacebookSdk.sdkInitialize(this);
        setContentView(R.layout.activity_main);
        //getting hash key for facebook app
        gethashkey();
        //creating call back manager factory
        callbackManager = CallbackManager.Factory.create();
        //initializing facebook login button
        final LoginButton loginButton = (LoginButton) findViewById(R.id.login_button);
        //setting permission to read user info
        loginButton.setReadPermissions(Arrays.asList(
                "public_profile", "email", "user_birthday", "user_friends"));
        //registering callback to login button
        loginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                Log.e("onSuccess", "login success");
                //getting userInfo
                GraphRequest request = GraphRequest.newMeRequest(
                        loginResult.getAccessToken(),
                        new GraphRequest.GraphJSONObjectCallback() {
                            @Override
                            public void onCompleted(JSONObject object, GraphResponse response) {
                                Log.e("onCompleted", "response-" + response.toString());
                                // Application code
//                                try {
//                                    String email = object.getString("email");
//                                    String birthday = object.getString("birthday"); // 01/31/1980 format
//                                } catch (JSONException e) {
//                                    e.printStackTrace();
//                                }
                            }
                        });
                //passing bundle to async task to get details
                Bundle parameters = new Bundle();
                parameters.putString("fields", "id,name,email,gender,birthday");
                request.setParameters(parameters);
                request.executeAsync();

                //google plus sign in implementation
                //Initializing signinbutton
                signInButton = (SignInButton) findViewById(R.id.sign_in_button);
                signInButton.setSize(SignInButton.SIZE_WIDE);
                signInButton.setScopes(gso.getScopeArray());
                //Initializing google signin option
                gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestEmail()
                        .build();
                //Initializing google api client
                mGoogleApiClient = new GoogleApiClient.Builder(MainActivity.this)
                        .enableAutoManage(MainActivity.this, MainActivity.this)
                        .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                        .build();
                signInButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Log.e("signInButton", "Button clicked");
                        //Creating an intent
                        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
                        //Starting intent for result
                        startActivityForResult(signInIntent, RC_SIGN_IN);
                    }
                });

            }

            @Override
            public void onCancel() {
                Log.e("onCancel", "login onCancel");
            }

            @Override
            public void onError(FacebookException exception) {
                Log.e("onError", "-" + exception.getMessage());
            }
        });
    }

    //used to get hash key to create application on facebook developer page
    private void gethashkey() {
        try {
            PackageInfo info = getPackageManager().getPackageInfo("com.fblogintesting", PackageManager.GET_SIGNATURES);
            for (Signature signature : info.signatures) {
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                String sign = Base64.encodeToString(md.digest(), Base64.DEFAULT);
                Log.e("MY KEY HASH", "HashKey-" + sign);
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.e("exception", "-" + e.getMessage());
        } catch (NoSuchAlgorithmException e) {
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
        //If signin
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            //Calling a new function to handle signin
            handleSignInResult(result);
        }
    }

    //After the signing we are calling this function
    private void handleSignInResult(GoogleSignInResult result) {
        //If the login succeed
        if (result.isSuccess()) {
            //Getting google account
            GoogleSignInAccount acct = result.getSignInAccount();
            Log.e("MainActivity", "gplus name-" + acct.getDisplayName());
        } else {
            //If login fails
            Log.e("MainActivity", "Gplus signin failed");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //logging out gplus
        if (mGoogleApiClient.isConnected()) {
            Auth.GoogleSignInApi.signOut(mGoogleApiClient);
        }
        //logging out facebook
        if (AccessToken.getCurrentAccessToken() != null) {
            LoginManager.getInstance().logOut();
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }
}
