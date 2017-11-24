package com.cynetstudios.tictactoe;

import java.io.Serializable;

public class GameInfo implements Serializable {
    public class PlayerInfo implements Serializable {
        public class Player implements Serializable {
            private int score = 0;
            private String name = "Waiting...";
            private Switcher switcher = Switcher.DRAW;

            Player() {
                score = 0;
            }

            Player(String name, Switcher switcher) {
                score = 0;
                this.name = name;
                this.switcher = switcher;
            }

            public void setSwitcher(Switcher switcher){
                this.switcher = switcher;
            }

            public Switcher getSwitcher() {
                return switcher;
            }

            public void switchSwitcher(){
                switcher = (switcher == Switcher.NOUGHTS)
                        ? Switcher.CROSSES
                        : Switcher.NOUGHTS;
            }

            public String getName() {
                return name;
            }

            public void setName(String name) {
                this.name = name;
            }

            public void won() {
                score++;
            }

            public int getScore() {
                return score;
            }
        }

        private Player playerLocal, playerRemote;

        public PlayerInfo() {
            playerLocal = new Player();
            playerRemote = new Player();
        }

        public Player getPlayerLocal() {
            return playerLocal;
        }

        public Player getPlayerRemote() {
            return playerRemote;
        }

        public int getNumberofDraws() {
            return getRound() - (playerLocal.getScore() + playerRemote.getScore());
        }

        public void playerLocalWon(){
            playerLocal.won();
        }

        public void playerRemoteWon(){
            playerRemote.won();
        }

        public void switchSymbols() {
            playerRemote.switchSwitcher();
            playerLocal.switchSwitcher();
        }
    }

    private PlayerInfo playerInfo;
    private int round = 0;
    private boolean localMoveTurn;
    private boolean isHosting = false;

    public PlayerInfo.Player getLoser() {
        return (playerInfo.playerRemote.score < playerInfo.playerLocal.score)
                ? playerInfo.playerRemote
                : playerInfo.playerLocal;
    }

    public PlayerInfo.Player getWinner() {
        return (playerInfo.playerRemote.score >= playerInfo.playerLocal.score)
                ? playerInfo.playerRemote
                : playerInfo.playerLocal;
    }


    public GameInfo(boolean isHost) {
        isHosting = isHost;
        round = 0;
        playerInfo = new PlayerInfo();
    }

    public PlayerInfo getPlayerInfo() {
        return playerInfo;
    }

    public void setPlayerInfo(PlayerInfo playerInfo) {
        this.playerInfo = playerInfo;
    }

    public int getRound() {
        return round;
    }

    public void nextRound() {
        round++;
    }

    public boolean isHostTurn() {
        return localMoveTurn;
    }

    public void setHostTurn(boolean hostTurn) {
        this.localMoveTurn = hostTurn;
    }

    public void determineWinner(Switcher switcherResult) {
        if (switcherResult != Switcher.DRAW &&
                switcherResult != Switcher.NONE)
            if (playerInfo.playerLocal.switcher == switcherResult)
                playerInfo.playerLocalWon();
            else
                playerInfo.playerRemoteWon();
    }
}
