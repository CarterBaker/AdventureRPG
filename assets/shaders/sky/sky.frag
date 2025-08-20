#version 150

in vec3 v_dir;
out vec4 fragColor;

uniform float u_timeOfDay; // 0.0 = night, 0.5 = day, 1.0 = night

void main() {
    // 1. Vertical gradient factor
    float t = clamp(v_dir.y * 0.7 + 0.3, 0.0, 1.0);
    t = pow(t, 1.2);

    // 2. Define time-of-day sky colors
    vec3 nightColor = vec3(0.01, 0.03, 0.1);  // very dark blue
    vec3 dayColor   = vec3(0.4, 0.6, 1.0);    // bright blue

    // 3. Interpolate based on time of day
    // We'll use a simple "triangle" function so 0->0.5 = day rise, 0.5->1 = day fall
    float timeFactor = (u_timeOfDay <= 0.5) ? (u_timeOfDay * 2.0) : ((1.0 - u_timeOfDay) * 2.0);
    vec3 baseSky = mix(nightColor, dayColor, timeFactor);

    // 4. Apply vertical gradient on top
    vec3 bottom = baseSky * 0.5;  // darker at the bottom
    vec3 top    = baseSky;        // full color at the top
    vec3 sky = mix(bottom, top, t);

    fragColor = vec4(sky, 1.0);
}
