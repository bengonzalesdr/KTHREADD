package dev.kthreadd.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CrystalPolicyTest {
    @Test void placementExpiryMustNotShortenAttackRetry() {
        var pending=new CrystalTracker<String>(); pending.attacked(4,100);
        pending.expire(700,500);
        assertFalse(pending.canAttack(4,700,2000));
        assertTrue(pending.canAttack(4,2100,2000));
    }
    @Test void delayedConfirmationDoesNotProduceDuplicatePlacement() {
        for(long latency:new long[]{0,75,200,500}) {
            var pending=new CrystalTracker<String>(); pending.placed("base",100);
            for(long tick=100;tick<100+latency;tick+=50) assertTrue(pending.blocked("base",tick,750));
            assertTrue(pending.spawned("base",9,100+latency));
            assertEquals(latency,pending.lastConfirmationMs());
            assertTrue(pending.blocked("base",2000,750));
            pending.removed(9); assertFalse(pending.blocked("base",2001,750));
        }
    }
    @Test void lethalDamageWinsWithoutRewardingOverkill() {
        assertTrue(CrystalPolicy.score(8, 1, 7, .75, true) > CrystalPolicy.score(15, 1, 20, .75, true));
        assertEquals(CrystalPolicy.score(8, 1, 7, .75, true), CrystalPolicy.score(30, 1, 7, .75, true));
    }
    @Test void selfDamageBreaksEqualDamageTie() {
        assertTrue(CrystalPolicy.score(8, 1, 20, 1, true) > CrystalPolicy.score(8, 4, 20, 1, true));
    }
    @Test void safetyIncludesPreviouslyCommittedDamage() {
        assertFalse(CrystalPolicy.safe(3, 2, 4, 20, .5, true));
        assertFalse(CrystalPolicy.safe(3, 2, 8, 5.5, .5, true));
        assertTrue(CrystalPolicy.safe(3, 0, 4, 20, .5, true));
        assertFalse(CrystalPolicy.safe(Double.NaN, 0, 4, 20, .5, true));
    }
    @Test void confirmedRemovalReleasesPositionBeforeTimeout() {
        var pending = new CrystalTracker<String>();
        pending.placed("base", 100);
        assertTrue(pending.blocked("base", 150, 500));
        assertTrue(pending.spawned("base", 42, 160));
        assertTrue(pending.blocked("base", 1000, 500));
        pending.removed(42);
        assertFalse(pending.blocked("base", 1001, 500));
        assertEquals(1, pending.confirmed());
    }
    @Test void unconfirmedAttemptsExpireAndLateSpawnsAreNotClaimed() {
        var pending = new CrystalTracker<String>();
        pending.placed("base", 0);
        pending.expire(501, 500);
        assertFalse(pending.blocked("base", 501, 500));
        assertFalse(pending.spawned("base", 42, 510));
        assertEquals(1, pending.expired());
    }
    @Test void attackIsSuppressedUntilTimeoutOrRemoval() {
        var pending = new CrystalTracker<String>();
        pending.attacked(4, 100);
        assertFalse(pending.canAttack(4, 200, 300));
        assertTrue(pending.canAttack(4, 400, 300));
        pending.removed(4);
        assertTrue(pending.canAttack(4, 201, 300));
        pending.clear();
        assertEquals(0, pending.confirmed());
    }
}
