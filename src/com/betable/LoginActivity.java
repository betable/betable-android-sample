package com.betable;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.FrameLayout;
import android.widget.Toast;
import com.betable.fragment.BetableLogin;
import com.betable.fragment.BetableLogin.BetableLoginListener;

@SuppressWarnings("unused")
public class LoginActivity extends FragmentActivity implements BetableLoginListener {
    protected static final String TAG = "LoginActivity";

    static String loginTag = "betable-login";
    static String visibilityKey = "visibility_key";

    BetableLogin betableLogin;
    FrameLayout loginFrame;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.main);

        this.loginFrame = (FrameLayout) this.findViewById(R.id.betable_login_view);
        if (savedInstanceState != null) {
            this.loginFrame.setVisibility(savedInstanceState.getInt(visibilityKey));
        }

        this.findViewById(R.id.betable_login_button).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                LoginActivity.this.betableLogin.show(LoginActivity.this.getSupportFragmentManager(),
                        R.id.betable_login_view, loginTag);
                LoginActivity.this.loginFrame.setVisibility(FrameLayout.VISIBLE);
            }

        });

        this.betableLogin = (BetableLogin) this.getSupportFragmentManager().findFragmentByTag(loginTag);
        if (this.betableLogin == null) {
            this.betableLogin = BetableLogin.newInstance(BetableApp.getBetableProperty(BetableApp.CLIENT_ID_KEY),
                    BetableApp.getBetableProperty(BetableApp.CLIENT_SECRET_KEY),
                    BetableApp.getBetableProperty(BetableApp.REDIRECT_URI_KEY));
        }
    }

    @Override
    public void onSuccessfulLogin(String accessToken) {
        if (this.betableLogin.isVisible()) {
            this.betableLogin.dismiss();
            this.loginFrame.setVisibility(FrameLayout.INVISIBLE);
        }
        Intent intent = new Intent(this, BetActivity.class);
        intent.putExtra(BetActivity.ACCESS_TOKEN_KEY, accessToken);
        this.startActivity(intent);
    }

    @Override
    public void onFailedLogin(String reason) {
        Toast.makeText(this, "Bummer, something went wrong. " + reason,
                Toast.LENGTH_LONG).show();
        if (this.betableLogin.isVisible()) {
            this.betableLogin.dismiss();
            this.loginFrame.setVisibility(FrameLayout.INVISIBLE);
        }
    }

}