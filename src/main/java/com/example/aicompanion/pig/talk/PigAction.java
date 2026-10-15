package com.example.aicompanion.pig.talk;

/** Що свиня може зробити у відповідь на слова гравця. */
public enum PigAction {
    NONE("none"),
    FOLLOW("follow"),
    STAY("stay"),
    GUARD("guard"),
    FORAGE("forage"),
    WANDER("wander"),
    COME("come"),
    JUMP("jump"),
    BEFRIEND("befriend"),
    LEAVE("leave");

    private final String id;

    PigAction(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static PigAction byId(String id) {
        for (PigAction action : values()) {
            if (action.id.equalsIgnoreCase(id)) return action;
        }
        return NONE;
    }
}
