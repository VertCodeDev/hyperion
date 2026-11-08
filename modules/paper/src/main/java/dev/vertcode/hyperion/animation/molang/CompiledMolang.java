package dev.vertcode.hyperion.animation.molang;

import java.util.List;

/**
 * A compiled Molang text, and the names in it Hyperion does not know, which read as 0.
 *
 * @param expression the expression
 * @param unsupported the unknown names and functions, empty when everything is supported
 * @param constant true when the value never changes, so it can be worked out once
 */
public record CompiledMolang(MolangExpression expression, List<String> unsupported, boolean constant) {

    /**
     * Copy the list, so the result never changes.
     */
    public CompiledMolang {
        unsupported = List.copyOf(unsupported);
    }
}
