package dev.vertcode.hyperion.api.event;

import org.bukkit.event.Event;

import dev.vertcode.hyperion.api.model.Model;

/**
 * An event about one live model, fired on the region that owns it.
 */
public abstract class ModelEvent extends Event {

    private final Model model;

    /**
     * Create an event about a model.
     *
     * @param model the model the event is about
     */
    protected ModelEvent(final Model model) {
        this.model = model;
    }

    /**
     * Return the model this event is about.
     *
     * @return the model
     */
    public Model model() {
        return this.model;
    }
}
