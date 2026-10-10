package me.collective.event;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class EventBus {
    public static final EventBus INSTANCE = new EventBus();

    public static class Listener {
        public final Object target;
        public final Method method;
        public final int priority;

        public Listener(Object target, Method method, int priority) {
            this.target = target;
            this.method = method;
            this.priority = priority;
            this.method.setAccessible(true);
        }

        public void invoke(Object event) {
            try {
                method.invoke(target, event);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private final Map<Class<?>, List<Listener>> listenersMap = new ConcurrentHashMap<>();

    public void subscribe(Object object) {
        if (object == null) return;

        Class<?> clazz = object.getClass();
        while (clazz != null && clazz != Object.class) {
            for (Method method : clazz.getDeclaredMethods()) {
                if (method.isAnnotationPresent(EventHandler.class) && method.getParameterCount() == 1) {
                    Class<?> eventType = method.getParameterTypes()[0];
                    EventHandler annotation = method.getAnnotation(EventHandler.class);
                    Listener listener = new Listener(object, method, annotation.priority());

                    listenersMap.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>()).add(listener);
                    // Sort descending by priority
                    sortListeners(eventType);
                }
            }
            clazz = clazz.getSuperclass();
        }
    }

    public void unsubscribe(Object object) {
        if (object == null) return;

        for (List<Listener> listeners : listenersMap.values()) {
            listeners.removeIf(l -> l.target == object);
        }
    }

    private synchronized void sortListeners(Class<?> eventType) {
        List<Listener> list = listenersMap.get(eventType);
        if (list != null) {
            List<Listener> sorted = new ArrayList<>(list);
            sorted.sort((a, b) -> Integer.compare(b.priority, a.priority));
            listenersMap.put(eventType, new CopyOnWriteArrayList<>(sorted));
        }
    }

    public void post(Object event) {
        if (event == null) return;

        List<Listener> listeners = listenersMap.get(event.getClass());
        if (listeners != null) {
            for (Listener listener : listeners) {
                listener.invoke(event);
            }
        }
    }
}
