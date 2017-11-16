package de.unibi.cebitec.bibigrid.model;

import javax.annotation.Nonnull;

public enum IntentMode {
    VERSION("V", "version"),
    HELP("h", "help"),
    TERMINATE("t", "terminate"),
    CREATE("c", "create"),
    LIST("l", "list"),
    VALIDATE("ch", "check");

    private final String shortParam;
    private final String longParam;

    IntentMode(String shortParam, String longParam) {
        this.shortParam = shortParam;
        this.longParam = longParam;
    }

    public String getShortParam() {
        return shortParam;
    }

    public String getLongParam() {
        return longParam;
    }

    @Nonnull
    public static IntentMode fromString(String value) {
        for (IntentMode mode : values()) {
            if (mode.shortParam.equals(value) || mode.longParam.equals(value)) {
                return mode;
            }
        }
        return HELP;
    }
}
