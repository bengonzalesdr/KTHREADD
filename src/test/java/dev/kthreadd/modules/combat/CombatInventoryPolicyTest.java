package dev.kthreadd.modules.combat;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CombatInventoryPolicyTest {
    @Test void clusteredThreatsReserveTwoHitsButNotAnUnboundedSum() {
        assertEquals(14, CombatInventoryPolicy.crystalThreat(new double[]{7, 2, 7, 3}));
        assertEquals(8, CombatInventoryPolicy.crystalThreat(new double[]{8}));
        assertEquals(0, CombatInventoryPolicy.crystalThreat(new double[]{}));
    }
    @Test void missingUtilityNeverStripsAnExistingTotem() {
        assertEquals(CombatInventoryPolicy.ItemChoice.KEEP, CombatInventoryPolicy.fallback(
            CombatInventoryPolicy.ItemChoice.GAP, false, true, false, false));
        assertEquals(CombatInventoryPolicy.ItemChoice.TOTEM, CombatInventoryPolicy.fallback(
            CombatInventoryPolicy.ItemChoice.CRYSTAL, false, false, true, true));
        assertEquals(CombatInventoryPolicy.ItemChoice.KEEP, CombatInventoryPolicy.fallback(
            CombatInventoryPolicy.ItemChoice.TOTEM, false, false, false, true));
    }
    @Test void fullStacksCanReceiveOnlyMatchingItems() {
        assertTrue(CombatInventoryPolicy.stackRoom(true, 48, 64, 64));
        assertFalse(CombatInventoryPolicy.stackRoom(false, 48, 64, 64));
        assertFalse(CombatInventoryPolicy.stackRoom(true, 64, 64, 64));
        assertFalse(CombatInventoryPolicy.stackRoom(true, 1, 64, 1));
    }
    @Test void bowWaitsForConfiguredChargeAndUsableShot() {
        assertFalse(CombatInventoryPolicy.releaseBow(3, 20, true));
        assertFalse(CombatInventoryPolicy.releaseBow(19, 20, true));
        assertTrue(CombatInventoryPolicy.releaseBow(20, 20, true));
        assertFalse(CombatInventoryPolicy.releaseBow(20, 20, false));
    }
    @Test void wornArmorGetsDurabilityPenaltyBeforeItBreaks() {
        assertTrue(CombatInventoryPolicy.durabilityPenalty(.04) > CombatInventoryPolicy.durabilityPenalty(.5));
        assertEquals(0, CombatInventoryPolicy.durabilityPenalty(.5));
    }
    @Test void weaknessRequiresSufficientAttackDamageOrStrongerStrength() {
        assertFalse(CombatInventoryPolicy.canAttack(0, -1, 4));
        assertTrue(CombatInventoryPolicy.canAttack(0, -1, 5));
        assertTrue(CombatInventoryPolicy.canAttack(0, 1, 1));
        assertFalse(CombatInventoryPolicy.canAttack(1, 1, 1));
        assertTrue(CombatInventoryPolicy.canAttack(-1, -1, 1));
        assertFalse(CombatInventoryPolicy.canAttack(0, 0, 1));
        assertTrue(CombatInventoryPolicy.canAttack(0, 0, 2));
    }
}
