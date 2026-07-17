package dev.helikon.client.event;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventBusTest {
    @Test
    void dispatchesTypedEventsSupportsUnsubscribeAndIsolatesListenerFailures() {
        List<String> reportedFailures = new ArrayList<>();
        EventBus bus = new EventBus((event, exception) -> reportedFailures.add(event.getClass().getSimpleName()));
        AtomicInteger worldEvents = new AtomicInteger();
        Subscription subscription = bus.subscribe(WorldEvent.class, event -> worldEvents.incrementAndGet());
        bus.subscribe(WorldEvent.class, event -> {
            throw new IllegalStateException("expected");
        });

        bus.post(new WorldEvent(WorldEvent.Phase.JOIN, "example.test"));
        assertEquals(1, worldEvents.get());
        assertEquals(List.of("WorldEvent"), reportedFailures);
        assertEquals(2, bus.subscriberCount());

        subscription.unsubscribe();
        bus.post(new WorldEvent(WorldEvent.Phase.LEAVE, "example.test"));
        assertEquals(1, worldEvents.get());
        assertEquals(1, bus.subscriberCount());
    }

    @Test
    void eventCatalogRepresentsEveryRequiredInternalBoundary() {
        assertEquals(2, WorldEvent.Phase.values().length);
        assertEquals(2, PlayerLifecycleEvent.Phase.values().length);
        assertEquals(2, ScreenEvent.Phase.values().length);
        assertEquals(3, InputEvent.Kind.values().length);
        assertEquals(4, RenderEvent.Kind.values().length);
        assertEquals(2, PlayerUpdateEvent.Kind.values().length);
        assertEquals(4, InteractionEvent.Kind.values().length);
        assertEquals(2, ChatEvent.Direction.values().length);
        assertEquals(2, PacketObservationEvent.Direction.values().length);
        assertEquals(2, ChunkEvent.Phase.values().length);
        assertEquals(2, ResourceReloadEvent.Phase.values().length);

        EventBus bus = new EventBus((event, exception) -> { });
        AtomicInteger received = new AtomicInteger();
        bus.subscribe(ChatEvent.class, event -> received.incrementAndGet());
        bus.post(new ChatEvent(ChatEvent.Direction.SEND, "hello", false));
        assertEquals(1, received.get());
    }

    @Test
    void normalizedEventsRejectMalformedPayloads() {
        assertThrows(IllegalArgumentException.class, () -> new ScreenEvent(ScreenEvent.Phase.OPEN, " "));
        assertThrows(IllegalArgumentException.class, () -> new InputEvent(InputEvent.Kind.MOUSE_SCROLL,
                InputEvent.Action.PRESS, 0, 0, 0, 0.0, 1.0));
        assertThrows(IllegalArgumentException.class, () -> new RenderEvent(RenderEvent.Kind.HUD, Double.NaN, ""));
        assertThrows(IllegalArgumentException.class, () -> new InventoryUpdateEvent(-1));
        assertThrows(IllegalArgumentException.class, () -> new PacketObservationEvent(
                PacketObservationEvent.Direction.RECEIVE, ""));

        InputEvent scroll = new InputEvent(InputEvent.Kind.MOUSE_SCROLL, InputEvent.Action.SCROLL,
                0, 0, 0, -1.5, 2.0);
        assertTrue(scroll.scrollY() > 0.0);
        assertFalse(scroll.action() == InputEvent.Action.PRESS);
    }

    @Test
    void screenTrackerClosesAndOpensWhenTheSameScreenClassIsReplaced() {
        ScreenTransitionTracker tracker = new ScreenTransitionTracker();
        Object first = new Object();
        Object replacement = new Object();

        assertEquals(List.of(new ScreenEvent(ScreenEvent.Phase.OPEN, "example.Screen")),
                tracker.update(first, "example.Screen"));
        assertEquals(List.of(), tracker.update(first, "example.Screen"));
        assertEquals(List.of(
                new ScreenEvent(ScreenEvent.Phase.CLOSE, "example.Screen"),
                new ScreenEvent(ScreenEvent.Phase.OPEN, "example.Screen")
        ), tracker.update(replacement, "example.Screen"));
        assertEquals(List.of(new ScreenEvent(ScreenEvent.Phase.CLOSE, "example.Screen")),
                tracker.update(null, ""));
    }
}
