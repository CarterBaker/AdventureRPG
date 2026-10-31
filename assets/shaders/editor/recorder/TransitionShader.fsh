#version 330 core

in vec2 v_uv;
out vec4 o_color;

uniform sampler2D u_from;
uniform sampler2D u_to;
uniform vec4 u_mixParams;  // x how far over, y kind, z edge softness, w width over height
uniform vec4 u_sideParams; // xy sweep direction, z from side blank, w to side blank
uniform vec3 u_color;

const int KIND_DISSOLVE = 0;
const int KIND_DIP = 1;
const int KIND_WIPE = 2;
const int KIND_PUSH = 3;
const int KIND_IRIS = 4;

const float MIN_SOFTNESS = 0.0001;
const float HALF = 0.5;

// The side the picture comes from at the point, or the colour where that side shows nothing
vec3 readFrom(vec2 uv) {
    return u_sideParams.z > HALF ? u_color : texture(u_from, uv).rgb;
}

// The side the picture goes to at the point, or the colour where that side shows nothing
vec3 readTo(vec2 uv) {
    return u_sideParams.w > HALF ? u_color : texture(u_to, uv).rgb;
}

// How far the incoming side has reached at a point lying at the place along a sweep that runs from zero to the
// reach, its edge softened either side
float reveal(float place, float reach, float softness) {

    float edge = reach * (1.0 + 2.0 * softness) - softness;

    return 1.0 - smoothstep(edge - softness, edge + softness, place);
}

// The point's place across the frame along the sweep, zero where the sweep begins and one where it ends
float sweepPlace(vec2 uv) {
    return dot(uv - HALF, u_sideParams.xy) + HALF;
}

bool isInside(vec2 uv) {
    return uv.x >= 0.0 && uv.x <= 1.0 && uv.y >= 0.0 && uv.y <= 1.0;
}

void main() {

    float over = u_mixParams.x;
    int kind = int(u_mixParams.y + HALF);
    float softness = max(u_mixParams.z, MIN_SOFTNESS);
    vec3 color;

    if (kind == KIND_DIP) {
        color = over < HALF
                ? mix(readFrom(v_uv), u_color, over / HALF)
                : mix(u_color, readTo(v_uv), (over - HALF) / HALF);
    } else if (kind == KIND_WIPE) {
        color = mix(readFrom(v_uv), readTo(v_uv), reveal(sweepPlace(v_uv), over, softness));
    } else if (kind == KIND_PUSH) {
        vec2 fromUV = v_uv - u_sideParams.xy * over;
        vec2 toUV = fromUV + u_sideParams.xy;
        color = isInside(toUV) ? readTo(toUV) : readFrom(fromUV);
    } else if (kind == KIND_IRIS) {
        vec2 offset = (v_uv - HALF) * vec2(u_mixParams.w, 1.0);
        float radius = length(vec2(HALF * u_mixParams.w, HALF));
        color = mix(readFrom(v_uv), readTo(v_uv), reveal(length(offset) / radius, over, softness));
    } else {
        color = mix(readFrom(v_uv), readTo(v_uv), over);
    }

    o_color = vec4(color, 1.0);
}
