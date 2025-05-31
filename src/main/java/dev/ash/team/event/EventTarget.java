package dev.ash.team.event;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used to mark methods as event handlers.
 * Methods with this annotation will be called when the corresponding event is posted.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface EventTarget {
    /**
     * The priority of the event handler.
     * 
     * @return The priority
     */
    EventPriority priority() default EventPriority.MEDIUM;
}