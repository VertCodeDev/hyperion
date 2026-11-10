package dev.vertcode.hyperion.pack.armor;

/**
 * Small shaders shaped like vanilla's, written for these tests, with the lines the patcher
 * looks for.
 */
final class ShaderSamples {

    /**
     * A vertex shader in the style of 1.21 to 1.21.8.
     */
    static final String VERTEX = """
            #version 150

            #moj_import <minecraft:light.glsl>

            in vec3 Position;
            in vec4 Color;
            in vec2 UV0;
            in ivec2 UV2;
            in vec3 Normal;

            uniform mat4 ModelViewMat;
            uniform mat4 ProjMat;

            out vec4 vertexColor;
            out vec2 texCoord0;

            void main() {
                gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
                vertexColor = minecraft_mix_light(Light0_Direction, Light1_Direction, Normal, Color);
                texCoord0 = UV0;
            }
            """;

    /**
     * A vertex shader in the style of 26.3, every input and output at a location.
     */
    static final String VERTEX_LOCATED = """
            #version 330
            #extension GL_ARB_separate_shader_objects : require

            layout(location = 0) in vec3 Position;
            layout(location = 1) in vec4 Color;
            layout(location = 2) in vec2 UV0;
            layout(location = 5) in vec3 Normal;

            layout(location = 2) out vec4 vertexColor;
            layout(location = 6) out vec2 texCoord0;

            void main() {
                gl_Position = vec4(Position, 1.0);
                vertexColor = Color;
                texCoord0 = UV0;
            }
            """;

    /**
     * A fragment shader in the style of 1.21.2 and later.
     */
    static final String FRAGMENT = """
            #version 150

            uniform sampler2D Sampler0;

            in vec4 vertexColor;
            in vec4 lightMapColor;
            in vec2 texCoord0;

            out vec4 fragColor;

            void main() {
                vec4 color = texture(Sampler0, texCoord0);
                if (color.a < 0.1) {
                    discard;
                }
                fragColor = color * vertexColor * lightMapColor;
            }
            """;

    private ShaderSamples() {
    }
}
