package com.cynetstudios.tictactoe;

import android.annotation.SuppressLint;
import android.app.*;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    public static String isHost = "isHost", PreferencesFile = "general_settings";

    private Context mContext;
    private Button btnHostGame, btnJoinGame, btnHowToPlay, btnExit;
    private ImageView btnSettings;
    private TextView lblPlayer;
    private String playerName = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
        setContentView(R.layout.activity_main);
        initGui();
        loadPreferences();
    }

    private void loadPreferences() {
        SharedPreferences prefs = getSharedPreferences(PreferencesFile, Context.MODE_PRIVATE);
        playerName = prefs.getString(GameConnector.PlayerName, "");
        updateGui();
    }

    private void savePreferences(){
        SharedPreferences preferences = mContext.getSharedPreferences(MainActivity.PreferencesFile, MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(GameConnector.PlayerName, playerName).apply();
    }

    private void initGui() {
        btnHostGame = findViewById(R.id.btnHostGame);
        btnJoinGame = findViewById(R.id.btnJoinGame);
        btnHowToPlay = findViewById(R.id.btnHowToPlay);
        btnExit = findViewById(R.id.btnExit);

        btnHostGame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startGameActivity(true);
            }
        });
        btnJoinGame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startGameActivity(false);
            }
        });
        btnHowToPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new AlertDialog.Builder(mContext)
                        .setTitle("How to Play")
                        .setMessage("How can I play?\n-------------------------\n\nJoining a game:\n\t- Go to Join Game\t\n- Enter the server address and port number\n\t- Click Join\n\nHost a Game:\n\t- Go to Host Game\n\t- Select the address on which you want to host the game and the port\n\t- Click Host Game\n=============================\n\nWhat do I do?\n----------------------\n\nOne player has the \'O\' symbol and another has the \'X\' symbol\n\nPlace these on the 3x3 Board, and try to form a straight line either \n\t\t- Diagonally\n\t\t- Vertically\n\t\t- Horizontally\n\nThe first player to do this wins.\n\nHave Fun!")
                        .setNeutralButton("Ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                            }
                        }).create().show();
            }
        });
        btnExit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new AlertDialog.Builder(mContext)
                        .setTitle("Exiting...")
                        .setMessage("Are you sure?")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                                System.exit(0);
                            }
                        })
                        .setNegativeButton("No", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                            }
                        })
                        .create().show();
            }
        });
        btnSettings = findViewById(R.id.btnSettings);
        btnSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                askName();
            }
        });
        lblPlayer = findViewById(R.id.lblPlayer);
    }

    private void startGameActivity(boolean isHosting) {
        if (!playerName.isEmpty()) {
            Intent intent = new Intent(mContext, GameConnector.class);
            intent.putExtra(isHost, isHosting);
            intent.putExtra(GameConnector.PlayerName, playerName);
            startActivityForResult(intent, 0);
        } else {
            Toast.makeText(mContext, "Player name cannot be empty", Toast.LENGTH_SHORT).show();
            askName();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (playerName.isEmpty()) {
            askName();
        }
    }

    private void askName() {
        final Utils.InputDialog d = new Utils.InputDialog(mContext);
        d.setTitle("Enter your name");
        d.setInput(playerName);
        d.show();
        d.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        playerName = d.getInput();
                        savePreferences();
                        updateGui();
                    }
                });
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void updateGui(){
        lblPlayer.setText(mContext.getResources().getString(R.string.welcome).concat(" ").concat(playerName));
    }
}
