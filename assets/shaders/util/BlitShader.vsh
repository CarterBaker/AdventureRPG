#version 330 core
layout (location = 0) in vec2 aPos;
layout (location = 1) in vec2 aTexCoord;

uniform vec4 u_destRect;   // x, y, w, h in pixels — negative sentinel = fullscreen
uniform vec2 u_resolution; // OS window width, height

out vec2 v_uv;

void main() {
    v_uv = aTexCoord;

    vec2 pos;

    if (u_destRect.x < 0.0) {
        // Fullscreen — remap unit quad [0,1] to NDC [-1,1]
        pos = aPos * 2.0 - 1.0;
    } else {
        float ndcX = (u_destRect.x / u_resolution.x) * 2.0 - 1.0;
        float ndcY = (u_destRect.y / u_resolution.y) * 2.0 - 1.0;
        float ndcW = (u_destRect.z / u_resolution.x) * 2.0;
        float ndcH = (u_destRect.w / u_resolution.y) * 2.0;

        pos.x = ndcX + aPos.x * ndcW;
        pos.y = ndcY + aPos.y * ndcH;
    }

    gl_Position = vec4(pos, 0.0, 1.0);
}