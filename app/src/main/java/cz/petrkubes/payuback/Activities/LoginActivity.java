package cz.petrkubes.payuback.Activities;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.appevents.AppEventsLogger;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;

import org.w3c.dom.Text;

import cz.petrkubes.payuback.Api.ApiRestClient;
import cz.petrkubes.payuback.Api.SimpleCallback;
import cz.petrkubes.payuback.Const;
import cz.petrkubes.payuback.Database.DatabaseHandler;
import cz.petrkubes.payuback.R;


public class LoginActivity extends AppCompatActivity {

    private LoginButton loginButton;
    private ProgressBar prgLoader;
    private TextView txtLoadingDescription;
    private ApiRestClient apiClient;
    private DatabaseHandler db;
    private CallbackManager callbackManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize the SDK before executing any other operations,
        FacebookSdk.sdkInitialize(getApplicationContext());
        AppEventsLogger.activateApp(this);
        callbackManager = CallbackManager.Factory.create();

        setContentView(R.layout.activity_login);

        // Remove actionbar shadow
        getSupportActionBar().setElevation(0);

        loginButton = (LoginButton) this.findViewById(R.id.login_button);
        prgLoader = (ProgressBar) this.findViewById(R.id.prg_loader);
        txtLoadingDescription = (TextView) this.findViewById(R.id.txt_loading_description);

        loginButton.setReadPermissions("user_friends", "email");

        loginButton.setVisibility(View.VISIBLE);
        prgLoader.setVisibility(View.GONE);

        apiClient = new ApiRestClient(getApplicationContext());
        db = new DatabaseHandler(getApplicationContext());

        // Starts application if the user is already logged in
        AccessToken accessToken = AccessToken.getCurrentAccessToken();
        if (accessToken != null) {
            if (db.getUser() != null) {
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                intent.putExtra("facebookId", AccessToken.getCurrentAccessToken().getUserId());
                intent.putExtra("facebookToken", AccessToken.getCurrentAccessToken().getToken());
                startActivity(intent);
            } else {
                loginUser(accessToken.getUserId(), accessToken.getToken());
            }
        }

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


    public void loginUser(String facebookId, String facebookToken) {

        loginButton.setVisibility(View.GONE);
        prgLoader.setVisibility(View.VISIBLE);
        txtLoadingDescription.setText(getResources().getString(R.string.loading_login));

        apiClient.login(facebookId, facebookToken, new SimpleCallback() {
            @Override
            public void onSuccess() {
                getUser(db.getUser().apiKey);
            }

            @Override
            public void onFailure() {

            }
        });
    }

    public void getUser(final String apiKey) {

        txtLoadingDescription.setText(getResources().getString(R.string.loading_getting_user));

        apiClient.getUser(apiKey, new SimpleCallback() {

            @Override
            public void onSuccess() {
                getCurrencies(apiKey);
            }

            @Override
            public void onFailure() {

            }
        });
    }

    public void getCurrencies(String apiKey) {

        txtLoadingDescription.setText(getResources().getString(R.string.loading_other));

        apiClient.getCurrencies(apiKey, new SimpleCallback() {
            @Override
            public void onSuccess() {

                txtLoadingDescription.setText("");
                prgLoader.setVisibility(View.GONE);
                loginButton.setVisibility(View.VISIBLE);

                // Starts application
                Intent intent = new Intent(getBaseContext(), MainActivity.class);
                startActivity(intent);

            }

            @Override
            public void onFailure() {

            }
        });
    }
}
