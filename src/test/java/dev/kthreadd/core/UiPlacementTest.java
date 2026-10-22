package dev.kthreadd.core;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class UiPlacementTest {
    @Test void headersCannotBePersistedBehindSearchOrFooterAfterResizing() {
        var top=UiPlacement.header(-20,-50,640,360,120,19,62,332);
        assertEquals(0,top.x()); assertEquals(62,top.y());
        var bottom=UiPlacement.header(900,900,640,360,120,19,62,332);
        assertEquals(520,bottom.x()); assertEquals(313,bottom.y());
    }
    @Test void tinyViewportAndNonFiniteInputsStillGiveReachableFiniteOrigin() {
        var p=UiPlacement.header(Double.NaN,Double.POSITIVE_INFINITY,80,90,120,19,62,72);
        assertTrue(Double.isFinite(p.x()) && Double.isFinite(p.y())); assertEquals(0,p.x()); assertTrue(p.y()>=0 && p.y()+19<=90);
    }
}
