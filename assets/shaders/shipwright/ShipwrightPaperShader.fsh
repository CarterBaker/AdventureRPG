#version 330 core

in vec2 vQuadUV;

uniform float     u_mode;
uniform sampler2D u_inkTexture;
uniform vec4      u_inkRect;
uniform vec4      u_contentRect;
uniform vec2      u_cells;
uniform vec4      u_cursor;
uniform vec2      u_mirror;
uniform vec4      u_woodLight;
uniform vec4      u_woodDark;
uniform float     u_woodPlank;
uniform vec4      u_paperColor;
uniform vec4      u_ruleColor;
uniform vec4      u_blockColor;
uniform vec4      u_centerColor;
uniform vec4      u_cursorColor;

out vec4 FragColor;

// The drafting table. Mode 0 is the table itself: planks of wood across the
// window, grained along their length, each a shade of its own. Mode 1 is the
// draft sheet: paper with the ink drawn over it inside u_contentRect, ruled
// in octants with every block ruled heavier, the centre line marked while
// the hull is drawn mirrored, and the cursor laid over the octant under the
// pointer and its mirror. Mode 2 is a card: paper with a view inked across
// u_contentRect. u_contentRect is a share of the quad, u_inkRect the share
// of the ink texture drawn across it, u_cells the octants across and up.

float hash(float n) {
    return fract(sin(n) * 43758.5453);
}

vec3 wood(vec2 pixel) {
    float plank  = floor(pixel.y / u_woodPlank);
    float within = fract(pixel.y / u_woodPlank);
    float shade  = hash(plank) * 0.35;
    float grain  = sin((pixel.x * 0.012 + hash(plank + 7.0) * 40.0) + sin(pixel.y * 0.05 + plank) * 2.5);
    float rings  = smoothstep(0.2, 1.0, fract(grain * 2.0 + within));
    float seam   = smoothstep(0.0, 0.04, within) * smoothstep(1.0, 0.96, within);
    vec3  color  = mix(u_woodDark.rgb, u_woodLight.rgb, clamp(0.45 + shade + rings * 0.2, 0.0, 1.0));
    return color * mix(0.55, 1.0, seam);
}

float rule(vec2 cell, float spacing) {
    vec2 scaled   = cell / spacing;
    vec2 distance = abs(fract(scaled - 0.5) - 0.5) / max(fwidth(scaled), vec2(1e-5));
    return 1.0 - min(min(distance.x, distance.y), 1.0);
}

bool inRect(vec2 point, vec4 rect) {
    return point.x >= rect.x && point.y >= rect.y && point.x < rect.z && point.y < rect.w;
}

void main() {

    if (u_mode < 0.5) {
        FragColor = vec4(wood(gl_FragCoord.xy), 1.0);
        return;
    }

    vec3 color = u_paperColor.rgb;
    vec2 local = (vQuadUV - u_contentRect.xy) / max(u_contentRect.zw - u_contentRect.xy, vec2(1e-5));

    if (local.x >= 0.0 && local.y >= 0.0 && local.x < 1.0 && local.y < 1.0) {

        vec4 ink = texture(u_inkTexture, mix(u_inkRect.xy, u_inkRect.zw, local));

        if (u_mode < 1.5) {

            vec2 cell = local * u_cells;

            color = mix(color, u_ruleColor.rgb, rule(cell, 1.0) * u_ruleColor.a);
            color = mix(color, u_blockColor.rgb, rule(cell, 2.0) * u_blockColor.a);

            if (u_mirror.x > 0.5) {
                float center = 1.0 - min(abs(cell.y - u_mirror.y) / max(fwidth(cell.y), 1e-5), 1.0);
                color = mix(color, u_centerColor.rgb, center * u_centerColor.a);
            }

            color = mix(color, ink.rgb, ink.a);

            vec4 mirrored = vec4(u_cursor.x, u_cells.y - u_cursor.w, u_cursor.z, u_cells.y - u_cursor.y);
            bool cursor   = inRect(cell, u_cursor) || (u_mirror.x > 0.5 && u_cursor.x >= 0.0 && inRect(cell, mirrored));

            if (cursor)
                color = mix(color, u_cursorColor.rgb, u_cursorColor.a);
        } else {
            color = mix(color, ink.rgb, ink.a);
        }
    }

    FragColor = vec4(color, 1.0);
}
