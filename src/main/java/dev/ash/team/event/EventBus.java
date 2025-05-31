package dev.ash.team.event;

import dev.ash.team.TeamClient;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The event bus handles registering listeners and dispatching events.
 * It uses the publish-subscribe pattern to decouple event publishers from subscribers.
 */
public class EventBus {
    // Map from event class to a list of handler methods and their targets
    private final Map<Class<? extends Event>, List<HandlerMethod>> eventHandlers = new HashMap<>();
    
    /**
     * Registers all event handlers in the given object.
     * 
     * @param object The object containing event handler methods
     */
    public void register(Object object) {
        // Find all methods with @EventTarget annotation
        for (Method method : object.getClass().getDeclaredMethods()) {
            if (!method.isAnnotationPresent(EventTarget.class)) {
                continue;
            }
            
            // Check if the method has exactly one parameter
            if (method.getParameterCount() != 1) {
                TeamClient.LOGGER.error("Invalid event handler method {}: must have exactly one parameter",
                        method.getName());
                continue;
            }
            
            // Check if the parameter is a subclass of Event
            Class<?> paramType = method.getParameterTypes()[0];
            if (!Event.class.isAssignableFrom(paramType)) {
                TeamClient.LOGGER.error("Invalid event handler method {}: parameter must be a subclass of Event",
                        method.getName());
                continue;
            }
            
            // Make the method accessible if it's private
            method.setAccessible(true);
            
            // Get the event class and priority
            @SuppressWarnings("unchecked")
            Class<? extends Event> eventClass = (Class<? extends Event>) paramType;
            EventPriority priority = method.getAnnotation(EventTarget.class).priority();
            
            // Create a handler method
            HandlerMethod handlerMethod = new HandlerMethod(object, method, priority);
            
            // Add the handler to the map
            eventHandlers.computeIfAbsent(eventClass, k -> new CopyOnWriteArrayList<>()).add(handlerMethod);
            
            // Sort the handlers by priority
            eventHandlers.get(eventClass).sort(Comparator.comparingInt(
                    h -> -h.getPriority().getValue())); // Negative to sort in descending order
        }
    }
    
    /**
     * Unregisters all event handlers in the given object.
     * 
     * @param object The object containing event handler methods
     */
    public void unregister(Object object) {
        // Remove all handlers for the given object
        for (List<HandlerMethod> handlers : eventHandlers.values()) {
            handlers.removeIf(handler -> handler.getTarget() == object);
        }
    }
    
    /**
     * Posts an event to all registered handlers.
     * 
     * @param event The event to post
     */
    public void post(Event event) {
        // Get all handlers for this event class and its superclasses
        List<HandlerMethod> handlers = new ArrayList<>();
        Class<?> eventClass = event.getClass();
        
        // Add handlers for the event class and all its superclasses
        while (eventClass != null && Event.class.isAssignableFrom(eventClass)) {
            @SuppressWarnings("unchecked")
            Class<? extends Event> currentClass = (Class<? extends Event>) eventClass;
            if (eventHandlers.containsKey(currentClass)) {
                handlers.addAll(eventHandlers.get(currentClass));
            }
            eventClass = eventClass.getSuperclass();
        }
        
        // Sort by priority
        handlers.sort(Comparator.comparingInt(h -> -h.getPriority().getValue()));
        
        // Call all handlers
        for (HandlerMethod handler : handlers) {
            try {
                handler.invoke(event);
                
                // Stop if the event is cancelled and not a cancellable event
                if (event.isCancelled()) {
                    break;
                }
            } catch (Exception e) {
                TeamClient.LOGGER.error("Error calling event handler", e);
            }
        }
    }
    
    /**
     * Represents a method that handles events.
     */
    private static class HandlerMethod {
        private final Object target;
        private final Method method;
        private final EventPriority priority;
        
        /**
         * Creates a new handler method.
         * 
         * @param target The object containing the method
         * @param method The method to call
         * @param priority The priority of the handler
         */
        public HandlerMethod(Object target, Method method, EventPriority priority) {
            this.target = target;
            this.method = method;
            this.priority = priority;
        }
        
        /**
         * Gets the target object of this handler.
         * 
         * @return The target object
         */
        public Object getTarget() {
            return target;
        }
        
        /**
         * Gets the priority of this handler.
         * 
         * @return The priority
         */
        public EventPriority getPriority() {
            return priority;
        }
        
        /**
         * Invokes the handler method with the given event.
         * 
         * @param event The event to pass to the handler
         * @throws Exception If an error occurs during invocation
         */
        public void invoke(Event event) throws Exception {
            method.invoke(target, event);
        }
    }
}