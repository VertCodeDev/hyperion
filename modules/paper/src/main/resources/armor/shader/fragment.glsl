// Hyperion armor, MIT licensed: turns the alpha codes of Hyperion's armor textures back into
// plain, tinted and glowing pixels, and hides what the vertex shader hid.

vec3 hy_unlit(vec3 rgb) {
    rgb /= max(hy_shade.rgb, vec3(0.01));
#ifndef EMISSIVE
    rgb /= max(lightMapColor.rgb, vec3(0.01));
#endif
    return rgb;
}

vec4 hy_armor_color(vec4 color) {
    if (hy_flags.x < 0.5) {
        return color;
    }

    if (hy_flags.z > 0.5) {
        return vec4(color.rgb, 0.0);
    }

    int alpha = int(color.a * 255.0 + 0.5);
    if (hy_flags.y > 0.5) {
        if (alpha == 0 || alpha > 4) {
            return vec4(color.rgb, 0.0);
        }

        color.a = 1.0;
        if (alpha != 2 && alpha != 4) {
            // Not tinted: undo the dye vanilla multiplies in.
            color.rgb /= max(hy_dye.rgb, vec3(1.0 / 255.0));
        }

        if (alpha >= 3) {
            color.rgb = hy_unlit(color.rgb);
        }

        return color;
    }

    if (alpha == 254) {
        color.a = 1.0;
        color.rgb = hy_unlit(color.rgb);
    }

    return color;
}
