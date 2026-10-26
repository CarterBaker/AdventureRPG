#version 330 core

in vec2 vLocal;

uniform vec4  u_fillColor;
uniform vec4  u_outlineColor;
uniform float u_outlineWidth;
uniform float u_iconShape;

out vec4 FragColor;

// A hand placement's icon, filled and outlined, with edges softened over one
// pixel: a house standing upright for a settlement, u_iconShape 0, or a
// banner whose point faces along local +y for a structure, u_iconShape 1.
// Shaped by the signed distance to the chosen outline's six corners.

const int ICON_CORNER_COUNT = 6;

const vec2 HOUSE_CORNERS[ICON_CORNER_COUNT] = vec2[](
    vec2( 0.0,  0.9),
    vec2( 0.8,  0.15),
    vec2( 0.8, -0.85),
    vec2( 0.0, -0.85),
    vec2(-0.8, -0.85),
    vec2(-0.8,  0.15));

const vec2 BANNER_CORNERS[ICON_CORNER_COUNT] = vec2[](
    vec2( 0.0,   0.9),
    vec2( 0.65,  0.4),
    vec2( 0.65, -0.85),
    vec2( 0.0,  -0.45),
    vec2(-0.65, -0.85),
    vec2(-0.65,  0.4));

vec2 iconCorner(int index) {
    return u_iconShape < 0.5 ? HOUSE_CORNERS[index] : BANNER_CORNERS[index];
}

float signedDistanceToIcon(vec2 point) {
    float distanceSq = dot(point - iconCorner(0), point - iconCorner(0));
    float side       = 1.0;

    for (int i = 0, j = ICON_CORNER_COUNT - 1; i < ICON_CORNER_COUNT; j = i, i++) {
        vec2 cornerI = iconCorner(i);
        vec2 cornerJ = iconCorner(j);
        vec2 edge    = cornerJ - cornerI;
        vec2 toPoint = point - cornerI;
        vec2 nearest = toPoint - edge * clamp(dot(toPoint, edge) / dot(edge, edge), 0.0, 1.0);

        distanceSq = min(distanceSq, dot(nearest, nearest));

        bvec3 crossing = bvec3(point.y >= cornerI.y,
            point.y < cornerJ.y,
            edge.x * toPoint.y > edge.y * toPoint.x);

        if (all(crossing) || all(not(crossing)))
        side = -side;
    }

    return side * sqrt(distanceSq);
}

void main() {
    float distance = signedDistanceToIcon(vLocal);
    float pixel    = fwidth(distance);
    float shape    = 1.0 - smoothstep(u_outlineWidth - pixel, u_outlineWidth + pixel, distance);
    float fill     = 1.0 - smoothstep(-pixel, pixel, distance);

    if (shape <= 0.0)
    discard;

    vec4 color = mix(u_outlineColor, u_fillColor, fill);

    FragColor = vec4(color.rgb, color.a * shape);
}
