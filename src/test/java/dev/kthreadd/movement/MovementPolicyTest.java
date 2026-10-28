package dev.kthreadd.movement;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class MovementPolicyTest {
 @Test void flightAndGlideHaveExplicitPriority() {
  assertEquals("Flight", MovementPolicy.owner(true,true,true,true,true,true,false));
  assertEquals("ElytraFly", MovementPolicy.owner(false,true,true,true,true,true,false));
  assertEquals("FastIce", MovementPolicy.owner(false,false,true,true,true,true,true));
  assertEquals("Speed", MovementPolicy.owner(false,false,true,true,true,true,false));
  assertEquals("Strafe", MovementPolicy.owner(false,false,true,true,true,false,false));
 }
 @Test void idleAndUnsupportedModesDoNotClaimMotion() {
  assertEquals("", MovementPolicy.owner(false,false,true,false,false,false,false));
  assertEquals("", MovementPolicy.owner(false,false,false,true,false,true,false));
 }
 @Test void diagonalInputHasSameSpeedAsForwardAndTinyInputStops() {
  var straight=MovementPolicy.horizontal(1,0,0,0.5);
  var diagonal=MovementPolicy.horizontal(1,1,0,0.5);
  assertEquals(0.5,Math.hypot(straight[0],straight[1]),1e-9);
  assertEquals(0.5,Math.hypot(diagonal[0],diagonal[1]),1e-9);
  assertArrayEquals(new double[]{0,0},MovementPolicy.horizontal(0,0,123,0.5));
 }
 @Test void respawnIsOneRequestPerDeathAndResetsOnRecovery() {
  var delay=new MovementPolicy.RespawnDelay();
  assertFalse(delay.tick(true,3)); assertFalse(delay.tick(true,3)); assertTrue(delay.tick(true,3));
  assertFalse(delay.tick(true,3)); assertFalse(delay.tick(false,3));
  assertFalse(delay.tick(true,3)); assertFalse(delay.tick(true,3)); assertTrue(delay.tick(true,3));
 }
 @Test void voidRescueRequiresLivingUnriddenNonSpectatorFallingPlayer() {
  assertTrue(MovementPolicy.rescue(true,false,false,false,-63,-64,-1,2));
  assertFalse(MovementPolicy.rescue(true,true,false,false,-63,-64,-1,2));
  assertFalse(MovementPolicy.rescue(false,false,false,false,-63,-64,-1,2));
  assertFalse(MovementPolicy.rescue(true,false,true,false,-63,-64,-1,2));
  assertFalse(MovementPolicy.rescue(true,false,false,true,-63,-64,-1,2));
  assertFalse(MovementPolicy.rescue(true,false,false,false,0,-64,-1,2));
 }
}
