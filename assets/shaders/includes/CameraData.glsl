layout(std140) uniform CameraData {
    mat4 u_projection;
    mat4 u_view;
    mat4 u_inverseProjection;
    mat4 u_inverseView;
    mat4 u_viewProjection;
    vec3 u_cameraPosition;
    float u_cameraFOV;
    vec2 u_viewport;
    float u_nearPlane;
    float u_farPlane;
};