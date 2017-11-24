package com.cynetstudios.tictactoe;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.View;
import android.widget.GridLayout;
import android.widget.ImageView;

import java.util.*;

import static com.cynetstudios.tictactoe.Utils.getRandomInteger;

public class TicTacToeView extends GridLayout implements OnTicTacToeClickListener, OnGameFinishedListener {

    private static SparseArray<Drawable> xDrawables = new SparseArray<>();
    private static SparseArray<Drawable> oDrawables = new SparseArray<>();
    private Context mContext;
    private List<ImageView> imageViews = new ArrayList<>();
    private Switcher randomDrawable;

    private ArrayList<OnTicTacToeClickListener> listeners = new ArrayList<>();
    private OnGameFinishedListener onGameFinishedListener;
    private boolean isEnabled = false;
    private int moves = 0;

    public TicTacToeView(Context context, Context mContext) {
        super(context);
        this.mContext = mContext;
        init();
    }

    public TicTacToeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        init();
    }

    public TicTacToeView(Context context, AttributeSet attrs, Context mContext) {
        super(context, attrs);
        this.mContext = mContext;
        init();
    }

    private void init() {
        View v = inflate(mContext, R.layout.tictactoe, null);
        addView(v);
        if (xDrawables.size() == 0 ||
                oDrawables.size() == 0)
            loadImages();
        initGui(v);
        defaultColor();
    }

    private void initGui(View v) {
        ImageView ttt00 = v.findViewById(R.id.ttt00);
        ttt00.setTag(Switcher.NONE);
        ImageView ttt01 = v.findViewById(R.id.ttt01);
        ttt01.setTag(Switcher.NONE);
        ImageView ttt02 = v.findViewById(R.id.ttt02);
        ttt02.setTag(Switcher.NONE);
        ImageView ttt10 = v.findViewById(R.id.ttt10);
        ttt10.setTag(Switcher.NONE);
        ImageView ttt11 = v.findViewById(R.id.ttt11);
        ttt11.setTag(Switcher.NONE);
        ImageView ttt12 = v.findViewById(R.id.ttt12);
        ttt12.setTag(Switcher.NONE);
        ImageView ttt20 = v.findViewById(R.id.ttt20);
        ttt20.setTag(Switcher.NONE);
        ImageView ttt21 = v.findViewById(R.id.ttt21);
        ttt21.setTag(Switcher.NONE);
        ImageView ttt22 = v.findViewById(R.id.ttt22);
        ttt22.setTag(Switcher.NONE);

        imageViews.add(ttt00);
        imageViews.add(ttt01);
        imageViews.add(ttt02);
        imageViews.add(ttt10);
        imageViews.add(ttt11);
        imageViews.add(ttt12);
        imageViews.add(ttt20);
        imageViews.add(ttt21);
        imageViews.add(ttt22);

        for (ImageView view : imageViews) {
            view.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    cellClicked((ImageView) view);
                }
            });
        }
    }

    private void loadImages() {
        xDrawables.append(0, ContextCompat.getDrawable(mContext, R.drawable.stroke_x_1));
        xDrawables.append(1, ContextCompat.getDrawable(mContext, R.drawable.stroke_x_2));
        oDrawables.append(0, ContextCompat.getDrawable(mContext, R.drawable.stroke_o_1));
        oDrawables.append(1, ContextCompat.getDrawable(mContext, R.drawable.stroke_o_2));
    }

    //--------------------------------------------------------------

    //tictactoe cell clicked callback
    @Override
    public void cellClicked(ImageView view) {
        if (!isEnabled)
            return;
        for (OnTicTacToeClickListener listener : listeners) {
            listener.cellClicked(view);
        }
    }

    @Override
    public void gameFinished(Game.GameResult gameResult) {
        // TODO: 2017/09/28 check if game is gameFinished, determine if & which switch won, send switch
        onGameFinishedListener.gameFinished(gameResult);
    }

    public void doMove(Move move) {
        doMove(imageViews.get(move.getIndex()), move.getSwitcher());
    }

    public void doMove(ImageView view, Switcher switcher) {
        view.setTag(switcher);
        moves++;
        switch (switcher) {
            case CROSSES: {
                view.setImageDrawable(xDrawables.get(getRandomInteger(0, 2)));
                break;
            }
            case NOUGHTS: {
                view.setImageDrawable(oDrawables.get(getRandomInteger(0, 2)));
                break;
            }
        }
        invalidate();
    }

    //method for manaully initialting clicks
    public void clicked(ImageView view, Switcher switcher) {
        doMove(view, switcher);
    }

    public void clicked(int index, Switcher switcher) {
        clicked(imageViews.get(index), switcher);
    }

    public void checkBoardState() {

        Switcher result = Switcher.NONE;

        if (moves == 9)
            result = Switcher.DRAW;

        boolean xCol0 = checkMatch(0, 3, 6, Switcher.CROSSES),
                xCol1 = checkMatch(1, 4, 7, Switcher.CROSSES),
                xCol2 = checkMatch(2, 5, 8, Switcher.CROSSES),
                xRow0 = checkMatch(0, 1, 2, Switcher.CROSSES),
                xRow1 = checkMatch(3, 4, 5, Switcher.CROSSES),
                xRow2 = checkMatch(6, 7, 8, Switcher.CROSSES),
                xDiagTL = checkMatch(0, 4, 8, Switcher.CROSSES),
                xDiagTR = checkMatch(2, 4, 6, Switcher.CROSSES),

                oCol0 = checkMatch(0, 3, 6, Switcher.NOUGHTS),
                oCol1 = checkMatch(1, 4, 7, Switcher.NOUGHTS),
                oCol2 = checkMatch(2, 5, 8, Switcher.NOUGHTS),
                oRow0 = checkMatch(0, 1, 2, Switcher.NOUGHTS),
                oRow1 = checkMatch(3, 4, 5, Switcher.NOUGHTS),
                oRow2 = checkMatch(6, 7, 8, Switcher.NOUGHTS),
                oDiagTL = checkMatch(0, 4, 8, Switcher.NOUGHTS),
                oDiagTR = checkMatch(2, 4, 6, Switcher.NOUGHTS);

        if (xCol0 || xCol1 || xCol2 || xRow0 || xRow1 || xRow2 || xDiagTL || xDiagTR)
            result = Switcher.CROSSES;

        if (oCol0 || oCol1 || oCol2 || oRow0 || oRow1 || oRow2 || oDiagTL || oDiagTR)
            result = Switcher.NOUGHTS;



        if (result != Switcher.NONE)
            if (onGameFinishedListener != null)
                onGameFinishedListener.gameFinished(new Game.GameResult(null, null, -1, result));
    }

    private boolean checkMatch(int i1, int i2, int i3, Switcher switcher) {
        boolean i1Tag = imageViews.get(i1).getTag().equals(switcher),
                i2Tag = imageViews.get(i2).getTag().equals(switcher),
                i3Tag = imageViews.get(i3).getTag().equals(switcher);
        boolean bResult = (i1Tag && i2Tag && i3Tag);
        if (bResult)
            highlight(i1, i2, i3);
        invalidate();
        return bResult;
    }

    public void clearBoard() {
        for (ImageView v : imageViews) {
            v.setTag(Switcher.NONE);
            v.setImageDrawable(null);
            v.setBackgroundColor(ContextCompat.getColor(mContext, R.color.colorWhite));
        }
        moves = 0;
    }

    public void gameDraw(){
        for (ImageView v : imageViews) {
            v.setBackgroundColor(ContextCompat.getColor(mContext, R.color.colorLightRed));
        }
    }

    public void move(Move move) {
        clicked(imageViews.get(move.getIndex()), move.getSwitcher());
    }

    private void highlight(int... i) {
        for (Integer ii : i) {
            ImageView imageView = imageViews.get(ii);
            imageView.setBackgroundColor(ContextCompat.getColor(mContext, R.color.colorMatch));
        }
    }

    //--------------------------------------------------------------

    public void setOnGameFinishedListener(OnGameFinishedListener onGameFinishedListener) {
        this.onGameFinishedListener = onGameFinishedListener;
    }

    public void setOnTicTacToeClicked(OnTicTacToeClickListener listener) {
        listeners.add(listener);
    }


    public Integer getIndex(ImageView view) {
        return imageViews.indexOf(view);
    }

    public void defaultColor(){
        for (int i = 0; i < imageViews.size(); i++) {
            ImageView v = imageViews.get(i);
            v.setBackgroundColor(ContextCompat.getColor(mContext, (i % 2 == 0) ? R.color.colorPrimaryDark: R.color.colorGrey));
        }
    }

    public void enableGrid(boolean enable) {
        isEnabled = enable;
        for (ImageView v : imageViews) {
            v.setBackgroundColor(ContextCompat.getColor(mContext, (enable) ? R.color.colorWhite : R.color.colorLightGrey));
        }
    }

    public boolean isGridEnabled() {
        return isEnabled;
    }
}
