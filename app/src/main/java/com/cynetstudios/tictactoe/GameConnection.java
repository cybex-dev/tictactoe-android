package com.cynetstudios.tictactoe;

import java.io.Serializable;
import java.net.InetAddress;

public class GameConnection implements Serializable {

    private InetAddress address;
    private int port;

    public GameConnection(InetAddress ipAddr, int port) {
        this.address = ipAddr;
        this.port = port;
    }

    public InetAddress getAddress() {
        return address;
    }

    public void setAddress(InetAddress address) {
        this.address = address;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}
