#version 330 core

in vec2 vAtlasUV;

#include "includes/TileTextures.glsl"

uniform vec4  u_tileRects[64];
uniform int   u_tileIDs[64];
uniform int   u_tileCount;
uniform int   u_hoveredTile;
uniform int   u_selectedTile;
uniform vec4  u_backgroundColor;
uniform vec4  u_outlineColor;
uniform vec4  u_hoverColor;
uniform vec4  u_selectColor;
uniform float u_outlineWidth;

out vec4 FragColor;

// Shows each tile's albedo in its grid cell over a flat backdrop, then
// outlines every tile, measured in screen pixels.
void main() {
    vec2 dx    = dFdx(vAtlasUV);
    vec2 dy    = dFdy(vAtlasUV);
    vec4 color = vec4(u_backgroundColor.rgb, 1.0);
    vec2 pixel = fwidth(vAtlasUV);

    for (int i = 0; i < u_tileCount; i++) {
        vec4 rect = u_tileRects[i];

        if (any(lessThan(vAtlasUV, rect.xy)) || any(greaterThan(vAtlasUV, rect.zw)))
            continue;

        vec2 span   = rect.zw - rect.xy;
        vec2 local  = (vAtlasUV - rect.xy) / span;
        vec2 tileDx = dx / span;
        vec2 tileDy = dy / span;
        vec4 texel  = sampleTileAlbedo(resolveTileReadGrad(u_tileIDs[i], tileDx, tileDy), local, tileDx, tileDy);

        color.rgb = mix(u_backgroundColor.rgb, texel.rgb, texel.a);

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
