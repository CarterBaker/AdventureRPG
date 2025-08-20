#version 150

in vec2 fragUV;
out vec4 fragColor;

void main() {
    // simple gradient sky
    fragColor = vec4(fragUV.y, fragUV.y * 0.5 + 0.25, 1.0, 1.0);
}
