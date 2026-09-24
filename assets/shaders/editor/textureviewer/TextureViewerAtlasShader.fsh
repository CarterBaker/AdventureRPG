#version 330 core

in vec2 vAtlasUV;

#include "items/includes/ItemsStandard.glsl"

uniform sampler2DArray u_textureArray;
uniform vec4  u_tileRects[64];
uniform int   u_tileCount;
uniform int   u_hoveredTile;
uniform int   u_selectedTile;
uniform vec4  u_backgroundColor;
uniform vec4  u_outlineColor;
uniform vec4  u_hoverColor;
uniform vec4  u_selectColor;
uniform float u_outlineWidth;

out vec4 FragColor;

// Shows the albedo layer of the atlas over a flat backdrop, then outlines
// every source image's region, measured in screen pixels.
void main() {
    vec4 texel = texture(u_textureArray, vec3(vAtlasUV, float(u_layer_albedo)));
    vec4 color = vec4(mix(u_backgroundColor.rgb, texel.rgb, texel.a), 1.0);
    vec2 pixel = fwidth(vAtlasUV);

    for (int i = 0; i < u_tileCount; i++) {
        vec4 rect = u_tileRects[i];

        if (any(lessThan(vAtlasUV, rect.xy)) || any(greaterThan(vAtlasUV, rect.zw)))
            continue;

        vec2 edge = min(vAtlasUV - rect.xy, rect.zw - vAtlasUV) / pixel;

        if (min(edge.x, edge.y) > u_outlineWidth)
            continue;

        vec4 outline = u_outlineColor;

        if (i == u_hoveredTile)
            outline = u_hoverColor;

        if (i == u_selectedTile)
            outline = u_selectColor;

        color.rgb = mix(color.rgb, outline.rgb, outline.a);
    }

    FragColor = color;
}
