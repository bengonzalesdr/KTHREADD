package dev.kthreadd.render;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class VisualPolicyTest {
 @Test void cursorCoversCubeWithoutOverflowAndWrapsAtBoundary() {
  var scan=new ScanCursor(); scan.configure(1); var seen=new java.util.HashSet<String>();
  for(int i=0;i<27;i++) seen.add(java.util.Arrays.toString(scan.next()));
  assertEquals(27,seen.size()); assertTrue(seen.contains("[-1, -1, -1]")); assertTrue(seen.contains("[1, 1, 1]"));
  assertArrayEquals(new int[]{-1,-1,-1},scan.next());
  scan.configure(0); assertArrayEquals(new int[]{0,0,0},scan.next());
 }
 @Test void resetAfterRangeChangeDoesNotContinueOldVolume() {
  var scan=new ScanCursor(); scan.configure(3); scan.next(); scan.configure(1);
  assertArrayEquals(new int[]{-1,-1,-1},scan.next());
 }
 @Test void nearestHighwayReportsSignedCorrectionAndDiagonal() {
  var road=VisualPolicy.highway(100,104); assertEquals("X=Z",road.axis());
  assertEquals(2.828,road.offset(),0.001); assertEquals(-2,road.dx(),0.001); assertEquals(2,road.dz(),0.001);
  assertEquals("Z=0",VisualPolicy.highway(120,1).axis());
 }
 @Test void healthFractionAccountsForMaxHealthAbsorptionAndInvalidInputs() {
  assertEquals(0.5,VisualPolicy.healthFraction(20,40,0));
  assertEquals(1,VisualPolicy.healthFraction(20,20,8));
  assertEquals(0,VisualPolicy.healthFraction(Float.NaN,20,0));
 }
}
