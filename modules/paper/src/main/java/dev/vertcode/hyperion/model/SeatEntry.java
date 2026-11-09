package dev.vertcode.hyperion.model;

import dev.vertcode.hyperion.api.model.Model;
import dev.vertcode.hyperion.api.model.behavior.builtin.SeatBehavior;

/**
 * A seat someone sits in, found by the entity they ride.
 *
 * @param model the model the seat belongs to
 * @param seat the seat
 */
public record SeatEntry(Model model, SeatBehavior seat) {
}
