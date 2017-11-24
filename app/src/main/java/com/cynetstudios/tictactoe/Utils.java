package com.cynetstudios.tictactoe;

import android.app.Dialog;
import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.text.InputFilter;
import android.text.Spanned;
import android.util.Log;
import android.view.View;
import android.widget.*;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

public class Utils {
    public static InputFilter[] getPortFilters() {
        InputFilter[] filters = new InputFilter[1];
        filters[0] = new InputFilter() {
            @Override
            public CharSequence filter(CharSequence source, int start,
                                       int end, Spanned dest, int dstart, int dend) {
                if (end > start) {
                    String destTxt = dest.toString();
                    String resultingTxt = destTxt.substring(0, dstart) +
                            source.subSequence(start, end) +
                            destTxt.substring(dend);
                    if (!resultingTxt.matches("[0-9]{0,5}")) {
                        return "";
                    } else {
                        int v = Integer.parseInt(resultingTxt);
                        if (v > 65535)
                            return "";
                    }
                }
                return null;
            }
        };
        return filters;
    }

    public static InputFilter[] getIPFilter() {
        // https://stackoverflow.com/questions/8661915/what-androidinputtype-should-i-use-for-entering-an-ip-address
        InputFilter[] filters = new InputFilter[1];
        filters[0] = new InputFilter() {
            @Override
            public CharSequence filter(CharSequence source, int start,
                                       int end, Spanned dest, int dstart, int dend) {
                if (end > start) {
                    String destTxt = dest.toString();
                    String resultingTxt = destTxt.substring(0, dstart) +
                            source.subSequence(start, end) +
                            destTxt.substring(dend);
                    if (!resultingTxt.matches("^\\d{1,3}(\\." + "(\\d{1,3}(\\.(\\d{1,3}(\\.(\\d{1,3})?)?)?)?)?)?")) {
                        return "";
                    } else {
                        String[] splits = resultingTxt.split("\\.");
                        for (int i = 0; i < splits.length; i++) {
                            if (Integer.valueOf(splits[i]) > 255) {
                                return "";
                            }
                        }
                    }
                }
                return null;
            }
        };
        return filters;
    }

    public static List<String> getAllIPv4Addresses() {
        List<String> listNetworkIps = new ArrayList<>();
        try {
            Enumeration e = NetworkInterface.getNetworkInterfaces();
            while (e.hasMoreElements()) {
                NetworkInterface n = (NetworkInterface) e.nextElement();
                Enumeration ee = n.getInetAddresses();
                while (ee.hasMoreElements()) {
                    InetAddress i = (InetAddress) ee.nextElement();
                    if (i instanceof Inet4Address) {
                            if (!i.getHostAddress().toLowerCase().contains("localhost") &&
                                    !i.getHostAddress().toLowerCase().contains("127.0.0.1")){
                            listNetworkIps.add(i.getHostAddress());
                        }
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return listNetworkIps;
    }

    public static boolean checkRemotePort(final GameConnection gameConnection, final String message) {
        final AtomicBoolean status = new AtomicBoolean(false), isActive = new AtomicBoolean(false);
        int i = 0;
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // possible issue with server opening and sending new data
                    // possible issue with exception missing correct handling procedure when testing socket
                    Socket clientSocket = new Socket(gameConnection.getAddress(), gameConnection.getPort());
                    isActive.set(true);
                    ObjectOutputStream outputStream = new ObjectOutputStream(clientSocket.getOutputStream());
                    ObjectInputStream inputStream = new ObjectInputStream(clientSocket.getInputStream());

                    //server says hello, client 'politely' says test
                    Object o = inputStream.readObject();
                    if (o.toString().equals(Game.Request)){
                        outputStream.writeObject(message);
                        outputStream.flush();
                        status.set(true);
                    } else
                        status.set(false);
                    if (clientSocket != null)
                        if (!clientSocket.isClosed())
                            clientSocket.close();


                } catch (IOException e) {
                    Log.e("Utils::checkRemotePort", "Socket could not connect to " + gameConnection.getAddress().getHostAddress() + ":" + String.valueOf(gameConnection.getPort()) + "\n" + e.toString());
                    e.printStackTrace();
                    status.set(false);
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        });
        t.start();
        try {
            while (t.isAlive()) {
                if (i < 10) {
                    Thread.sleep(100);
                    i++;
                } else {
                    if (isActive.get() == Boolean.TRUE) {
                        t.join();
                    } else {
                        t.interrupt();
                        return false;
                    }
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }
        return status.get();

    }

    public static boolean checkLocalPort(final GameConnection gameConnection) {
        final AtomicBoolean status = new AtomicBoolean(false);
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // possible issue with server opening and sending new data
                    // possible issue with exception missing correct handling procedure when testing socket
                    ServerSocket serverSocket = new ServerSocket(gameConnection.getPort(), 0, gameConnection.getAddress());
                    status.set(true);
                    if (serverSocket != null)
                        if (!serverSocket.isClosed())
                            serverSocket.close();

                } catch (IOException e) {
                    Log.e("Utils::checkLocalPort", "ServerSocket could not connect create local socket on " + gameConnection.getAddress().getHostAddress() + ":" + String.valueOf(gameConnection.getPort()) + "\n" + e.toString());
                    status.set(false);
                }
            }
        });
        t.start();
        try {
            t.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }
        return status.get();
    }


    public static class InputDialog extends Dialog {

        private Context mContext;
        private EditText edtInput;
        private Button btnOk;
        private ImageButton btnRandom;
        private TextView txtTitle;

        public InputDialog(Context context) {
            super(context);
            setContentView(R.layout.dialog_name);
            mContext = context;

            btnOk = findViewById(R.id.btnOk);
            btnOk.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    InputDialog.this.dismiss();
                }
            });
            btnRandom = findViewById(R.id.btnRandom);
            btnRandom.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    String[] stringArray = mContext.getResources().getStringArray(R.array.randomNames);
                    edtInput.setText(stringArray[Utils.getRandomInteger(0, stringArray.length)]);
                }
            });
            edtInput = findViewById(R.id.edtInput);
            txtTitle = findViewById(R.id.txtTitle);
        }

        @Override
        public void setTitle(CharSequence title) {
            txtTitle.setText(title);
        }

        public String getInput() {
            return edtInput.getText().toString();
        }

        public void setInput(String input) {
            edtInput.setText(input);
        }

    }

    public static int getRandomInteger(int min, int max) {
        int v = min - 1;
        while (v < min) {
            v = new Random().nextInt(max);
        }
        return v;
    }
}
