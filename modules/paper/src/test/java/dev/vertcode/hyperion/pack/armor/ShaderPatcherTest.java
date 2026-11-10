package dev.vertcode.hyperion.pack.armor;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShaderPatcherTest {

    @Test
    void letsHyperionChangeTheInputsBeforeVanillaUsesThem() {
        String patched = ShaderPatcher.armorVertex(ShaderSamples.VERTEX, 24f).orElseThrow();
        String main = patched.substring(patched.indexOf("void main()"));

        assertTrue(main.contains("vec3 hy_Position = Position;"));
        assertTrue(main.contains("hy_armor(hy_Position, hy_Normal, hy_UV0, hy_Color);"));
        assertTrue(main.contains("vec4(hy_Position, 1.0)"));
        assertTrue(main.contains("minecraft_mix_light(Light0_Direction, Light1_Direction, hy_Normal, hy_Color)"));
        assertTrue(main.contains("texCoord0 = hy_UV0;"));
        assertTrue(main.contains("#ifdef MINECRAFT_LIGHT_POWER"));
    }

    @Test
    void putsItsCodeBeforeMain() {
        String patched = ShaderPatcher.armorVertex(ShaderSamples.VERTEX, 24f).orElseThrow();
        int main = patched.indexOf("void main()");

        assertTrue(patched.indexOf("uniform sampler2D Sampler0;") < main);
        assertTrue(patched.indexOf("out vec4 hy_flags;") < main);
        assertTrue(patched.indexOf("void hy_armor(") < main);
        assertTrue(patched.contains("const float HY_DISTANCE = 24.0;"));
        assertFalse(patched.contains("layout(location"));
    }

    @Test
    void givesItsVaryingsALocationWhenVanillaDoes() {
        String vertex = ShaderPatcher.armorVertex(ShaderSamples.VERTEX_LOCATED, 32f).orElseThrow();
        String fragment = ShaderPatcher.armorFragment(ShaderSamples.FRAGMENT, ShaderSamples.VERTEX_LOCATED).orElseThrow();

        assertTrue(vertex.contains("layout(location = 10) out vec4 hy_flags;"));
        assertTrue(vertex.contains("layout(location = 12) out vec4 hy_shade;"));
        assertTrue(fragment.contains("layout(location = 10) in vec4 hy_flags;"));
    }

    @Test
    void decodesTheColorRightAfterVanillaSamplesIt() {
        String patched = ShaderPatcher.armorFragment(ShaderSamples.FRAGMENT, ShaderSamples.VERTEX).orElseThrow();
        String sample = "vec4 color = texture(Sampler0, texCoord0);";
        int sampled = patched.indexOf(sample) + sample.length();

        assertEquals(sampled, patched.indexOf("\n    color = hy_armor_color(color);"));
        assertTrue(patched.indexOf("vec4 hy_armor_color(") < patched.indexOf("void main()"));
        assertTrue(patched.contains("in vec4 hy_dye;"));
    }

    @Test
    void leavesAShaderItDoesNotKnowAlone() {
        String noNormal = ShaderSamples.VERTEX.replace("in vec3 Normal;", "");

        assertTrue(ShaderPatcher.armorVertex(noNormal, 32f).isEmpty());
        assertTrue(ShaderPatcher.armorFragment(ShaderSamples.FRAGMENT.replace("texture(Sampler0", "texture(Other"), "").isEmpty());
    }

    @Test
    void readsTheSetColorTheItemsAreDyed() {
        String patched = ShaderPatcher.leatherVertex(ShaderSamples.VERTEX).orElseThrow();

        assertTrue(patched.contains("hy_byte(color.g) == 72 && hy_byte(color.b) == 89"));
        assertTrue(patched.contains("hy_leather(hy_UV0, hy_Color);"));
        assertFalse(patched.contains("hy_flags"));
    }
}
