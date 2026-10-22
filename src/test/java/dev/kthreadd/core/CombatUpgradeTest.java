package dev.kthreadd.core;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class CombatUpgradeTest {
    @Test void reservationSurvivesSpawnAndReleasesOnRemoval() {
        var tracker=new CrystalTracker<String>();tracker.placed("base",100);
        assertTrue(tracker.reservedBases().contains("base"));
        tracker.spawned("base",8,140);tracker.expire(900,500);
        assertTrue(tracker.reservedBases().contains("base"));
        tracker.removed(8);assertTrue(tracker.reservedBases().isEmpty());
    }
    @Test void rejectedPlacementDoesNotReserveDamage() {
        var tracker=new CrystalTracker<String>();tracker.placed("base",0);tracker.cancelPlacement("base");
        assertTrue(tracker.reservedBases().isEmpty());
    }
    @Test void recorderIsBoundedAndCountsDroppedHistory() {
        var journal=new CombatJournal(3);
        for(int i=0;i<5;i++)journal.add(i,"skip","cooldown",i);
        assertEquals(3,journal.events().size());assertEquals(2,journal.dropped());
        assertEquals(5,journal.counts().get("skip:cooldown"));
        assertEquals(2,journal.events().get(0).timeMs());
        journal.clear();assertTrue(journal.events().isEmpty());assertEquals(0,journal.dropped());
    }
    @Test void urgencyOnlyInterruptsOffenseForRealProtectionNeeds() {
        assertEquals("CrystalAura",CombatPriority.first(false,false));
        assertEquals("AutoArmor",CombatPriority.first(true,false));
        assertEquals("AutoArmor",CombatPriority.first(false,true));
    }
}
