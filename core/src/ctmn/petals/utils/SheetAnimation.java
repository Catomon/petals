package ctmn.petals.utils;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class SheetAnimation {

    private TextureRegion[] frames;
    private Timer timer;

    private float sheetWidth;
    private float sheetHeight;

    private boolean isCycled;

    private int currentFrame;

    public SheetAnimation() {
    }

    public SheetAnimation(Texture sheets, float sheetWidth, float sheetHeight, float delay, boolean isCycled) {
        set(sheets, sheetWidth, sheetHeight, delay, isCycled);
    }

    public SheetAnimation(Texture sheets, float sheetWidth, float sheetHeight, float delay) {
        set(sheets, sheetWidth, sheetHeight, delay, true);
    }

    public void update(float delta) {
        timer.update(delta);

        if (timer.isDone() && currentFrame < frames.length) { //0
            currentFrame++;

            timer.continueFor();
        }

        if (isCycled && currentFrame == frames.length) //-1
            currentFrame = 0;
    }

    public void set(Texture sheets, float sheetWidth, float sheetHeight, float delay, boolean isCycled) {
        this.sheetWidth = sheetWidth;
        this.sheetHeight = sheetHeight;
        this.isCycled = isCycled;

        int rows = (int) (sheets.getHeight() / sheetHeight);
        int cols = (int) (sheets.getWidth() / sheetWidth);

        frames = new TextureRegion[cols * rows];

        TextureRegion[][] tmp = TextureRegion.split(sheets, sheets.getWidth() / cols, sheets.getHeight() / rows); // #10
        int index = 0;
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                frames[index++] = tmp[i][j];
            }
        }

        timer = new Timer();
        timer.start(delay);
    }

    public void reverseFrames() {
        TextureRegion[] tr = new TextureRegion[frames.length];
        int j = 0;
        for (int i = frames.length-1; i > 0; i--) {
            tr[j] = frames[i];
            j++;
        }
        frames =  tr;
    }

    public void reset() {
        currentFrame = 0;
        timer.continueFor();
    }

    public TextureRegion getCurrentFrame() {
        return frames[currentFrame];
    }

    public boolean isDone() {
        return currentFrame == frames.length - 1 && timer.isDone();
    }

    public TextureRegion[] getFrames() {
        return frames;
    }

    public float getTickTime() {
        return timer.getTimeNeed() * frames.length;
    }

    public float getSheetWidth() {
        return sheetWidth;
    }

    public float getSheetHeight() {
        return sheetHeight;
    }

    public Timer getTimer() {
        return timer;
    }
}
