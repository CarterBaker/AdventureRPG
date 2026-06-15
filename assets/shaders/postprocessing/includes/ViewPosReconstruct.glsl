#ifndef VIEW_POS_RECONSTRUCT_GLSL
#define VIEW_POS_RECONSTRUCT_GLSL

// Requires: CameraData (u_inverseProjection), GBufferData (u_gDepth)
vec3 reconstructViewPos(vec2 uv) {
    float depth = texture(u_gDepth, uv).r;
    vec4  clip  = vec4(uv * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    vec4  view  = u_inverseProjection * clip;
    return view.xyz / view.w;
}

#endif