package dev.kthreadd.core;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class CombatEngineUpgradeTest {
    @Test void rejectedPositionsBackOffAndSuccessfulConfirmationRecovers() {
        var feedback=new PlacementFeedback<String>(8);
        feedback.failed("a",100);assertFalse(feedback.ready("a",150));
        feedback.failed("a",200);assertFalse(feedback.ready("a",350));
        feedback.confirmed("a",180);assertTrue(feedback.ready("a",351));
        assertEquals(225,feedback.suggestedTimeout(50));
    }
    @Test void feedbackStateIsBoundedAndDoesNotAffectOtherPositions() {
        var feedback=new PlacementFeedback<Integer>(4);
        for(int i=0;i<50;i++)feedback.failed(i,100);
        assertTrue(feedback.size()<=4);assertTrue(feedback.ready(999,101));
    }
    @Test void motionForecastRejectsTeleportsAndReducesDirectionChangeConfidence() {
        var history=new MotionHistory();
        history.sample(1,0,0,0);history.sample(2,.2,0,0);history.sample(3,.4,0,0);
        var forward=history.predict(1);assertTrue(forward.x()>0 && forward.x()<=.3);
        history.sample(4,.2,0,0);assertTrue(Math.abs(history.predict(1).x())<forward.x());
        history.sample(5,100,0,0);assertEquals(0,history.predict(2).lengthSquared());
    }
    @Test void budgetStopsNewWorkButAllowsCachedDecisionsToFinish() {
        var budget=new DecisionBudget(2,1_000_000_000L,100);
        assertTrue(budget.acquire(100));assertTrue(budget.acquire(101));assertFalse(budget.acquire(102));
        assertEquals(2,budget.used());assertTrue(budget.exhausted(102));
    }
    @Test void delayedFeedbackUsesActualConfirmationHistoryWithinBounds() {
        var feedback=new PlacementFeedback<Integer>(4);
        feedback.confirmed(1,1200);assertEquals(1500,feedback.suggestedTimeout(50));
        feedback.confirmed(1,0);assertTrue(feedback.suggestedTimeout(50)>=50);
    }
}
