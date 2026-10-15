package com.example.aicompanion.pig;

import java.util.Arrays;
import java.util.List;

/** Режими поведінки прирученої свині. */
public enum PigMode {
    FOLLOW("follow", "Іде за тобою"),
    STAY("stay", "Чекає на місці"),
    GUARD("guard", "Охороняє тебе"),
    FORAGE("forage", "Шукає їжу і приносить тобі"),
    WANDER("wander", "Гуляє сама");

    private final String id;
    private final String title;

    PigMode(String id, String title) {
        this.id = id;
        this.title = title;
    }

    public String id() {
        return id;
    }

    public String title() {
        return title;
    }

    public PigMode next() {
        PigMode[] all = values();
        return all[(ordinal() + 1) % all.length];
    }

    public static PigMode byId(String id) {
        for (PigMode mode : values()) {
            if (mode.id.equals(id)) return mode;
        }
        return null;
    }

    public static List<String> ids() {
        return Arrays.stream(values()).map(PigMode::id).toList();
    }
}
