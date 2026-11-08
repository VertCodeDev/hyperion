package dev.vertcode.hyperion.animation.molang;

import java.util.List;

/**
 * Compile the Molang in Blockbench keyframes.
 *
 * <p>Supported: numbers, {@code + - * /}, comparisons, {@code && || !}, {@code ?:}, parentheses,
 * {@code math.*} (trigonometry in degrees, as Molang has it), {@code query.anim_time} and
 * {@code query.life_time} (both the animation's time), and {@code variable.*} and
 * {@code temp.*} set with {@code =}. Anything else reads as 0 and is listed, for a load warning.
 */
public final class Molang {

    private Molang() {
    }

    /**
     * Compile a Molang text. A plain number is read without the parser.
     *
     * @param text the text, such as {@code 22.5} or {@code math.sin(query.anim_time * 90) * 10}
     * @return the compiled text
     * @throws MolangException when the text is not valid Molang
     */
    public static CompiledMolang compile(final String text) {
        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            return new CompiledMolang(context -> 0f, List.of(), true);
        }

        try {
            float value = Float.parseFloat(trimmed);
            return new CompiledMolang(context -> value, List.of(), true);
        } catch (NumberFormatException exception) {
            return new MolangParser(trimmed).parse();
        }
    }
}
