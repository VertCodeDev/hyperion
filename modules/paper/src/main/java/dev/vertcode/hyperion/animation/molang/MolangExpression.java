package dev.vertcode.hyperion.animation.molang;

/**
 * A compiled Molang expression.
 */
@FunctionalInterface
public interface MolangExpression {

    /**
     * Work out the expression's value.
     *
     * @param context the time, variables and randomness it reads
     * @return the value
     */
    float evaluate(MolangContext context);
}
