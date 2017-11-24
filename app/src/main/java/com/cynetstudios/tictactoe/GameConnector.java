package com.cynetstudios.tictactoe;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

public class GameConnector extends Activity implements OnFragmentFinishListener {

    public static String GameType = "GameType", GameConnect = "GameConnect", Test = "Test", lastAddress = "lastAddress", lastPort = "lastPort";
    public static String PlayerName = "PlayerName";
    private FragmentManager fragmentManager;
    private Fragment gameTypeFragment;
    private Intent gameIntent;
    private boolean isHosting;
    private Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gameconnector);
        mContext = this;
        gameIntent = getIntent();

        if (gameIntent.hasExtra(MainActivity.isHost))
            isHosting = gameIntent.getBooleanExtra(MainActivity.isHost, false);
        fragmentManager = getFragmentManager();
        gameTypeFragment = (isHosting)
                ? new FragmentHostGame().setOnFragmentFinishListener(this)
                : new FragmentJoinGame().setOnFragmentFinishListener(this);
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.add(R.id.fragmentContainer, gameTypeFragment, GameType);
        fragmentTransaction.commit();
    }

    @Override
    public void finished(Bundle bundle) {
        GameConnection gameConnection = null;
        if (bundle != null){
            if (bundle.containsKey(GameConnect))
                gameConnection = (GameConnection) bundle.getSerializable(GameConnect);
        }
//        fragmentManager.beginTransaction().remove(gameTypeFragment).commit();

        startGame(isHosting, gameConnection);
    }

    private void startGame(boolean isHosting, GameConnection gameConnection) {
        Intent intent = new Intent(mContext, Game.class);
        intent.putExtra(MainActivity.isHost, isHosting);
        intent.putExtra(GameConnect, gameConnection);
        String playerName = getIntent().getStringExtra(PlayerName);
        intent.putExtra(PlayerName, playerName);
        startActivityForResult(intent, 0);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (data != null){
            switch (requestCode){
                case 0:{
                    if (resultCode == 0){
                        finishActivity(resultCode);
                    }
                    else {
                        setResult(resultCode, data);
                        finishActivity(1);
                    }
                    break;
                }
            }
        }
        setResult(resultCode, null);
        finishActivity(1);
    }

    @Override
    public void finish() {
        super.finish();
    }
}
