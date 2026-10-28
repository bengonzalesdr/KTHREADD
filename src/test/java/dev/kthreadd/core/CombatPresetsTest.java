package dev.kthreadd.core;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class CombatPresetsTest {
    @Test void invalidPresetIsAtomicAndDoesNotEnableModule(){
        var module=new ClientModule("test",Category.COMBAT,"");module.number("a",2,0,5,1);
        assertThrows(IllegalArgumentException.class,()->CombatPresets.apply(List.of(module),List.of(new CombatPresets.Change("test","a",4),new CombatPresets.Change("missing","a",4))));
        assertEquals(2,module.value("a"));assertFalse(module.enabled());
    }
    @Test void presetsRetainCoreSafetyProtections(){
        for(String name:List.of("balanced","aggressive","defensive")){
            var changes=CombatPresets.changes(name);
            for(String protection:List.of("AntiSuicide","ProtectFriends","ReservePendingDamage"))assertTrue(changes.stream().anyMatch(c->c.setting().equals(protection)&&c.value()==1));
        }
    }
    @Test void unknownPresetDoesNotSilentlyFallBack(){assertThrows(IllegalArgumentException.class,()->CombatPresets.changes("anything"));}
}
