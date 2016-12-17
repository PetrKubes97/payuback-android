package cz.petrkubes.ioweyou.Activities;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.appevents.AppEventsLogger;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;

import cz.petrkubes.ioweyou.Api.ApiRestClient;
import cz.petrkubes.ioweyou.Api.SimpleCallback;
import cz.petrkubes.ioweyou.Database.DatabaseHandler;
import cz.petrkubes.ioweyou.R;

public class LoginActivity extends AppCompatActivity {

    // Widgets
    private LoginButton loginButton;
    private ProgressBar prgLoader;
    private TextView txtLoadingDescription;

    private ApiRestClient apiClient;
    private DatabaseHandler db;
    private CallbackManager callbackManager;

    @Override
    protected void onResume() {
        super.onResume();
        // Starts application if the user is already logged in
        AccessToken accessToken = AccessToken.getCurrentAccessToken();
        if (accessToken != null) {
            if (db.getUser() != null) {
                startMainActivity();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize the SDK before executing any other operations,
        FacebookSdk.sdkInitialize(getApplicationContext());
        AppEventsLogger.activateApp(this);
        callbackManager = CallbackManager.Factory.create();

        setContentView(R.layout.activity_login);

        loginButton = (LoginButton) this.findViewById(R.id.login_button);
        prgLoader = (ProgressBar) this.findViewById(R.id.prg_loader);
        txtLoadingDescription = (TextView) this.findViewById(R.id.txt_loading_description);

        loginButton.setReadPermissions("user_friends", "email");

        loginButton.setVisibility(View.VISIBLE);
        prgLoader.setVisibility(View.GONE);

        apiClient = new ApiRestClient(getApplicationContext());
        db = new DatabaseHandler(getApplicationContext());

        // Callback registration
        loginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                // Starts loading tasks
                loginUser(loginResult.getAccessToken().getUserId(), loginResult.getAccessToken().getToken());
            }

            @Override
            public void onCancel() {
                // App code
            }

            @Override
            public void onError(FacebookException exception) {
                // App code
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }


    /**
     * Displays progress bar, logs user in, and starts main activity
     * @param facebookId user's facebook id
     * @param facebookToken user's facebook token
     */
    private void loginUser(String facebookId, final String facebookToken) {

        loginButton.setVisibility(View.GONE);
        prgLoader.setVisibility(View.VISIBLE);
        txtLoadingDescription.setText(getResources().getString(R.string.loading_login));

        apiClient.login(facebookId, facebookToken, new SimpleCallback() {
            @Override
            public void onSuccess() {
                apiClient.updateAll(db.getUser().apiKey, new SimpleCallback() {
                    @Override
                    public void onSuccess() {
                        startMainActivity();
                    }

                    @Override
                    public void onFailure(String message) {
                        // TODO handle failures
                    }
                });
            }

            @Override
            public void onFailure(String message) {

            }
        });
    }

    private void startMainActivity() {
        Intent intent = new Intent(getBaseContext(), MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }
}