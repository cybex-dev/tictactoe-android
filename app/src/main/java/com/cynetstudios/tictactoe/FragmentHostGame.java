package com.cynetstudios.tictactoe;

import android.app.Fragment;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

import static com.cynetstudios.tictactoe.GameConnector.GameConnect;
import static com.cynetstudios.tictactoe.Utils.getPortFilters;

public class FragmentHostGame extends Fragment implements OnFragmentFinishListener {

    private OnFragmentFinishListener listener;
    private EditText edtHostPort;
    private Spinner spinnerAddress;
    private Button btnHostGame;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
//        return super.onCreateView(inflater, container, savedInstanceState);
        View v = inflater.inflate(R.layout.activity_hostgame, container, false);

        spinnerAddress = v.findViewById(R.id.spinnerHostIPAddress);
        edtHostPort = v.findViewById(R.id.edtHostPort);
            btnHostGame = v.findViewById(R.id.btnHostGame);

        // https://stackoverflow.com/questions/7992216/android-fragment-handle-back-button-press
        v.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int i, KeyEvent keyEvent) {
                if( i == KeyEvent.KEYCODE_BACK )
                {
                    finished(null);
                }
                return false;
            }
        });
        return v;

    }

    private void populateSpinner() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                final List<String> allIPv4Addresses = Utils.getAllIPv4Addresses();
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        spinnerAddress.setAdapter(new ArrayAdapter<>(getActivity().getApplicationContext(), R.layout.spinner_item, allIPv4Addresses));
                    }
                });
            }
        }).start();
    }

    public FragmentHostGame setOnFragmentFinishListener(OnFragmentFinishListener listener){
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
        populateSpinner();
        edtHostPort.setFilters(getPortFilters());
        btnHostGame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (edtHostPort.getText().toString().isEmpty())
                    edtHostPort.setError("Enter Port of Server to play");
                Bundle b = new Bundle();

                InetAddress ipAddr = null;
                int port = 0;
                try {
                    ipAddr = Inet4Address.getByName(spinnerAddress.getSelectedItem().toString());
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                    Toast.makeText(getActivity().getApplicationContext(), "Unable to get IP address", Toast.LENGTH_SHORT).show();
                    finished(new Bundle());
                }
                port = Integer.parseInt(edtHostPort.getText().toString());
                GameConnection gameConnection = new GameConnection(ipAddr, port);

                if (Utils.checkLocalPort(gameConnection)){
                    b.putSerializable(GameConnect, gameConnection);
                    b.putBoolean(MainActivity.isHost, true);
                    finished(b);
                } else {
                    Toast.makeText(getActivity().getApplicationContext(),
                            "Cannot use port " + edtHostPort.getText().toString() + " using IP: " + ipAddr.getHostAddress(),
                            Toast.LENGTH_LONG).show();
                }

            }
        });
    }
}
