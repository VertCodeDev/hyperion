package dev.vertcode.hyperion.api.model.behavior.builtin;

/**
 * The movement keys a rider holds, as their client last sent them.
 *
 * <p>Hyperion reads these from the client's packets, which works the same on every Minecraft
 * version it supports, including those older than Paper's own input api.
 *
 * @param forward whether the forward key is held
 * @param backward whether the back key is held
 * @param left whether the left strafe key is held
 * @param right whether the right strafe key is held
 * @param jump whether the jump key is held
 * @param sneak whether the sneak key is held
 * @param sprint whether the rider is sprinting
 */
public record RiderInput(
        boolean forward,
        boolean backward,
        boolean left,
        boolean right,
        boolean jump,
        boolean sneak,
        boolean sprint
) {

    /**
     * No key held.
     */
    public static final RiderInput NONE = new RiderInput(false, false, false, false, false, false, false);
}
