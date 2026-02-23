#version 330 core

in vec2 vTexCoord;
in float vLayerIndex;

#include "includes/StandardTextureLayoutData.glsl"  // keeps your texture array references intact

out vec4 FragColor;

// Optional: scale UVs for visibility
const float uvScale = 1.0;

void main() {
    // Show U as red, V as green
    vec3 uvColor = vec3(vTexCoord.x * uvScale, vTexCoord.y * uvScale, 0.0);

    // Optional: overlay layer index as blue channel
    uvColor.b = fract(vLayerIndex); // or just 0 if no valid layer

    FragColor = vec4(uvColor, 1.0);
}