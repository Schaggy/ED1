package net.botwithus.debug;

import java.time.LocalTime;

public final class Varp {
    private final int id;
    private int value;
    private boolean hidden;

    private LocalTime lastUpdated;

    public Varp(int id, int value, boolean hidden) {
        this.id = id;
        this.value = value;
        this.hidden = hidden;
        this.lastUpdated = LocalTime.now();
    }

    public int getId() {
        return id;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
        this.lastUpdated = LocalTime.now();
    }

    public boolean isHidden() {
        return hidden;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    public LocalTime getLastUpdated() {
        return lastUpdated;
    }
}
