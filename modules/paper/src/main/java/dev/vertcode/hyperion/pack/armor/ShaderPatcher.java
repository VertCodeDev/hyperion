package dev.vertcode.hyperion.pack.armor;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dev.vertcode.hyperion.armor.ArmorPackInfo;

/**
 * Splice Hyperion's armor code into the vanilla shaders of the server's own version.
 *
 * <p>Shaders change in almost every Minecraft version, so Hyperion never ships a copy: it reads
 * the vanilla files from the client jar and adds its code at points every version has. The
 * vertex shader's main gets a copy of the position, normal, UV and color that Hyperion may
 * change before vanilla's code uses them; the fragment shader decodes the sampled color right
 * after vanilla samples it. A file without those points is left alone, and that part of the
 * armor is drawn flat.
 */
final class ShaderPatcher {

    private static final String MAIN = "void main()";
    private static final List<String> INPUTS = List.of("Position", "Normal", "UV0", "Color");
    private static final String[] TYPES = {"vec3", "vec3", "vec2", "vec4"};
    private static final Pattern SAMPLE = Pattern.compile(
            "vec4\\s+color\\s*=\\s*texture\\(\\s*Sampler0\\s*,\\s*texCoord0\\s*\\)\\s*;");
    private static final String SAMPLER = "uniform sampler2D Sampler0;\n";
    // light.glsl defines this in every version; from 26.2 the file is only included for lit pipelines.
    private static final String LIGHT_GUARD = "MINECRAFT_LIGHT_POWER";
    private static final String[] VARYINGS = {"hy_flags", "hy_dye", "hy_shade"};
    private static final int FIRST_LOCATION = 10;
    private static final String VERTEX = "armor/shader/vertex.glsl";
    private static final String FRAGMENT = "armor/shader/fragment.glsl";
    private static final String LEATHER = "armor/shader/leather.glsl";
    private static final int BYTE = 0xFF;
    private static final int BYTE_BITS = 8;

    private ShaderPatcher() {
    }

    /**
     * Add the 3D armor code to the entity vertex shader.
     *
     * @param source the vanilla vertex shader
     * @param distance how far from the camera armor is drawn in 3D, in blocks
     * @return the patched shader, or empty when it has no place for the code
     */
    static Optional<String> armorVertex(final String source, final float distance) {
        if (!hasInputs(source)) {
            return Optional.empty();
        }

        boolean locations = source.contains("layout(location");
        String functions = template(VERTEX).replace("${DISTANCE}", String.format(Locale.ROOT, "%.1f", distance));
        String declarations = sampler(source) + varyings("out", locations) + functions;
        String shade = """
                if (hy_flags.x > 0.5) {
                        hy_shade = minecraft_mix_light(Light0_Direction, Light1_Direction, hy_Normal, vec4(1.0));
                    }
                """;
        shade = "\n#ifdef " + LIGHT_GUARD + "\n    " + shade + "#endif\n";

        String call = "hy_armor(hy_Position, hy_Normal, hy_UV0, hy_Color);\n    " + shade;

        return vertex(source, declarations, call);
    }

    /**
     * Add the 3D armor code to the entity fragment shader.
     *
     * @param source the vanilla fragment shader
     * @param vertex the vanilla vertex shader, which tells whether varyings need a location
     * @return the patched shader, or empty when it has no place for the code
     */
    static Optional<String> armorFragment(final String source, final String vertex) {
        Matcher sample = SAMPLE.matcher(source);
        int main = source.indexOf(MAIN);
        if (!sample.find() || main < 0 || sample.start() < main) {
            return Optional.empty();
        }

        boolean locations = vertex.contains("layout(location") || source.contains("layout(location");
        String declarations = varyings("in", locations) + template(FRAGMENT) + "\n";
        String sampled = source.substring(0, sample.end()) + "\n    color = hy_armor_color(color);" + source.substring(sample.end());

        return Optional.of(sampled.substring(0, main) + declarations + sampled.substring(main));
    }

    /**
     * Add the leather cell code to the 1.21 to 1.21.1 armor vertex shader.
     *
     * @param source the vanilla armor vertex shader
     * @return the patched shader, or empty when it has no place for the code
     */
    static Optional<String> leatherVertex(final String source) {
        if (!hasInputs(source)) {
            return Optional.empty();
        }

        String functions = template(LEATHER)
                .replace("${TAG_GREEN}", String.valueOf(ArmorPackInfo.COLOR_TAG >> BYTE_BITS & BYTE))
                .replace("${TAG_BLUE}", String.valueOf(ArmorPackInfo.COLOR_TAG & BYTE));

        return vertex(source, sampler(source) + functions, "hy_leather(hy_UV0, hy_Color);\n");
    }

    /**
     * Put declarations before a vertex shader's main, and make its main work on copies of the
     * inputs that a call may change first.
     *
     * @param source the shader
     * @param declarations what goes before main
     * @param call what runs at the start of main, on the copies
     * @return the patched shader, or empty when main is not found
     */
    private static Optional<String> vertex(final String source, final String declarations, final String call) {
        int main = source.indexOf(MAIN);
        int open = main < 0 ? -1 : source.indexOf('{', main);
        int close = open < 0 ? -1 : closing(source, open);
        if (close < 0) {
            return Optional.empty();
        }

        String body = source.substring(open + 1, close);
        StringBuilder copies = new StringBuilder("\n");
        for (int index = 0; index < INPUTS.size(); index++) {
            String input = INPUTS.get(index);
            body = body.replaceAll("\\b" + input + "\\b", "hy_" + input);
            copies.append("    ").append(TYPES[index]).append(" hy_").append(input).append(" = ").append(input).append(";\n");
        }

        copies.append("    ").append(call);

        return Optional.of(source.substring(0, main)
                + declarations
                + "\n"
                + source.substring(main, open + 1)
                + copies
                + body
                + source.substring(close));
    }

    /**
     * Return whether a vertex shader declares every input Hyperion copies.
     *
     * @param source the shader
     * @return true when it does
     */
    private static boolean hasInputs(final String source) {
        for (int index = 0; index < INPUTS.size(); index++) {
            Pattern declaration = Pattern.compile("in\\s+" + TYPES[index] + "\\s+" + INPUTS.get(index) + "\\s*;");
            if (!declaration.matcher(source).find()) {
                return false;
            }
        }

        return source.contains(MAIN);
    }

    /**
     * Find the brace that closes one.
     *
     * @param source the text
     * @param open where the opening brace is
     * @return where the closing brace is, or -1 when there is none
     */
    private static int closing(final String source, final int open) {
        int depth = 0;
        for (int index = open; index < source.length(); index++) {
            char character = source.charAt(index);
            if (character == '{') {
                depth++;
            } else if (character == '}') {
                depth--;
                if (depth == 0) {
                    return index;
                }
            }
        }

        return -1;
    }

    /**
     * Declare {@code Sampler0} when a shader does not yet.
     *
     * @param source the shader
     * @return the declaration, or nothing
     */
    private static String sampler(final String source) {
        return Pattern.compile("sampler2D\\s+Sampler0\\b").matcher(source).find() ? "" : SAMPLER;
    }

    /**
     * Declare Hyperion's varyings.
     *
     * @param direction {@code out} in the vertex shader, {@code in} in the fragment shader
     * @param locations whether the shader gives every varying a location
     * @return the declarations
     */
    private static String varyings(final String direction, final boolean locations) {
        StringBuilder text = new StringBuilder();
        for (int index = 0; index < VARYINGS.length; index++) {
            if (locations) {
                text.append("layout(location = ").append(FIRST_LOCATION + index).append(") ");
            }

            text.append(direction).append(" vec4 ").append(VARYINGS[index]).append(";\n");
        }

        return text.append('\n').toString();
    }

    /**
     * Read one of Hyperion's shader templates from the jar.
     *
     * @param path the template's path
     * @return its text
     */
    static String template(final String path) {
        try (InputStream stream = ShaderPatcher.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalStateException("The jar is missing " + path);
            }

            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}
