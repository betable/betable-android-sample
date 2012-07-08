package com.betable;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;

public class BetActivity extends FragmentActivity {
    protected static final String TAG = "BetActivity";

    public static final String ACCESS_TOKEN_KEY = "com.betable.ACCESS_TOKEN";
    public static final String TITLE_KEY = "com.betable.TITLE";
    public static final String BET_AMOUNT_KEY = "com.betable.BET_AMOUNT";

    AlertDialog betDialog;
    BetableRequestHandler requestHandler;
    String betAmount;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.bet);

        this.betDialog = this.createBetDialog();

        if (savedInstanceState == null) {
            BetableApp.BETABLE = new Betable(this.getIntent().getExtras().getString(ACCESS_TOKEN_KEY));
            BetableApp.BETABLE.setGameId(BetableApp.getBetableProperty(BetableApp.GAME_ID_KEY));
            this.requestHandler = new BetableRequestHandler();
            BetableApp.BETABLE.getUser(this.requestHandler);
        } else {
            this.requestHandler = (BetableRequestHandler) this.getLastCustomNonConfigurationInstance();
        }

        this.findViewById(R.id.can_i_gamble_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Location location = BetActivity.this.getLastKnownLocation();
                if (location != null) {
                    BetableApp.BETABLE.canIGamble(BetActivity.this.getLastKnownLocation(),BetActivity.this.requestHandler);
                } else {
                    Toast.makeText(BetActivity.this, "Could not get a location.", Toast.LENGTH_LONG).show();
                }
            }
        });

        this.findViewById(R.id.get_user_wallet_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                BetableApp.BETABLE.getUserWallet(BetActivity.this.requestHandler);
            }
        });

        this.findViewById(R.id.bet_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                BetActivity.this.betDialog.show();
            }
        });
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putString(TITLE_KEY, this.getTitle().toString());
        savedInstanceState.putString(BET_AMOUNT_KEY, "");
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        this.setTitle(savedInstanceState.getString(TITLE_KEY));
        this.betAmount = savedInstanceState.getString(BET_AMOUNT_KEY);
    }

    @Override
    public Object onRetainCustomNonConfigurationInstance() {
        return this.requestHandler;
    }

    private AlertDialog createBetDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        if (this.betAmount != null) input.setText(this.betAmount);
        builder.setMessage("How much would you like to bet?")
                .setView(input)
                .setCancelable(true)
                .setPositiveButton("Bet!", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        try {
                            BetActivity.this.betAmount = input.getText().toString().trim();
                            JSONObject betPayload = BetActivity.this.createBetPayload(BetActivity.this.betAmount);
                            BetableApp.BETABLE.bet(betPayload, BetActivity.this.requestHandler);
                        } catch (JSONException e) {
                            Log.e(TAG, e.getMessage());
                        }
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                });
        return builder.create();
    }

    private JSONObject createBetPayload(String betAmount) throws JSONException {
        JSONObject bet = new JSONObject();
        bet.put("wager", betAmount);
        bet.put("currency", "GBP");
        bet.put("paylines", new JSONArray("[[1,1,1],[2,2,2]]"));
        Location location = this.getLastKnownLocation();
        bet.put("location", String.valueOf(location.getLatitude()) +  "," + String.valueOf(location.getLongitude()));
        return bet;
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

    private void handleUserResponse(JSONObject body) {
        StringBuilder nameBuilder = new StringBuilder();
        try {
            nameBuilder.append(body.getString("first_name")).append(" ").append(
                    body.getString("last_name"));
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage());
        }
        this.setTitle(nameBuilder.toString());
    }

    private void handleGambleResponse(JSONObject body) {
        boolean canIGamble = false;
        try {
            canIGamble = body.getBoolean("can_gamble");
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage());
        }

        if (canIGamble) {
            Toast.makeText(this, "Yes! You can gamble!", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Sorry, you can't gamble here.", Toast.LENGTH_LONG).show();
        }
    }

    private void handleWalletResponse(JSONObject body) {
        String monies = "", currencyType = "";
        try {
            monies = body.getJSONObject("sandbox").getString("balance");
            currencyType = body.getJSONObject("sandbox").getString("currency");
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage());
        }

        String currentTitle = this.getTitle().toString();
        this.setTitle(currentTitle + " - " + monies + " (" + currencyType + ")");
    }

    private void handleBetResponse(JSONObject body) {
        String payout = null;
        try {
            payout = body.getString("payout");
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage());
        }

        if (payout != null && payout.equals("0.00")) {
            Toast.makeText(this, "Sorry, you didn't win this time.", Toast.LENGTH_LONG).show();
        } else if (payout != null) {
            Toast.makeText(this, "Hooray, you won " + payout + "!", Toast.LENGTH_LONG).show();
        }
    }

    private JSONObject formatResponse(HttpEntity entity) {
        JSONObject json = null;
        try {
            json = new JSONObject(EntityUtils.toString(entity));
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage());
        }
        return json;
    }

    // request handler

    class BetableRequestHandler extends Handler {

        @Override
        public void handleMessage(Message message) {
            JSONObject responseBody = BetActivity.this.formatResponse(((HttpResponse) message.obj).getEntity());
            switch(message.what) {
                case Betable.USER_REQUEST:
                    BetActivity.this.handleUserResponse(responseBody);
                    break;
                case Betable.GAMBLE_REQUEST:
                    BetActivity.this.handleGambleResponse(responseBody);
                    break;
                case Betable.WALLET_REQUEST:
                    BetActivity.this.handleWalletResponse(responseBody);
                    break;
                case Betable.BET_REQUEST:
                    BetActivity.this.handleBetResponse(responseBody);
                    break;
            }
        }

    }
}
