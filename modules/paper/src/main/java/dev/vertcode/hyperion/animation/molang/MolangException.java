package dev.vertcode.hyperion.animation.molang;

/**
 * A Molang text that cannot be read.
 */
public final class MolangException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Create the exception.
     *
     * @param message what is wrong, for a load warning
     */
    MolangException(final String message) {
        super(message);
    }
}
