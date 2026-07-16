package dev.helikon.client.event;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Small explicit event bus. Subscriptions are keyed by concrete event type;
 * no reflection or listener discovery occurs in hot paths.
 */
public final class EventBus {
    private final Map<Class<? extends ClientEvent>, CopyOnWriteArrayList<Consumer<? extends ClientEvent>>> listeners =
            new ConcurrentHashMap<>();
    private final BiConsumer<ClientEvent, RuntimeException> errorHandler;

    public EventBus(BiConsumer<ClientEvent, RuntimeException> errorHandler) {
        this.errorHandler = Objects.requireNonNull(errorHandler, "errorHandler");
    }

    public <T extends ClientEvent> Subscription subscribe(Class<T> eventType, Consumer<T> listener) {
        Objects.requireNonNull(eventType, "eventType");
        Objects.requireNonNull(listener, "listener");

        CopyOnWriteArrayList<Consumer<? extends ClientEvent>> eventListeners =
                listeners.computeIfAbsent(eventType, unused -> new CopyOnWriteArrayList<>());
        eventListeners.add(listener);
        return () -> eventListeners.remove(listener);
    }

    public <T extends ClientEvent> void post(T event) {
        Objects.requireNonNull(event, "event");
        List<Consumer<? extends ClientEvent>> eventListeners = listeners.get(event.getClass());
        if (eventListeners == null) {
            return;
        }

        for (Consumer<? extends ClientEvent> rawListener : new ArrayList<>(eventListeners)) {
            dispatch(rawListener, event);
        }
    }

    public int subscriberCount() {
        return listeners.values().stream().mapToInt(List::size).sum();
    }

    @SuppressWarnings("unchecked")
    private <T extends ClientEvent> void dispatch(Consumer<? extends ClientEvent> rawListener, T event) {
        try {
            ((Consumer<T>) rawListener).accept(event);
        } catch (RuntimeException exception) {
            errorHandler.accept(event, exception);
        }
    }
}
