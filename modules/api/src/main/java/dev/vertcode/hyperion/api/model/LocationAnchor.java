package dev.vertcode.hyperion.api.model;

import org.bukkit.Location;

/**
 * A model that stands on its own at a location.
 *
 * @param location the model's origin; copied in and out, so changing it changes nothing
 */
public record LocationAnchor(Location location) implements ModelAnchor {

    /**
     * Create an anchor at a copy of the location.
     *
     * @param location the model's origin
     */
    public LocationAnchor {
        location = location.clone();
    }

    /**
     * Return the model's origin.
     *
     * @return a copy of the location
     */
    @Override
    public Location location() {
        return this.location.clone();
    }
}
