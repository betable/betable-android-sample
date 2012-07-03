package com.betable;

import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.nfc.Tag;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.List;

public class BetActivity extends FragmentActivity {
    protected static final String TAG = "BetActivity";

    public static final String ACCESS_TOKEN_KEY = "com.betable.ACCESS_TOKEN";

    Button canIGambleButton;
    Button getUserButton;
    Button getUserWalletButton;
    Button betButton;
    HttpResponseHandler httpResponseHandler;
    String accessToken;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.bet);

        this.accessToken = this.getIntent().getExtras().getString(ACCESS_TOKEN_KEY);
        BetableApp.BETABLE = new Betable(this.accessToken);
        if (this.getLastCustomNonConfigurationInstance() == null) {
            this.httpResponseHandler = new HttpResponseHandler();
        } else {
            this.httpResponseHandler = (HttpResponseHandler) this.getLastCustomNonConfigurationInstance();
        }

        this.initializeButtons();

        this.canIGambleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Location location = BetActivity.this.getLastKnownLocation();
                if (location != null) {
                    BetableApp.BETABLE.canIGamble(BetActivity.this.getLastKnownLocation(),
                            BetActivity.this.httpResponseHandler);
                } else {
                    Toast.makeText(BetActivity.this, "Could not get a location.", Toast.LENGTH_LONG).show();
                }
            }
        });

        this.getUserButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                BetableApp.BETABLE.getUser(BetActivity.this.httpResponseHandler);
            }
        });

        this.getUserWalletButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                BetableApp.BETABLE.getUserWallet(BetActivity.this.httpResponseHandler);
            }
        });

        this.betButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                BetableApp.BETABLE.bet(null, BetActivity.this.httpResponseHandler);
            }
        });
    }

    @Override
    public Object onRetainCustomNonConfigurationInstance() {
        return this.httpResponseHandler;
    }

    private void initializeButtons() {
        this.canIGambleButton = (Button) this.findViewById(R.id.can_i_gamble_button);
        this.getUserButton = (Button) this.findViewById(R.id.get_user_button);
        this.getUserWalletButton = (Button) this.findViewById(R.id.get_user_wallet_button);
        this.betButton = (Button) this.findViewById(R.id.bet_button);
    }

    private Location getLastKnownLocation() {
        LocationManager manager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        List<String> providers = manager.getAllProviders();
        Location bestLocation = null;
        for (String provider : providers) {
            Location location = manager.getLastKnownLocation(provider);
            if (bestLocation == null) {
                bestLocation = location;
            } else if (location != null && location.getTime() > bestLocation.getTime()) {
                bestLocation = location;
            }
        }
        return bestLocation;
    }

    class HttpResponseHandler extends Handler {
        @Override
        public void handleMessage(Message message) {
            int status = message.what;
            HttpResponse response = (HttpResponse) message.obj;

            Toast.makeText(BetActivity.this, "Response status " + String.valueOf(status), Toast.LENGTH_SHORT).show();

            String responseBody = "";
            try {
                responseBody = EntityUtils.toString(response.getEntity(), "UTF-8");
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            }

            Toast.makeText(BetActivity.this, responseBody, Toast.LENGTH_LONG).show();
            Log.d(TAG, responseBody);
        }
    }
}
