package ctmn.petals.utils;

public class Timer {

    private float timeNeed, timePassed;

    private boolean isDone, isStart, isCycled;

    public Timer(float timeNeed) {
        start(timeNeed);
    }

    public Timer() {
        this(0);
    }

    public void update(float delta) {
        if (isStart) {
            if (isCycled && isDone) {
                isDone = false;
                timePassed -= timeNeed;
            }

            timePassed += delta;
            if (timePassed >= timeNeed)
                isDone = true;
        }
    }

    public void continueFor() {
        timePassed = 0;
        isDone = false;
        isStart = true;
    }

    public void start(float time) {
        timeNeed = time;
        timePassed = 0;
        isDone = false;
        isStart = true;
    }

    public void continueFor(float timeNeed) {
        this.timeNeed = timeNeed;
        isDone = false;
        isStart = true;
    }

    public void pause() {
        isStart = false;
    }

    public void resume() {
        isStart = true;
    }

    public void stop() {
        isDone = true;
        isStart = false;
        timePassed = 0;
    }

    public void setCycled(boolean cycled) {
        isCycled = cycled;
    }

    public boolean isDone() {
        return isDone;
    }

    public void setDone(boolean done) {
        isDone = done;
    }

    public float getTimeNeed() {
        return timeNeed;
    }

    public void setTimeNeed(float timeNeed) {
        this.timeNeed = timeNeed;
    }

    public float getTimePassed() {
        return timePassed;
    }

    public void setTimePassed(float timePassed) {
        this.timePassed = timePassed;
    }
}
