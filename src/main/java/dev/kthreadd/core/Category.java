package dev.kthreadd.core;
public enum Category {
    COMBAT("Combat"), MOVEMENT("Movement"), PLAYER("Player"), OTHER("Other"), RENDER("Render");
    public final String label;
    Category(String label) { this.label=label; }
}
