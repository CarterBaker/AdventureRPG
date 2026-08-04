#version 330 core

in vec3 vWorldNormal;
out vec4 FragColor;

const vec3  WATER_COLOR = vec3(0.35, 0.6, 0.9);
const float WATER_ALPHA = 0.55;

void main() {
    vec3 normal = normalize(vWorldNormal);
    float shade = clamp(0.6 + normal.y * 0.4, 0.6, 1.0);

    FragColor = vec4(WATER_COLOR * shade, WATER_ALPHA);
}