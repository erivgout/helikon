package dev.helikon.client.module.miscellaneous;

/** The two local lifecycle moments for which Helikon retains a coordinate snapshot. */
public enum CoordinateKind {
    DEATH("Death"),
    LOGOUT("Logout");

    private final String displayName;

    CoordinateKind(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
