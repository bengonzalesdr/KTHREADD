package dev.kthreadd.core;
import dev.kthreadd.modules.combat.CombatActions;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class CombatActionsTest {
    @Test void inventoryActionExcludesCrystalAndMeleeUntilNextTick() {
        CombatActions.beginTick(); assertTrue(CombatActions.claim("Offhand"));
        assertFalse(CombatActions.claim("CrystalAura")); assertFalse(CombatActions.available("KillAura"));
        CombatActions.beginTick(); assertTrue(CombatActions.claim("CrystalAura"));
        assertTrue(CombatActions.claim("CrystalAura")); assertFalse(CombatActions.claim("AutoArmor"));
        CombatActions.beginTick();
    }
}
