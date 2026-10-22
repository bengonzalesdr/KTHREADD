package dev.kthreadd.core;
public final class UiPlacement {
    private UiPlacement() {}
    public record Point(double x,double y) {}
    public static Point header(double x,double y,double width,double height,double panelWidth,double headerHeight,double top,double bottom) {
        double minY=Math.max(0,Math.min(top,height-headerHeight));
        double maxY=Math.max(minY,Math.min(height-headerHeight,bottom-headerHeight));
        return new Point(Math.max(0,Math.min(Math.max(0,width-panelWidth),Double.isFinite(x)?x:0)),Math.max(minY,Math.min(maxY,Double.isFinite(y)?y:minY)));
    }
}
