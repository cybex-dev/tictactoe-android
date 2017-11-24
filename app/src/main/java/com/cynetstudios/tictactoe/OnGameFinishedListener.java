package com.cynetstudios.tictactoe;

import com.cynetstudios.tictactoe.Game.GameResult;

public interface OnGameFinishedListener {
    void gameFinished(GameResult gameResult);
}
