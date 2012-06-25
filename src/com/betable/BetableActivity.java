package com.betable;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import com.betable.fragment.BetableLogin;
import com.betable.fragment.BetableLogin.BetableLoginListener;

public class BetableActivity extends FragmentActivity implements
        BetableLoginListener {

    Betable betable;
    BetableLogin loginView;
    Button loginButton;

    private final String clientId = "j4lAcOwsZ8Wdh6DObWxaYzg2sHfppF6t";
    private final String clientSecret = "C62RUr2nTjAjdlCxowxCAr8VzMqdWSlp";
    private final String redirectUri = "http://127.0.0.1:8000/callback";
    private String accessToken;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.main);

        this.loginButton = (Button) this
                .findViewById(R.id.betable_login_button);

        this.loginButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                BetableActivity.this.loginView.show(
                        BetableActivity.this.getSupportFragmentManager(),
                        "betable-login");
            }

        });

        if (savedInstanceState != null) {
            this.loginView = (BetableLogin) this.getSupportFragmentManager()
                    .getFragment(savedInstanceState,
                            BetableLogin.class.getName());
        } else {
            this.loginView = BetableLogin.newInstance(this.clientId,
                    this.clientSecret, this.redirectUri);
        }

        this.loginView.setListener(this);
    }

    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        this.getSupportFragmentManager().putFragment(savedInstanceState,
                BetableLogin.class.getName(), this.loginView);
    }

    @Override
    public void onSuccessfulLogin(String accessToken) {
        Toast.makeText(this,
                "Hooray, we have an access token! It's " + accessToken,
                Toast.LENGTH_LONG).show();
        this.accessToken = accessToken;
        this.betable = new Betable(this.accessToken);
        this.loginView.dismiss();
    }

    @Override
    public void onFailedLogin(String reason) {
        Toast.makeText(this, "Bummer, something went wrong. " + reason,
                Toast.LENGTH_LONG).show();
        if (this.loginView.isVisible()) {
            this.loginView.dismiss();
        }
    }
}