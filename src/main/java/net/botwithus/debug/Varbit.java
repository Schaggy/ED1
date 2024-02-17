package net.botwithus.debug;

import net.botwithus.rs3.game.js5.types.vars.VarDomainType;
import net.botwithus.rs3.game.vars.VarManager;

import java.time.LocalTime;

public class Varbit {
    private final int id;
    private int value;
    private boolean hidden;

    private LocalTime lastUpdated;

    private final VarDomainType domain;

    public Varbit(int id, int value, boolean hidden) {
        this.id = id;
        this.value = value;
        this.hidden = hidden;
        this.lastUpdated = LocalTime.now();
        this.domain = VarManager.getVarDomain(id);
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

    public VarDomainType getDomain() {
        return domain;
    }

    public LocalTime getLastUpdated() {
        return lastUpdated;
    }
}
