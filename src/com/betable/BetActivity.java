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

    AlertDialog betDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.bet);

        if (savedInstanceState == null) {
            BetableApp.BETABLE = new Betable(this.getIntent().getExtras().getString(ACCESS_TOKEN_KEY));
            BetableApp.BETABLE.setGameId(BetableApp.getBetableProperty(BetableApp.GAME_ID_KEY));
            this.getUserInfo();
            this.betDialog = this.createBetDialog();
        } else {
            this.betDialog = (AlertDialog) this.getLastCustomNonConfigurationInstance();
        }

        this.findViewById(R.id.can_i_gamble_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Location location = BetActivity.this.getLastKnownLocation();
                if (location != null) {
                    BetableApp.BETABLE.canIGamble(BetActivity.this.getLastKnownLocation(), new Handler() {
                        @Override
                        public void handleMessage(Message message) {
                            HttpResponse response = (HttpResponse) message.obj;

                            JSONObject responseBody;
                            boolean canIGamble = false;
                            try {
                                responseBody = new JSONObject(EntityUtils.toString(response.getEntity()));
                                canIGamble = responseBody.getBoolean("can_gamble");
                            } catch (IOException e) {
                                Log.e(TAG, e.getMessage());
                            } catch (JSONException e) {
                                Log.e(TAG, e.getMessage());
                            }

                            if (canIGamble) {
                                Toast.makeText(BetActivity.this, "Yes! You can gamble!", Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(BetActivity.this, "Sorry, you can't gamble here.", Toast.LENGTH_LONG).show();
                            }
                        }
                    });
                } else {
                    Toast.makeText(BetActivity.this, "Could not get a location.", Toast.LENGTH_LONG).show();
                }
            }
        });

        this.findViewById(R.id.get_user_wallet_button).setOnClickListener(new View.OnClickListener() {
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
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        this.setTitle(savedInstanceState.getString(TITLE_KEY));
    }

    @Override
    public Object onRetainCustomNonConfigurationInstance() {
        return this.betDialog;
    }

    private AlertDialog createBetDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        builder.setMessage("How much would you like to bet?")
                .setView(input)
                .setCancelable(true)
                .setPositiveButton("Bet!", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        try {
                            JSONObject betPayload = BetActivity.this.createBetPayload(input.getText().toString().trim());
                            Log.d(TAG, betPayload.toString());
                            BetableApp.BETABLE.bet(betPayload, new Handler() {
                                @Override
                                public void handleMessage(Message message) {
                                    HttpResponse response = (HttpResponse) message.obj;
                                    try {
                                        Log.d(TAG, EntityUtils.toString(response.getEntity()));
                                    } catch (IOException e) {
                                        Log.e(TAG, e.getMessage());
                                    }
                                }
                            });
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
        bet.put("wager", betAmount + ".00");
        bet.put("currency", "GBP");
        bet.put("paylines", new JSONArray("[[1,1,1],[2,2,2]]"));
        Location location = this.getLastKnownLocation();
        bet.put("location", String.valueOf(location.getLatitude()) +  "," + String.valueOf(location.getLongitude()));
        return bet;
    }

    private void getUserInfo() {
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
