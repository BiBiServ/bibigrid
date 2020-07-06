package de.unibi.cebitec.bibigrid.core.model;

public enum IntentMode {
    VERSION("V", "version","Display version"),
    HELP("h", "help","Display help message"),
    TERMINATE("t", "terminate","Terminate cluster"),
    SCALE_UP("su", "scale-up","Scale up a running cluster"),
    SCALE_DOWN("sd", "scale-down"," Scale down a running cluster"),
    CREATE("c", "create", "Create cluster"),
    LIST("l", "list"," List all started/running cluster"),
    VALIDATE("ch", "check", "Validate cluster configuration"),
    IDE("ide", "ide", "Establish a secured connection to specified ide");

    private final String shortParam;
    private final String longParam;
    private final String description;

    IntentMode(String shortParam, String longParam, String description) {
        this.shortParam = shortParam;
        this.longParam = longParam;
        this.description = description;
    }

    public String getShortParam() {
        return shortParam;
    }

    public String getLongParam() {
        return longParam;
    }

    public String getDescription() { return description; }

    public static IntentMode fromString(String value) {
        for (IntentMode mode : values()) {
            if (mode.shortParam.equals(value) || mode.longParam.equals(value)) {
                return mode;
            }
        }
        return HELP;
    }
}
