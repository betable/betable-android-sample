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
    public static final String TITLE_KEY = "com.betable.TITLE";

    Button canIGambleButton;
    Button getUserWalletButton;
    Button betButton;
    String accessToken;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.bet);

        this.accessToken = this.getIntent().getExtras().getString(ACCESS_TOKEN_KEY);
        BetableApp.BETABLE = new Betable(this.accessToken);

        if (savedInstanceState == null) {
            BetableApp.BETABLE.getUser(new Handler() {
                @Override
                public void handleMessage(Message message) {
                    HttpResponse response = (HttpResponse) message.obj;
                    JSONObject responseBody = null;
                    StringBuilder nameBuilder = new StringBuilder();
                    try {
                        responseBody = new JSONObject(EntityUtils.toString(response.getEntity()));
                        nameBuilder.append(responseBody.getString("first_name")).append(" ").append(
                            responseBody.getString("last_name"));
                    } catch (IOException e) {
                        Log.e(TAG, e.getMessage());
                    } catch (JSONException e) {
                        Log.e(TAG, e.getMessage());
                    }

                    if (responseBody != null) {
                        BetActivity.this.setTitle(nameBuilder.toString());
                    }
                }
            });
        }

        this.initializeButtons();

        this.canIGambleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Location location = BetActivity.this.getLastKnownLocation();
                if (location != null) {
                    BetableApp.BETABLE.canIGamble(BetActivity.this.getLastKnownLocation(), new Handler() {
                        @Override
                        public void handleMessage(Message message) {
                        }
                    });
                } else {
                    Toast.makeText(BetActivity.this, "Could not get a location.", Toast.LENGTH_LONG).show();
                }
            }
        });

        this.getUserWalletButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                BetableApp.BETABLE.getUserWallet(new Handler() {
                    @Override
                    public void handleMessage(Message message) {
                        HttpResponse response = (HttpResponse) message.obj;

                        JSONObject responseBody = null;
                        String monies = "",
                                currencyType = "";
                        try {
                            responseBody = new JSONObject(EntityUtils.toString(response.getEntity()));
                            monies = responseBody.getJSONObject("sandbox").getString("balance");
                            currencyType = responseBody.getJSONObject("sandbox").getString("currency");
                        } catch (IOException e) {
                            Log.e(TAG, e.getMessage());
                        } catch (JSONException e) {
                            Log.e(TAG, e.getMessage());
                        }

                        if (responseBody != null) {
                            String currentTitle = BetActivity.this.getTitle().toString();
                            BetActivity.this.setTitle(currentTitle + " - " + monies + " (" + currencyType + ")");
                        }
                    }
                });
            }
        });

        this.betButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                BetableApp.BETABLE.bet(null, new Handler() {

                });
            }
        });
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putString(TITLE_KEY, this.getTitle().toString());
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        this.setTitle(savedInstanceState.getString(TITLE_KEY));
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
}
