package dev.helikon.client.hud;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientTpsEstimateTest {
    @Test
    void smoothsObservedTickCadenceWithoutExceedingTwenty() {
        ClientTpsEstimate estimate = new ClientTpsEstimate();

        estimate.observeTick(1_000_000_000L);
        estimate.observeTick(1_100_000_000L);

        assertTrue(estimate.tps() < 20.0D);
        assertTrue(estimate.tps() > 0.0D);
        estimate.observeTick(1_150_000_000L);
        assertTrue(estimate.tps() <= 20.0D);
    }

    @Test
    void resetRestoresTheSafeDefault() {
        ClientTpsEstimate estimate = new ClientTpsEstimate();
        estimate.observeTick(0L);
        estimate.observeTick(200_000_000L);

        estimate.reset();

        assertEquals(20.0D, estimate.tps());
    }
}
