package made4mischief.astatine.loader.api.event;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class EventManager {
    public static final EventManager INSTANCE = new EventManager();
    private final List<RegisteredListener> listeners = new CopyOnWriteArrayList<>();

    private EventManager() {
    }

    public void installRuntime(Object runtime) {
    }

    public void register(Listenable listenable) {
        for (Method method : listenable.getClass().getDeclaredMethods()) {
            if (method.isAnnotationPresent(EventTarget.class) && method.getParameterCount() == 1) {
                method.setAccessible(true);
                listeners.add(new RegisteredListener(listenable, method, method.getParameterTypes()[0]));
            }
        }
    }

    public void unregister(Listenable listenable) {
        listeners.removeIf(l -> l.listener == listenable);
    }

    public void post(Object event) {
        Class<?> eventClass = event.getClass();
        for (RegisteredListener rl : listeners) {
            if (rl.eventType.isAssignableFrom(eventClass)) {
                try {
                    rl.method.invoke(rl.listener, event);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private record RegisteredListener(Listenable listener, Method method, Class<?> eventType) {
    }
}
