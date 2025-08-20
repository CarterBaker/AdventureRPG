#version 150

in vec3 v_dir;
out vec4 fragColor;

void main() {
    // Amplify the effect of looking up vs down
    float t = clamp(v_dir.y * 0.7 + 0.3, 0.001, 1.0); 
    t = pow(t, 1.2); // optional gamma tweak for more contrast

    // Mix between horizon color (blue) and sky-top color (pink)
    vec3 bottom = vec3(0.05, 0.1, 0.2); // darker blue for down
    vec3 top    = vec3(1.0, 0.8, 1.0);  // lighter pink/white for up

    fragColor = vec4(mix(bottom, top, t), 1.0);
}
