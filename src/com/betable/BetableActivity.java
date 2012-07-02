package com.betable;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.betable.fragment.BetableLogin;
import com.betable.fragment.BetableLogin.BetableLoginListener;

public class BetableActivity extends FragmentActivity implements
        BetableLoginListener {

    static String loginTag = "betable-login";
    static String visibilityKey = "visibility_key";

    Betable betable;
    BetableLogin betableLogin;
    Button loginButton;
    FrameLayout loginFrame;

    private String accessToken;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.main);

        this.loginButton = (Button) this.findViewById(R.id.betable_login_button);
        this.loginFrame = (FrameLayout) this.findViewById(R.id.betable_login_view);
        if (savedInstanceState != null) {
            this.loginFrame.setVisibility(savedInstanceState.getInt(visibilityKey));
        }

        this.loginButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                BetableActivity.this.betableLogin.show(BetableActivity.this.getSupportFragmentManager(),
                        R.id.betable_login_view, BetableActivity.this.loginTag);
                BetableActivity.this.loginFrame.setVisibility(FrameLayout.VISIBLE);
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
        Toast.makeText(this,
                "Hooray, we have an access token! It's " + accessToken,
                Toast.LENGTH_LONG).show();
        this.accessToken = accessToken;
        this.betable = new Betable(this.accessToken);
        if (this.betableLogin.isVisible()) {
            this.betableLogin.dismiss();
        }
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