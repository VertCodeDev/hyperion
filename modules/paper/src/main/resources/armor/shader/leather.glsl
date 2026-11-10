// Hyperion armor, MIT licensed: leather armor dyed in a set's color draws that set's cell of the
// leather texture; any other leather draws the first cell, vanilla's own.

int hy_byte(float value) {
    return int(value * 255.0 + 0.5);
}

void hy_leather(inout vec2 uv, inout vec4 color) {
    ivec2 size = textureSize(Sampler0, 0);
    int cells = size.x / max(1, size.y * 2);
    if (cells < 2) {
        return;
    }

    int cell = 0;
    int red = hy_byte(color.r);
    if (hy_byte(color.g) == ${TAG_GREEN} && hy_byte(color.b) == ${TAG_BLUE} && red > 0 && red < cells) {
        cell = red;
        color = vec4(1.0, 1.0, 1.0, color.a);
    }

    uv.x = (uv.x + float(cell)) / float(cells);
}
