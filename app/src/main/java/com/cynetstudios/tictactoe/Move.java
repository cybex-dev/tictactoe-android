package com.cynetstudios.tictactoe;

import java.io.Serializable;

public class Move implements Serializable{
    private Switcher switcher;
    private int index;

    public Move(Switcher switcher, int index) {
        this.switcher = switcher;
        this.index = index;
    }

    public Switcher getSwitcher() {
        return switcher;
    }

    public int getIndex() {
        return index;
    }
}
