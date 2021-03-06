package com.garrapeta.gameoflive;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.MotionEvent;
import android.view.SurfaceHolder;

public class GameOfLifeRenderer {


    private static final int SPACING = 50;
    private SurfaceHolderProvider surfaceHolderProvider;

    private Paint paint = new Paint();
    private boolean visible = true;
    private final Handler handler = new Handler();

    private final Runnable drawRunner;

    private static int renderPeriod;
    private boolean touchEnabled;
    private boolean drawGrid;
    private boolean drawNumbers;

    private final GameOfLifeWorld world;
    private boolean isPlaying = true;
    private Bitmap backgroundBitmap;

    public GameOfLifeRenderer(Context context, SurfaceHolderProvider surfaceHolderProvider) {
        this.surfaceHolderProvider = surfaceHolderProvider;
        setConfiguration(context);
        backgroundBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.skin_tile);
        paint.setAntiAlias(true);
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeWidth(1f);

        world = new GameOfLifeWorld();

        drawRunner = new Runnable() {
            @Override
            public void run() {
                processFrame();
            }
        };

        handler.post(drawRunner);
    }

    public void setConfiguration(Context context) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        touchEnabled = prefs.getBoolean(context.getString(R.string.pref_key_touch), true);
        drawGrid = prefs.getBoolean(context.getString(R.string.pref_key_grid), true);
        drawNumbers = prefs.getBoolean(context.getString(R.string.pref_key_numbers), false);
        renderPeriod = prefs.getInt(context.getString(R.string.pref_key_period), 300);
    }

    public void surfaceCreated(SurfaceHolder surfaceHolder) {

    }


    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        world.createMatrix(width / SPACING, height / SPACING);
    }

    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        this.visible = false;
        handler.removeCallbacks(drawRunner);
    }

    public void onVisibilityChanged(boolean visible) {
        this.visible = visible;
        if (visible) {
            handler.post(drawRunner);
        } else {
            handler.removeCallbacks(drawRunner);
        }
    }

    public void onTouchEvent(MotionEvent event) {
        if (!touchEnabled) {
        }
        if (event.getAction() == MotionEvent.ACTION_UP) {
            world.onCellClicked((int) (event.getX() / SPACING), (int) (event.getY() / SPACING));
            drawWorld();
        }
    }

    public void setPlaying(boolean playing) {
        this.isPlaying = playing;
    }

    public boolean isPlaying() {
        return this.isPlaying;
    }

    private void processFrame() {
        evolveWorld();
        drawWorld();
    }

    private void evolveWorld() {
        if (isPlaying) {
            world.step();
        }
    }

    private void drawWorld() {

        SurfaceHolder holder = surfaceHolderProvider.getSurfaceHolder();
        Canvas canvas = null;
        try {
            canvas = holder.lockCanvas();
            if (canvas != null) {
                drawWorld(canvas);
            }
        } finally {
            if (canvas != null)
                holder.unlockCanvasAndPost(canvas);
        }
        handler.removeCallbacks(drawRunner);
        if (visible) {
            handler.postDelayed(drawRunner, renderPeriod);
        }
    }

    private void drawWorld(Canvas canvas) {
        canvas.drawColor(Color.BLACK);

        drawBackground(canvas);

        if (drawGrid) {
            drawGrid(canvas);
        }
        drawCells(canvas);

        if (drawNumbers) {
            drawNeighbours(canvas);
        }
    }

    private void drawBackground(Canvas canvas) {
        for (int i = 0; i < canvas.getWidth(); i += backgroundBitmap.getWidth()) {
            for (int j = 0; j < canvas.getHeight(); j += backgroundBitmap.getHeight()) {
                canvas.drawBitmap(backgroundBitmap, i, j, paint);
            }
        }
    }

    private void drawGrid(Canvas canvas) {
        paint.setColor(Color.GRAY);

        int gridHeight = world.getRows() * SPACING;
        int gridWidth = world.getCols() * SPACING;

        for (int i = 0; i < world.getCols(); i ++) {
            int startX = i * SPACING;
            canvas.drawLine(startX, 0, startX, gridHeight, paint);
        }

        for (int j = 0; j < world.getRows(); j ++) {
            int startY = j * SPACING;
            canvas.drawLine(0, startY, gridWidth, startY, paint);
        }
    }

    private void drawCells(Canvas canvas) {
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.FILL);

        int cols = world.getCols();
        int rows = world.getRows();

        for (int i = 0; i < cols; i++) {
            for (int j = 0; j < rows; j++) {
                drawCell(canvas, i, j);
            }
        }
    }

    private void drawCell(Canvas canvas, int x, int y) {
        boolean alive = world.isAlive(x, y);
        if (alive) {
            canvas.drawRect(x * SPACING, y * SPACING, (x + 1) * SPACING, (y + 1) * SPACING, paint);
        }
    }

    private void drawNeighbours(Canvas canvas) {
        paint.setColor(Color.WHITE);
        paint.setTextSize(SPACING / 2);

        int cols = world.getCols();
        int rows = world.getRows();

        for (int i = 0; i < cols; i++) {
            for (int j = 0; j < rows; j++) {
                drawNeighbours(canvas, i, j);
            }
        }
    }

    private void drawNeighbours(Canvas canvas, int x, int y) {
        int count = world.getLivingNeighbours(x, y);
        canvas.drawText(String.valueOf(count), x * SPACING, (y + 1) * SPACING, paint);
    }

    public static interface SurfaceHolderProvider {

        public SurfaceHolder getSurfaceHolder();
    }
}
