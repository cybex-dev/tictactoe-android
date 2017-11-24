package com.cynetstudios.tictactoe;

import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

import static com.cynetstudios.tictactoe.GameConnector.GameConnect;
import static com.cynetstudios.tictactoe.MainActivity.PreferencesFile;
import static com.cynetstudios.tictactoe.Utils.getIPFilter;
import static com.cynetstudios.tictactoe.Utils.getPortFilters;

public class FragmentJoinGame extends Fragment implements OnFragmentFinishListener {

    private OnFragmentFinishListener listener;
    private EditText edtPort, edtAddress;
    private Button btnPlay;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
//        return super.onCreateView(inflater, container, savedInstanceState);
        View v = inflater.inflate(R.layout.activity_joingame, container, false);

        edtAddress = v.findViewById(R.id.edtIPAddress);
        edtPort = v.findViewById(R.id.edtPort);
        btnPlay = v.findViewById(R.id.btnJoinGame);

        // https://stackoverflow.com/questions/7992216/android-fragment-handle-back-button-press
        v.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int i, KeyEvent keyEvent) {
                if (i == KeyEvent.KEYCODE_BACK) {
                    finished(null);
                    return true;
                }
                return false;
            }
        });
        loadPreferences();
        return v;

    }

    private void loadPreferences() {
        SharedPreferences prefs = getActivity().getSharedPreferences(PreferencesFile, Context.MODE_PRIVATE);
        edtAddress.setText(prefs.getString(GameConnector.lastAddress, "0.0.0.0"));
        edtPort.setText(prefs.getString(GameConnector.lastPort, "1025"));
    }

    private void savePreferences() {
        SharedPreferences prefs = getActivity().getSharedPreferences(PreferencesFile, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(GameConnector.lastAddress)
                .remove(GameConnector.lastPort)
                .putString(GameConnector.lastAddress, edtAddress.getText().toString())
                .putString(GameConnector.lastPort, edtPort.getText().toString())
                .apply();

    }

    public FragmentJoinGame setOnFragmentFinishListener(OnFragmentFinishListener listener) {
        this.listener = listener;
        return this;
    }

    @Override
    public void finished(Bundle bundle) {
        listener.finished(bundle);
    }

    @Override
    public void onResume() {
        super.onResume();
        edtAddress.setFilters(getIPFilter());
        edtPort.setFilters(getPortFilters());
        btnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (edtAddress.getText().toString().isEmpty())
                    edtAddress.setError("Enter IP address of Server to play");
                if (edtPort.getText().toString().isEmpty())
                    edtPort.setError("Enter Port of Server to play");
                Bundle b = new Bundle();
                InetAddress ipAddr = null;
                int port;
                try {
                    ipAddr = Inet4Address.getByName(edtAddress.getText().toString());
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                    Toast.makeText(getActivity().getApplicationContext(), "Unable to get IP address", Toast.LENGTH_SHORT).show();
                    finished(new Bundle());
                }
                port = Integer.parseInt(edtPort.getText().toString());

                GameConnection gameConnection = new GameConnection(ipAddr, port);
                if (Utils.checkRemotePort(gameConnection, GameConnector.Test)) {
                    b.putSerializable(GameConnect, gameConnection);
                    b.putBoolean(MainActivity.isHost, false);
                    finished(b);
                    savePreferences();
                } else {
                    Toast.makeText(getActivity().getApplicationContext(),
                            "No game found on " + edtAddress.getText().toString() + ":" + edtPort.getText().toString(),
                            Toast.LENGTH_LONG).show();
                }

            }
        });
    }
}
