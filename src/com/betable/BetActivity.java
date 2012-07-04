package com.betable;

import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
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
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;

public class BetActivity extends FragmentActivity {
    protected static final String TAG = "BetActivity";

    public static final String ACCESS_TOKEN_KEY = "com.betable.ACCESS_TOKEN";

    Button canIGambleButton;
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
            BetableApp.BETABLE.getUser(this.httpResponseHandler);
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

            Log.d(TAG, "Response status " + String.valueOf(status));

            JSONObject responseJson = null;
            try {
                responseJson = new JSONObject(EntityUtils.toString(response.getEntity(), "UTF-8"));
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            } catch (JSONException e) {
                Log.e(TAG, e.getMessage());
            }

            if (responseJson == null) {
                return;
            } else {
                Log.d(TAG, responseJson.toString());
            }

            if (responseJson.has("first_name")) {
                try  {
                    BetActivity.this.setTitle(responseJson.getString("first_name") + " " + responseJson.getString("last_name"));
                } catch (JSONException e) {
                    Log.e(TAG, e.getMessage());
                }
            }

        }
    }
}
