// Hyperion armor, MIT licensed: draws 3D armor on vanilla's armor boxes, from records stored in
// the armor texture. Any texture without Hyperion's magic pixel is left as vanilla draws it.

const float HY_DISTANCE = ${DISTANCE};

// Vanilla's armor boxes other than the head: body, right arm, left arm, right leg, left leg.
// Texture offset and size (u, v, width, height), depth, lowest corner and size in model space.
const vec4 HY_LAYOUT[5] = vec4[5](
    vec4(16.0, 16.0, 8.0, 12.0), vec4(40.0, 16.0, 4.0, 12.0), vec4(40.0, 16.0, 4.0, 12.0),
    vec4(0.0, 16.0, 4.0, 12.0), vec4(0.0, 16.0, 4.0, 12.0));
const float HY_DEPTH[5] = float[5](4.0, 4.0, 4.0, 4.0, 4.0);
const vec3 HY_MIN[5] = vec3[5](
    vec3(-4.0, 0.0, -2.0), vec3(-3.0, -2.0, -2.0), vec3(-1.0, -2.0, -2.0),
    vec3(-2.0, 0.0, -2.0), vec3(-2.0, 0.0, -2.0));
const vec3 HY_SIZE[5] = vec3[5](
    vec3(8.0, 12.0, 4.0), vec3(4.0, 12.0, 4.0), vec3(4.0, 12.0, 4.0),
    vec3(4.0, 12.0, 4.0), vec3(4.0, 12.0, 4.0));

int hy_byte(float value) {
    return int(value * 255.0 + 0.5);
}

int hy_number(int x, int y) {
    vec4 texel = texelFetch(Sampler0, ivec2(x, y), 0);
    return hy_byte(texel.r) * 65536 + hy_byte(texel.g) * 256 + hy_byte(texel.b);
}

bool hy_mirrored(int part) {
    return part == 2 || part == 4;
}

// A side's rectangle on the 64 by 32 texture, sides in vanilla's order: down, up, west, north,
// east, south.
vec4 hy_rect(int part, int face) {
    vec4 box = HY_LAYOUT[part];
    float u = box.x;
    float v = box.y;
    float w = box.z;
    float h = box.w;
    float d = HY_DEPTH[part];
    if (face == 0) return vec4(u + d, v, u + d + w, v + d);
    if (face == 1) return vec4(u + d + w, v + d, u + d + 2.0 * w, v);
    if (face == 2) return vec4(u, v + d, u + d, v + d + h);
    if (face == 3) return vec4(u + d, v + d, u + d + w, v + d + h);
    if (face == 4) return vec4(u + d + w, v + d, u + 2.0 * d + w, v + d + h);
    return vec4(u + 2.0 * d + w, v + d, u + 2.0 * d + 2.0 * w, v + d + h);
}

// The UV vanilla gives a side's corner; a mirrored box lists its corners the other way round.
vec2 hy_corner_uv(vec4 rect, int corner, bool mirror) {
    int index = mirror ? 3 - corner : corner;
    if (index == 0) return rect.zy;
    if (index == 1) return rect.xy;
    if (index == 2) return rect.xw;
    return rect.zw;
}

// Which box a corner belongs to, from its UV and where it is in its face; -1 for the head.
int hy_part(vec2 pixel, int face, int corner) {
    for (int part = 0; part < 5; part++) {
        if (distance(pixel, hy_corner_uv(hy_rect(part, face), corner, hy_mirrored(part))) < 0.01) {
            return part;
        }
    }

    return -1;
}

void hy_armor(inout vec3 position, inout vec3 normal, inout vec2 uv, vec4 color) {
    hy_flags = vec4(0.0);
    hy_dye = color;
    hy_shade = vec4(1.0);
    vec4 magic = texelFetch(Sampler0, ivec2(0, 0), 0);
    if (hy_byte(magic.a) != 3 || hy_byte(magic.r) != 72 || hy_byte(magic.g) != 89 || hy_byte(magic.b) != 80) {
        return;
    }

    vec4 header = texelFetch(Sampler0, ivec2(1, 0), 0);
    int kind = hy_byte(header.g);
    int parts = hy_byte(header.b);
    float grow = float(hy_byte(texelFetch(Sampler0, ivec2(2, 0), 0).b)) / 64.0;
    hy_flags.x = 1.0;

    vec2 pixel = uv * vec2(64.0, 32.0);
    int corner = gl_VertexID % 4;
    int face = (gl_VertexID / 4) % 6;
    int part = hy_part(pixel, face, corner);
    bool drawn = part >= 0 && ((parts >> part) & 1) == 1;
    if (kind == 0) {
        // A flat layer hides near the camera, where its limb shows in 3D.
        if (drawn && length(position) < HY_DISTANCE) {
            hy_flags.z = 1.0;
        }

        return;
    }

    // A 3D layer draws one face on each box's front and back, and hides everything else.
    hy_flags.y = 1.0;
    hy_flags.z = 1.0;
    if (part < 0 || (face != 3 && face != 5)) {
        return;
    }

    int side = face == 3 ? 0 : 1;
    int row = 1 + part * 2 + side;
    if (hy_byte(texelFetch(Sampler0, ivec2(0, row), 0).a) == 0) {
        return;
    }

    // The limb's turn, from the face's normal, its sideways axis kept level. Vanilla never rolls
    // the body or legs; an arm's small sway is the one turn this cannot see.
    vec3 back = face == 3 ? -normal : normal;
    vec3 across = cross(vec3(0.0, 1.0, 0.0), -back);
    if (length(across) < 0.05) {
        return;
    }

    vec3 xAxis = normalize(across);
    vec3 zAxis = normalize(back);
    vec3 yAxis = cross(zAxis, xAxis);
    mat3 turn = mat3(xAxis, yAxis, zAxis);

    // Where vanilla put this corner on the box gives the limb's pivot.
    vec4 rect = hy_rect(part, face);
    vec3 low = HY_MIN[part] - grow;
    vec3 high = HY_MIN[part] + HY_SIZE[part] + grow;
    bool mirror = hy_mirrored(part);
    float x1 = mirror ? high.x : low.x;
    float x2 = mirror ? low.x : high.x;
    float tu = (pixel.x - rect.x) / (rect.z - rect.x);
    float tv = (pixel.y - rect.y) / (rect.w - rect.y);
    vec3 vanilla = side == 0
            ? vec3(mix(x1, x2, tu), mix(low.y, high.y, tv), low.z)
            : vec3(mix(x2, x1, tu), mix(low.y, high.y, tv), high.z);
    vec3 pivot = position - turn * vanilla / 16.0;
    if (length(pivot) > HY_DISTANCE) {
        return;
    }

    int x = corner * 5;
    vec3 target = vec3(
            float(hy_number(x, row) - 8388608),
            float(hy_number(x + 1, row) - 8388608),
            float(hy_number(x + 2, row) - 8388608)) / 256.0;
    vec2 targetUv = vec2(float(hy_number(x + 3, row)), float(hy_number(x + 4, row))) / 64.0;
    vec4 packedNormal = texelFetch(Sampler0, ivec2(20, row), 0);
    vec3 faceNormal = vec3(
            float(hy_byte(packedNormal.r) - 128),
            float(hy_byte(packedNormal.g) - 128),
            float(hy_byte(packedNormal.b) - 128)) / 127.0;

    position = pivot + turn * target / 16.0;
    normal = normalize(turn * faceNormal);
    uv = targetUv / vec2(textureSize(Sampler0, 0));
    hy_flags.z = 0.0;
}
