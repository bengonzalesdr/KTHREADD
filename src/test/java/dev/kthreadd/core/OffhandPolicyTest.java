package dev.kthreadd.core;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class OffhandPolicyTest {
    @Test void lowHealthAndGlidingPrioritizeTotem() {
        assertEquals(OffhandPolicy.Choice.TOTEM,OffhandPolicy.choose(true,8,20,false,true,true,12,false));
        assertEquals(OffhandPolicy.Choice.TOTEM,OffhandPolicy.choose(true,30,20,true,false,false,12,false));
    }
    @Test void utilityFallsBackToTotemEvenWithoutAutoTotemEnabled() {
        assertEquals(OffhandPolicy.Choice.TOTEM,OffhandPolicy.choose(false,6,20,false,true,false,12,false));
        assertEquals(OffhandPolicy.Choice.TOTEM,OffhandPolicy.choose(false,30,20,false,false,true,12,true));
    }
    @Test void utilityPriorityIsDeterministicAndNoModulesMeansNoAction() {
        assertEquals(OffhandPolicy.Choice.GAP,OffhandPolicy.choose(false,20,20,false,true,true,12,false));
        assertEquals(OffhandPolicy.Choice.CRYSTAL,OffhandPolicy.choose(false,20,20,false,false,true,12,false));
        assertEquals(OffhandPolicy.Choice.NONE,OffhandPolicy.choose(false,1,20,false,false,false,12,false));
    }
}
