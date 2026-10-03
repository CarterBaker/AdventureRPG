#version 330 core

in vec2 vLocal;

uniform vec4  u_fillColor;
uniform vec4  u_outlineColor;
uniform float u_outlineWidth;

out vec4 FragColor;

// A chevron arrow pointing along local +y, filled and outlined, with edges
// softened over one pixel. Shaped by the signed distance to its four corners:
// the tip, the two back corners and the notch between them.

const int MARKER_CORNER_COUNT = 4;

const vec2 MARKER_CORNERS[MARKER_CORNER_COUNT] = vec2[](
    vec2( 0.0,  0.85),
    vec2( 0.7, -0.75),
    vec2( 0.0, -0.35),
    vec2(-0.7, -0.75));

float signedDistanceToArrow(vec2 point) {
    float distanceSq = dot(point - MARKER_CORNERS[0], point - MARKER_CORNERS[0]);
    float side       = 1.0;

    for (int i = 0, j = MARKER_CORNER_COUNT - 1; i < MARKER_CORNER_COUNT; j = i, i++) {
        vec2 edge    = MARKER_CORNERS[j] - MARKER_CORNERS[i];
        vec2 toPoint = point - MARKER_CORNERS[i];
        vec2 nearest = toPoint - edge * clamp(dot(toPoint, edge) / dot(edge, edge), 0.0, 1.0);

        distanceSq = min(distanceSq, dot(nearest, nearest));

        bvec3 crossing = bvec3(point.y >= MARKER_CORNERS[i].y,
            point.y < MARKER_CORNERS[j].y,
            edge.x * toPoint.y > edge.y * toPoint.x);

        if (all(crossing) || all(not(crossing)))
        side = -side;
    }

    return side * sqrt(distanceSq);
}

void main() {
    float distance = signedDistanceToArrow(vLocal);
    float pixel    = fwidth(distance);
    float shape    = 1.0 - smoothstep(u_outlineWidth - pixel, u_outlineWidth + pixel, distance);
    float fill     = 1.0 - smoothstep(-pixel, pixel, distance);

    if (shape <= 0.0)
    discard;

    vec4 color = mix(u_outlineColor, u_fillColor, fill);

    FragColor = vec4(color.rgb, color.a * shape);
}
