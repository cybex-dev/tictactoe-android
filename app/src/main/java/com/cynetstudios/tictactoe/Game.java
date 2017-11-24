package com.cynetstudios.tictactoe;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.*;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

public class Game extends Activity implements OnGameFinishedListener, OnPlayerTurnListener {

    public static String GameResultInfo = "GameResult",
            NewGame = "NewGame",
            NewGameRequest = "NewGameRequest",
            Request = "?";
    private AtomicBoolean bResultsOpen = new AtomicBoolean(false),
            bLocalResponded = new AtomicBoolean(false),
            bLocalResponse = new AtomicBoolean(false);

    public static class GameResult implements Serializable {
        private int rounds;
        private GameInfo.PlayerInfo.Player winner, loser;
        private Switcher switcherResult = Switcher.NONE;

        public GameResult(GameInfo.PlayerInfo.Player winner, GameInfo.PlayerInfo.Player loser, int rounds, Switcher switcherResult) {
            this.winner = winner;
            this.loser = loser;
            this.rounds = rounds;
            this.switcherResult = switcherResult;
        }

        public GameInfo.PlayerInfo.Player getWinner() {
            return winner;
        }

        public GameInfo.PlayerInfo.Player getLoser() {
            return loser;
        }

        public int getRounds() {
            return rounds;
        }

        public boolean isDraw() {
            return switcherResult == Switcher.DRAW;
        }

        public Switcher getSwitcherResult() {
            return switcherResult;
        }

        public void setSwitcherResult(Switcher switcherResult) {
            this.switcherResult = switcherResult;
        }
    }

    private Vibrator vibratorManager;
    private Context mContext;
    private GameInfo gameInfo;
    private GameConnection gameConnection;
    private TicTacToeView ticTacToeView;
    private Thread thdNetwork, thdSocketMonitor, thdClientRequest, thdLocalRequest;
    ;
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private ObjectInputStream clientInputStream;
    private ObjectOutputStream clientOutputStream;
    private String playerName;
    private boolean bRunGame = false,
            gameFinished = false,
            isHost = false,
            bRunNetworking = true;

    private TextView playerLocal, playerRemote, playerLocalScore, playerRemoteScore, lblServerAddress, lblRound;
    private ImageView playerLocalSymbol, playerRemoteSymbol;

    private OnPlayerTurnListener turnListener;
    private OnNewGameRequestListener newGameRequestListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        enableStrictMode();
        initGameData();
        initGui();
        startNetworkingThread();
        newGame();
        if (isHost)
            gameInfo.nextRound();
        ticTacToeView.defaultColor();
        updateGui(gameInfo);
    }

    private void enableStrictMode() {

        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                .detectDiskReads()
                .detectDiskWrites()
                .detectNetwork()   // or .detectAll() for all detectable problems
                .penaltyLog()
                .build());
        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                .detectLeakedSqlLiteObjects()
                .detectLeakedClosableObjects()
                .penaltyLog()
                .penaltyDeath()
                .build());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isHost) {
            Toast.makeText(mContext, "Waiting for opponent to join...", Toast.LENGTH_SHORT).show();
        }
    }

    private void startClientSocketMonitor(final Socket clientSocket) {
        thdSocketMonitor = new Thread(new Runnable() {
            @Override
            public void run() {
                while (clientSocket.isConnected() &&
                        !clientSocket.isClosed()) {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        Log.e("thdConnectionMonitor", "Thread was interrupted");
                        if (!isFinishing())
                            exitGame();
                    }
                }
                if (bRunGame || !gameFinished) {
                    try {
                        if (isHost)
                            displayServerConnectionLostErrorandExit();
                        else
                            displayClientConnectionLostErrorandExit();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

            }
        });
        thdSocketMonitor.start();
    }

    private void startNetworkingThread() {
        final AtomicBoolean bClientResponded = new AtomicBoolean(false),
                bClientResponse = new AtomicBoolean(false);

        thdNetwork = new Thread(new Runnable() {
            private Socket cSocket;
            private ObjectInputStream serverCommInputStream;
            private ObjectOutputStream serverCommOutputStream;

            @Override
            public void run() {
                if (isHost) {
                    openServerSocket();
                    setOnGameFinishedListener(new OnGameFinishedListener() {
                        @Override
                        public void gameFinished(GameResult gameResult) {
                            try {
                                gameFinished = true;
                                gameInfo.determineWinner(gameResult.switcherResult);
                                GameResult result = new GameResult(gameInfo.getWinner(), gameInfo.getLoser(), gameInfo.getRound(), gameResult.getSwitcherResult());
                                notifyClientGameState(gameInfo);

                                // new game request listener callback
                                newGameRequestListener = new OnNewGameRequestListener() {
                                    @Override
                                    public void newGameRequest() {
                                        final boolean localResponse = bLocalResponse.get(),
                                                clientResponse = bClientResponse.get();

                                        if (bLocalResponded.get() && bClientResponded.get()) {

                                            if (localResponse && clientResponse) {
                                                bClientResponded.set(false);
                                                bClientResponse.set(false);
                                                bLocalResponded.set(false);
                                                bLocalResponse.set(false);
                                                try {
                                                    gameInfo.getPlayerInfo().switchSymbols();
                                                    gameInfo.nextRound();
                                                    notifyClientGameState(gameInfo);

                                                    runOnUiThread(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            resetBoard();
                                                        }
                                                    });
                                                    notifyClientNewGame();
                                                    gameFinished = false;
                                                    boolean b = (!isHost == (gameInfo.getRound() % 2 == 0));
                                                    notifyClientTurn(b ? TurnType.WAIT : TurnType.GO);
                                                    displayTurnMessage(b ? TurnType.GO : TurnType.WAIT, true);
                                                } catch (IOException e) {
                                                    e.printStackTrace();
                                                    displayClientConnectionLostErrorandExit();
                                                }
                                            } else {
                                                exitGame();
                                            }
                                        } else
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    String name = (localResponse)
                                                            ? gameInfo.getPlayerInfo().getPlayerRemote().getName()
                                                            : gameInfo.getPlayerInfo().getPlayerLocal().getName();
                                                    Toast.makeText(mContext, "Waiting for " + name + "\'s response", Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                    }
                                };

                                //thread global values created,
                                // b{device}Response, b{device}Responded
                                showResultsAndRequestAnotherGame(result);
                                notifyResultsAndRequestAnother(result);

                            } catch (IOException e) {
                                e.printStackTrace();
                                displayClientConnectionLostErrorandExit();
                            }

                        }
                    });

                    // CODE FOR SERVER MOVE
                    ticTacToeView.setOnTicTacToeClicked(new OnTicTacToeClickListener() {
                        @Override
                        public void cellClicked(final ImageView view) {
                            if (ticTacToeView.isGridEnabled()) {
                                if (view.getTag() == Switcher.NONE) {
                                    try {
                                        final Switcher s = gameInfo.getPlayerInfo().getPlayerLocal().getSwitcher();

                                        notifyClientOfMove(new Move(s, ticTacToeView.getIndex(view)));
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                ticTacToeView.clicked(view, s);
                                                ticTacToeView.checkBoardState();
                                            }
                                        });

                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                ticTacToeView.enableGrid(false);
                                                setPlayerColors(false);
                                                updateGui(gameInfo);
                                            }
                                        });
                                        try {
                                            Thread.sleep(50);
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                        if (!gameFinished) {
                                            notifyClientGameState(gameInfo);
                                            notifyClientTurn(TurnType.GO);
                                            displayTurnMessage(TurnType.WAIT, false);
                                        }
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                        displayClientConnectionLostErrorandExit();
                                    }
                                } else {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(mContext, "Invalid Move!", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                }
                            } else {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(mContext, "Wait your turn!", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }

                        }
                    });

                    processServerCommConnections();

                } else {
                    openClientSocket();
                    startClientSocketMonitor(clientSocket);
                    handleServerData();
                }
            }

            private void showResultsAndRequestAnotherGame(final GameResult result) {

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Game.this.gameFinished(result);
                        bResultsOpen.set(false);
                        requestAnotherGame();
                        try {
                            if (bLocalResponse.get() == Boolean.FALSE) {
                                serverCommInputStream.close();
                                serverCommOutputStream.close();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        newGameRequestListener.newGameRequest();
                    }
                }).start();
            }

            // client related network methods
            private void openClientSocket() {
                try {
                    if (clientSocket == null)
                        clientSocket = new Socket(gameConnection.getAddress(), gameConnection.getPort());
                    else
                        Log.e("Game::openClientSocket", "Attempted to opena  socket that was already open");
                } catch (IOException e) {
                    e.printStackTrace();
                    new AlertDialog.Builder(mContext)
                            .setTitle("Connection Error")
                            .setMessage("Unable to connect to server.")
                            .setNegativeButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    dialogInterface.dismiss();
                                    setResult(1, null);
                                    finish();
                                }
                            }).create().show();
                }
            }

            private void greetServer() throws IOException, ClassNotFoundException {
                Object o = clientInputStream.readObject();
                if (o instanceof String)
                    if ((o).equals(Game.Request))
                        clientOutputStream.writeObject(GameConnector.PlayerName.concat(":").concat(playerName));
            }

            private void handleServerData() {
                ticTacToeView.setOnTicTacToeClicked(new OnTicTacToeClickListener() {
                    @Override
                    public void cellClicked(final ImageView view) {
                        if (ticTacToeView.isGridEnabled()) {
                            if (view.getTag() == Switcher.NONE) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        ticTacToeView.enableGrid(false);
                                        setPlayerColors(false);

                                    }
                                });
                                notifyServerOfMove(new Move(null, ticTacToeView.getIndex(view)));
                            } else {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(mContext, "Invalid Move!", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        } else {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(mContext, "Wait your turn!", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }
                });
                while (bRunNetworking) {
                    try {
                        assignClientStreams();

                        greetServer();

                        startClientGame();
                        //get stream, 1) send player name, 2) handle gamedata (score, round), 3) send move location, 4) send y/n response to new game
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e("Game::handleServerData", "Unable to communicate with server");
                        bRunNetworking = false;
                        if (bLocalResponded.get() == Boolean.FALSE) {
                            displayClientQuitAndExit();
                        } else displayServerConnectionLostErrorandExit();
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                        Log.e("Game::handleServerData", "Invalid data received from server");
                        bRunNetworking = false;
                        displayServerInvalidDataErrorandExit();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        Log.e("Game::handleServerData", "Unable to sleep thread checking the game board");
                        bRunNetworking = false;
                        displayServerConnectionLostErrorandExit();
                    }
                }
                Log.e("Game::handleServerDat", "bRunNetworking set false, stopping network connection(s)");
            }

            private void startClientGame() throws IOException, ClassNotFoundException, InterruptedException {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ticTacToeView.enableGrid(false);
                        setPlayerColors(false);
                    }
                });
                boolean isStart = true;
                bRunGame = true;

                /*
                CLIENT INSTANCE CODE
                 */

                while (bRunGame) {
                    Object o = clientInputStream.readObject();
                    if (o instanceof Move) {
                        Log.i("Client", "Server data received: Move");
                        //server move
                        // TODO: 2017/09/28 this 0 goes first is problematic
                        final Move move = (Move) o;
                        if (!gameInfo.getPlayerInfo().getPlayerRemote().getSwitcher().equals(move.getSwitcher()))
                            vibrate(Vibrate.SHORT);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Log.i("Server::Moved", "Server Move Command received");
                                ticTacToeView.doMove(move);
                                Log.i("Server::Moved", "Checking client board state");
                                ticTacToeView.checkBoardState(); //highlights matches only, does not return any data
                                Log.i("Server::Moved", "Client board state check");
                            }
                        });
                        Thread.sleep(50);
                        isStart = false;
                    } else if (o instanceof TurnType) {
                        displayTurnMessage((TurnType) o, isStart);
                        isStart = false;
                        if (o == TurnType.GO)
                            turnListener.playerTurn();
                    } else if (o instanceof String) {
                        Log.i("Client", "Server data received: String");
                        //client move
                        String msg = (String) o;
                        if (msg.equals(NewGameRequest)) {
                            Log.i("Client", "New Game Request Message received");
                            requestAnotherGame();
                            clientOutputStream.writeObject(bLocalResponse.get());
                            clientOutputStream.flush();
                            if (!bLocalResponse.get()) {
                                gameFinished = true;
                                bRunNetworking = false;
                                bRunGame = false;
                                try {
                                    clientInputStream.close();
                                    clientOutputStream.close();
                                    exitGame();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        } else if (msg.equals(Game.NewGame)) {
                            Log.i("Client", "Server New Game Command Received");
                            isStart = true;
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    resetBoard();
                                }
                            });
                        } else if (msg.equals(Game.Request)) {
                            Log.i("Client", "Server Request Message Received");
                            clientOutputStream.writeObject(Game.NewGame);
                            clientOutputStream.flush();
                        }
                    } else if (o instanceof GameInfo) {
                        Log.i("Client", "Server data received: GameInfo");
                        //game info update - assigns to client variable for notification messages
                        gameInfo = (GameInfo) o;
                        updateGui(gameInfo);
                    } else if (o instanceof GameResult) {
                        Log.i("Client", "Server data received: GameResult");
                        //game has gameFinished
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                ticTacToeView.enableGrid(false);
                            }
                        });
                        gameFinished((GameResult) o);
                    }
                }
            }


            // server related network methods

            private void openServerSocket() {
                try {
                    serverSocket = new ServerSocket(gameConnection.getPort(), 1, gameConnection.getAddress());
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e("Game::openServerSocket", "Unable to start server socket.");
                    showAlertDialogAndExit("Socket Error", "Unable to create and host game", 1);
                }
            }

            private void assignServerCommStreams() {
                try {
                    serverCommInputStream = new ObjectInputStream(cSocket.getInputStream());
                    serverCommOutputStream = new ObjectOutputStream(cSocket.getOutputStream());
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e("Game::assignServerComm", "Unable to read/write from server accepted socket");
                    closeServerCommSocket();
                    processServerCommConnections();
                }
            }

            private void closeServerCommSocket() {
                try {
                    cSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(mContext, "Unable to close client socket", Toast.LENGTH_SHORT).show();
                }
            }

            private void assignClientStreams() {
                try {
                    clientOutputStream = new ObjectOutputStream(clientSocket.getOutputStream());
                    clientInputStream = new ObjectInputStream(clientSocket.getInputStream());
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e("Game::assignClientStrea", "Unable to read/write from socket.");
                    showAlertDialogAndExit("Connection Error", "Unable to communicate with client", 1);
                }
            }

            private void processServerCommConnections() {

                try {
                    cSocket = serverSocket.accept();

                    assignServerCommStreams();

                    // TODO: 2017/09/28 check the while condition variable
                    while (bRunNetworking) {
                        try {
                            /*
                            SERVER INSTANCE CODE
                             */

                            //only for new first game requests
                            if (!bRunGame) {
                                serverCommOutputStream.writeObject(Game.Request);
                                serverCommOutputStream.flush();
                            }
                            Object o = serverCommInputStream.readObject();

                            if (o instanceof String) {
                                String s = (String) o;
                                if (s.equals(GameConnector.Test)) {
                                    Log.e("ClientMessage", "Received \'" + s + "\' from " + cSocket.getInetAddress().getHostAddress() + " : " + String.valueOf(cSocket.getPort()));
                                    if (!cSocket.isClosed()) {
                                        cSocket.close();
                                        cSocket = null;
                                        processServerCommConnections();
                                    }
                                } else if (s.contains(GameConnector.PlayerName)) {
                                    returnGreeting(s);
                                    updateGui(gameInfo);
                                    handleNewGameRequest();
                                    startClientSocketMonitor(cSocket);
                                    bRunGame = true;
                                } else if (s.equals(Game.NewGame)) {
                                    Log.e("Game::newGameRequest", "CLient requests a new game");
                                }
                            } else if (o instanceof Move) {
                                Switcher s = gameInfo.getPlayerInfo().getPlayerRemote().getSwitcher();
                                final Move m = new Move(s, ((Move) o).getIndex());

                                notifyClientOfMove(m);
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        ticTacToeView.clicked(m.getIndex(), m.getSwitcher());
                                        ticTacToeView.checkBoardState();
                                    }
                                });
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        ticTacToeView.enableGrid(true);
                                        setPlayerColors(true);
                                        updateGui(gameInfo);
                                    }
                                });
                                try {
                                    Thread.sleep(50);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                if (!gameFinished) {
                                    notifyClientGameState(gameInfo);
                                    vibrate(Vibrate.SHORT);
                                    notifyClientTurn(TurnType.WAIT);
                                    displayTurnMessage(TurnType.GO, false);
                                }
                            } else if (o instanceof Boolean) {
                                bClientResponse.set((Boolean) o);
                                bClientResponded.set(true);
                                if (!bClientResponse.get()) {
                                    bRunNetworking = false;
                                    gameFinished = true;
                                }
                                newGameRequestListener.newGameRequest();
                            }


                        } catch (ClassNotFoundException e) {
                            e.printStackTrace();
                            Log.e("Game::processServerComm", "Unable to read object from client");
                        } catch (IOException e) {
                            e.printStackTrace();
                            Log.e("Game::notifyResultsNewG", "Unable to send game results to client, client has quit");
                            bRunNetworking = false;
                            gameFinished = true;
                            if (bClientResponded.get() == Boolean.TRUE) {
                                if (bClientResponse.get() == Boolean.FALSE)
                                    displayClientQuitAndExit();
                            } else
                                displayClientConnectionLostErrorandExit();

                        }
                    }
                    Log.e("Game::processServerCo", "bRunNetworking set false, stopping network connection(s)");
//                    exitGame();
                    //get stream, 1) check for test message, 2) send gamedata, 3) handle move using point(x,y) , 4) recieve player name, 5) receive response for new game


                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e("Game::processClientConn", "A IO Error occurred");
                    if (cSocket != null) {
                        try {
                            cSocket.close();
                            cSocket = null;
                        } catch (IOException e1) {
                            e1.printStackTrace();
                            Log.e("Game::processClientConn", "Unable to close cSocket");
                            showAlertDialogAndExit("Connection Error", "Unable to communicate with client", 1);
                        }
                    }
                }
            }

            private void notifyClientNewGame() throws IOException {
                try {
                    serverCommOutputStream.writeObject(NewGame);
                    serverCommOutputStream.reset();
                    serverCommOutputStream.flush();
                } catch (IOException e) {
                    Log.e("Game::notifyClientGameS", "Unable to notify client of new game state");
                    e.printStackTrace();
                    throw e;
                }
            }

            private void notifyClientGameState(GameInfo gameInfo) throws IOException {
                GameInfo g = (gameInfo == null) ? Game.this.gameInfo : gameInfo;
                try {
                    serverCommOutputStream.reset();
                    serverCommOutputStream.writeObject(g);
                    serverCommOutputStream.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e("Game::notifyClientGameS", "Unable to notify client of new game state");
                    throw e;
                }
            }

            private void notifyClientOfMove(Move move) throws IOException {
                try {
                    serverCommOutputStream.writeObject(move);
                    serverCommOutputStream.reset();
                    serverCommOutputStream.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e("Game::notifyClientOfMov", "Unable to notify client of move");
                    throw e;
                }
            }

            private void returnGreeting(String s) throws IOException {
                String[] split = s.split(":");
                gameInfo.getPlayerInfo().getPlayerRemote().setName(split[1]);

                try {
                    serverCommOutputStream.writeObject(gameInfo);
                    serverCommOutputStream.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e("Game::returnGreet", "Unable to send data to client");
                    throw e;
                }
            }

            private void handleNewGameRequest() throws IOException {
                final boolean isServerTurn = (gameInfo.getRound() % 2 == 1);
                final TurnType clientTurnType = isServerTurn ? TurnType.WAIT : TurnType.GO;
                notifyClientTurn(clientTurnType);
                displayTurnMessage((isServerTurn) ? TurnType.GO : TurnType.WAIT, true);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ticTacToeView.enableGrid((clientTurnType == TurnType.WAIT));
                        setPlayerColors(isServerTurn);
                    }
                });
                gameInfo.getPlayerInfo().getPlayerLocal().setSwitcher(Switcher.NOUGHTS);
                gameInfo.getPlayerInfo().getPlayerRemote().setSwitcher(Switcher.CROSSES);
                vibrate(Vibrate.SHORT);
            }

            private void notifyClientTurn(TurnType type) throws IOException {
                try {
                    serverCommOutputStream.writeObject(type);
                    serverCommOutputStream.reset();
                    serverCommOutputStream.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e("Game::notifyClientTurn", "Unable to notify client of their turn");
                    throw e;
                }
            }

            //notify client of results
            private void notifyResultsAndRequestAnother(final GameResult gameResult) throws IOException {
                serverCommOutputStream.writeObject(gameResult);
                serverCommOutputStream.flush();
                serverCommOutputStream.writeObject(NewGameRequest);
                serverCommOutputStream.flush();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(mContext, "waiting for client response...", Toast.LENGTH_LONG).show();
                    }
                });
            }

        });
        thdNetwork.start();
    }

    private void resetBoard() {
        newGame();
        updateGui(gameInfo);
        boolean b = gameInfo.getRound() % 2 == 0;
        ticTacToeView.enableGrid(!isHost == b);
    }

    private void exitGame() {
        Intent intent = new Intent();
        intent.putExtra(Game.GameResultInfo, gameInfo);
        setResult(0, intent);
        finish();
    }

    private void setPlayerColors(boolean localPlayerTurn) {
        playerLocal.setBackgroundColor(localPlayerTurn ? ContextCompat.getColor(mContext, R.color.colorLightGrey) : Color.TRANSPARENT);
        playerLocalScore.setBackgroundColor(localPlayerTurn ? ContextCompat.getColor(mContext, R.color.colorLightGrey) : Color.TRANSPARENT);
        playerLocalSymbol.setBackgroundColor(localPlayerTurn ? ContextCompat.getColor(mContext, R.color.colorLightGrey) : Color.TRANSPARENT);
        playerRemote.setBackgroundColor(!localPlayerTurn ? ContextCompat.getColor(mContext, R.color.colorLightGrey) : Color.TRANSPARENT);
        playerRemoteScore.setBackgroundColor(!localPlayerTurn ? ContextCompat.getColor(mContext, R.color.colorLightGrey) : Color.TRANSPARENT);
        playerRemoteSymbol.setBackgroundColor(!localPlayerTurn ? ContextCompat.getColor(mContext, R.color.colorLightGrey) : Color.TRANSPARENT);
    }

    private void displayClientInvalidDataErrorandExit() {
        showAlertDialogAndExit("Invalid Data", "Invalid data was received from client, exiting...", 1);
    }

    private void displayServerInvalidDataErrorandExit() {
        showAlertDialogAndExit("Invalid Data", "Invalid data was received from client, exiting...", 1);
    }

    private void displayClientQuitAndExit() {
        showAlertDialogAndExit("Opponent Quit", "Your opponent has left the game, exiting...", 1);
    }

    private void displayClientConnectionLostErrorandExit() {
        showAlertDialogAndExit("Connection Error", "Unable to communicate with client, exiting...", 1);
    }

    private void displayServerConnectionLostErrorandExit() {
        showAlertDialogAndExit("Connection Error", "Unable to communicate with server, exiting...", 1);
    }

    private void displayTurnMessage(TurnType turnType, boolean isNewGame) {
        final String name = (isHost)
                ? (turnType == TurnType.WAIT)
                ? gameInfo.getPlayerInfo().getPlayerRemote().getName()
                : gameInfo.getPlayerInfo().getPlayerLocal().getName()
                : (turnType == TurnType.WAIT)
                ? gameInfo.getPlayerInfo().getPlayerLocal().getName() // local = server
                : gameInfo.getPlayerInfo().getPlayerRemote().getName(); // remote = client

        final String s = name + ((isNewGame) ? " goes first" : "\'s turn");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(mContext, s, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void notifyServerOfMove(Move move) {
        try {
            clientOutputStream.writeObject(move);
            clientOutputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
            showAlertDialogAndExit("Network Error", "Unable to send game data to server", 1);
            displayServerConnectionLostErrorandExit();
        }
    }

    private void updateGui(final GameInfo gameInfo) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (gameInfo != null) {
                    playerLocal.setText((isHost)
                            ? gameInfo.getPlayerInfo().getPlayerLocal().getName()
                            : gameInfo.getPlayerInfo().getPlayerRemote().getName());
                    playerRemote.setText((!isHost)
                            ? gameInfo.getPlayerInfo().getPlayerLocal().getName()
                            : gameInfo.getPlayerInfo().getPlayerRemote().getName());

                    playerLocalScore.setText(String.valueOf((isHost
                            ? gameInfo.getPlayerInfo().getPlayerLocal().getScore()
                            : gameInfo.getPlayerInfo().getPlayerRemote().getScore())));
                    playerRemoteScore.setText(String.valueOf((!isHost
                            ? gameInfo.getPlayerInfo().getPlayerLocal().getScore()
                            : gameInfo.getPlayerInfo().getPlayerRemote().getScore())));

                    lblRound.setText(mContext.getResources().getString(R.string.round).concat(" #").concat(String.valueOf(gameInfo.getRound())));

                    boolean isServerNoughts = gameInfo.getRound() % 2 == 1;
                    if (isHost) {
                        playerLocalSymbol.setImageDrawable(ContextCompat.getDrawable(mContext, (isServerNoughts)
                                ? R.drawable.stroke_o_1
                                : R.drawable.stroke_x_1));
                        playerRemoteSymbol.setImageDrawable(ContextCompat.getDrawable(mContext, (!isServerNoughts)
                                ? R.drawable.stroke_o_1
                                : R.drawable.stroke_x_1));
                    } else {
                        playerLocalSymbol.setImageDrawable(ContextCompat.getDrawable(mContext, (!isServerNoughts)
                                ? R.drawable.stroke_o_1
                                : R.drawable.stroke_x_1));
                        playerRemoteSymbol.setImageDrawable(ContextCompat.getDrawable(mContext, (isServerNoughts)
                                ? R.drawable.stroke_o_1
                                : R.drawable.stroke_x_1));
                    }
                } else {
                    playerLocal.setText(playerName);
                    playerRemote.setText("Loading...");
                    playerLocalScore.setText("N/A");
                    playerRemoteScore.setText("N/A");
                    playerLocalSymbol.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.stroke_question));
                    playerRemoteSymbol.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.stroke_question));
                    lblRound.setText(mContext.getResources().getString(R.string.round).concat(" not started"));
                }

            }
        });
    }

    private void closeClientSocket() {
        try {
            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(mContext, "Unable to close client socket", Toast.LENGTH_SHORT).show();
        }
    }

    private void closeServerSocket() {
        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(mContext, "Error closing server socket", Toast.LENGTH_LONG).show();
        }
    }

    private void showAlertDialogAndExit(final String title, final String msg, final int resultCode) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AlertDialog.Builder builder = new AlertDialog.Builder(mContext)
                        .setTitle(title)
                        .setMessage(msg)
                        .setNegativeButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                                setResult(resultCode, null);
                                finish();
                            }
                        });
                builder.create().show();
            }
        });
    }

    private void initGameData() {
        Intent intent = getIntent();
        if (intent.hasExtra(MainActivity.isHost)) {
            isHost = intent.getBooleanExtra(MainActivity.isHost, false);
            if (intent.hasExtra(GameConnector.GameConnect)) {
                gameConnection = (GameConnection) intent.getSerializableExtra(GameConnector.GameConnect);
            }
            if (intent.hasExtra(GameConnector.PlayerName)) {
                playerName = intent.getStringExtra(GameConnector.PlayerName);
            }

        } else {
            setResult(1, null);
            finish();
        }
        if (isHost) {
            gameInfo = new GameInfo(isHost);
            gameInfo.setHostTurn(isHost);
            gameInfo.getPlayerInfo().getPlayerLocal().setName(playerName);
        }
    }

    private void initGui() {
        setContentView(R.layout.activity_game);
        ticTacToeView = findViewById(R.id.tttGame);

        playerLocal = findViewById(R.id.playerLocalName);
        playerLocalScore = findViewById(R.id.playerLocalScore);
        playerLocalSymbol = findViewById(R.id.playerLocalSymbol);

        playerRemote = findViewById(R.id.playerRemoteName);
        playerRemoteScore = findViewById(R.id.playerRemoteScore);
        playerRemoteSymbol = findViewById(R.id.playerRemoteSymbol);

        lblServerAddress = findViewById(R.id.lblServerAdress);
        lblServerAddress.setText(gameConnection.getAddress().getHostAddress().concat(" : ").concat(String.valueOf(gameConnection.getPort())));

        lblRound = findViewById(R.id.lblRound);

        if (!isHost)
            turnListener = this;

        vibratorManager = (Vibrator) mContext.getSystemService(VIBRATOR_SERVICE);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ticTacToeView.defaultColor();
            }
        });

    }

    // Activity method finish
    @Override
    public void finish() {
        if (thdSocketMonitor != null)
            if (thdSocketMonitor.isAlive()) {
                bRunGame = false;
                gameFinished = true;
            }
        if (thdNetwork != null)
            if (thdNetwork.isAlive())
                bRunNetworking = false;

        if (thdSocketMonitor != null)
            if (thdSocketMonitor.isAlive())
                thdSocketMonitor.interrupt();
        if (thdNetwork != null)
            if (thdNetwork.isAlive())
                thdNetwork.interrupt();

        if (clientSocket != null) {
            if (!clientSocket.isClosed())
                closeClientSocket();
        }
        if (serverSocket != null) {
            if (!serverSocket.isClosed())
                closeServerSocket();
        }
        super.finish();
    }

    // OnTicTacToeView gameFinishedNotification (Server only)
    @Override
    public void gameFinished(GameResult gameResult) {
        bResultsOpen.set(true);
        if (gameResult.isDraw())
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ticTacToeView.gameDraw();
                }
            });
        vibrate(Vibrate.LONG);
        showResults(gameResult);
    }

    //show results locally
    private void showResults(final GameResult gameResult) {
        bResultsOpen.set(true);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                new AlertDialog.Builder(mContext)
                        .setTitle("Game Results")
                        .setMessage(((gameResult.isDraw()) ? "A draw!\n\n" : gameResult.getWinner().getName() + " is the winner\n\n") +
                                "Scores:\n---------\n\n" +
                                gameResult.getWinner().getName() + "\'s score: " + String.valueOf(gameResult.getWinner().getScore()) + " points (Wins = " + String.valueOf(Math.round(gameResult.getWinner().getScore() * 1.0 / gameResult.getRounds() * 100)) + "%)\n" +
                                gameResult.getLoser().getName() + "\'s score: " + String.valueOf(gameResult.getLoser().getScore()) + " points (Wins = " + String.valueOf(Math.round(gameResult.getLoser().getScore() * 1.0 / gameResult.getRounds() * 100)) + "%)\n" +
                                "# of Draws = " + String.valueOf(gameResult.getRounds() - (gameResult.getWinner().getScore() + gameResult.getLoser().getScore())))
                        .setNeutralButton("Ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                                bResultsOpen.set(false);
                            }
                        }).create().show();
            }
        });
        while (bResultsOpen.get() == Boolean.TRUE) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void requestAnotherGame() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AlertDialog.Builder requestDialog = new AlertDialog.Builder(mContext)
                        .setTitle("Tic Tag Toe")
                        .setMessage("Another Game?")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                                bLocalResponse.set(true);
                                bLocalResponded.set(true);
                            }
                        })
                        .setNegativeButton("No", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                                bRunGame = false;
                                gameFinished = true;
                                bLocalResponded.set(true);
                            }
                        });
                requestDialog.create().show();
            }
        });
        while (bLocalResponded.get() == Boolean.FALSE) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void newGame() {
        Log.i("Game", "New Game Starting");
        gameFinished = false;
        ticTacToeView.clearBoard();
    }

    public void setOnGameFinishedListener(OnGameFinishedListener listener) {
        ticTacToeView.setOnGameFinishedListener(listener);
    }

    //client only notification
    @Override
    public void playerTurn() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ticTacToeView.enableGrid(true);
                setPlayerColors(true);
                vibrate(Vibrate.SHORT);
            }
        });
    }

    private void vibrate(final Vibrate vibrate) {
        final int time = (vibrate == Vibrate.SHORT) ? 50 : 200;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (Build.VERSION.SDK_INT >= 26) {
                    vibratorManager.vibrate(VibrationEffect.createOneShot(time, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibratorManager.vibrate(time);
                }
            }
        });
    }
}
