#version 330 core

in vec2 vQuadUV;

uniform sampler2D u_tileTexture;
uniform vec4      u_viewRect;
uniform vec4      u_uvRect;
uniform float     u_face;
uniform vec4      u_faceRect;

uniform vec3 u_globeCamera;
uniform vec3 u_globeForward;
uniform vec3 u_globeRight;
uniform vec3 u_globeUp;

uniform vec3 u_cubeAxisX;
uniform vec3 u_cubeAxisY;
uniform vec3 u_cubeAxisZ;

uniform float u_shadeFloor;

out vec4 FragColor;

// Shows one map tile on a globe world's globe, a unit sphere at the origin. The quad covers only the window rectangle
// the tile shows in, so each pixel's ray is cast exactly as the camera's pick ray is (GlobeViewStruct): forward, plus
// the pixel's offset from the window's middle along the right and up axes, already scaled to the window's edges. The
// point struck is turned into the block cube's own frame, its face found by the standard cube map rule and its place
// there by the equal-angle mapping the world's blocks are laid by (GlobeUtility), as shares of the face; every pixel
// outside the tile's square of its face is left to the tiles beside it. Inside, the tile texture is read with its
// rows running down the face's t axis, as the tile was generated, from the part of a coarser tile standing in for one
// still generating, and shaded by how squarely the ground faces the eye.

const vec3 FACE_NORMAL[6] = vec3[](
        vec3(1.0, 0.0, 0.0), vec3(-1.0, 0.0, 0.0), vec3(0.0, 1.0, 0.0),
        vec3(0.0, -1.0, 0.0), vec3(0.0, 0.0, 1.0), vec3(0.0, 0.0, -1.0));
const vec3 FACE_S[6] = vec3[](
        vec3(0.0, 0.0, -1.0), vec3(0.0, 0.0, 1.0), vec3(1.0, 0.0, 0.0),
        vec3(1.0, 0.0, 0.0), vec3(1.0, 0.0, 0.0), vec3(-1.0, 0.0, 0.0));
const vec3 FACE_T[6] = vec3[](
        vec3(0.0, -1.0, 0.0), vec3(0.0, -1.0, 0.0), vec3(0.0, 0.0, 1.0),
        vec3(0.0, 0.0, -1.0), vec3(0.0, -1.0, 0.0), vec3(0.0, -1.0, 0.0));
const float QUARTER_PI = 0.78539816;

int locateFace(vec3 d) {
    vec3 a = abs(d);

    if (a.x >= a.y && a.x >= a.z)
        return d.x > 0.0 ? 0 : 1;

    if (a.y >= a.z)
        return d.y > 0.0 ? 2 : 3;

    return d.z > 0.0 ? 4 : 5;
}

void main() {
    vec2  ndc   = mix(u_viewRect.xy, u_viewRect.zw, vQuadUV);
    vec3  ray   = normalize(u_globeForward + ndc.x * u_globeRight + ndc.y * u_globeUp);
    float along = dot(u_globeCamera, ray);
    float reach = along * along - (dot(u_globeCamera, u_globeCamera) - 1.0);

    if (reach < 0.0)
        discard;

    float distance = -along - sqrt(reach);

    if (distance < 0.0)
        discard;

    vec3 point = normalize(u_globeCamera + ray * distance);
    vec3 cube  = vec3(dot(point, u_cubeAxisX), dot(point, u_cubeAxisY), dot(point, u_cubeAxisZ));
    int  face  = locateFace(cube);

    if (float(face) != u_face)
        discard;

    float major = dot(cube, FACE_NORMAL[face]);
    vec2  plane = vec2(dot(cube, FACE_S[face]), dot(cube, FACE_T[face])) / major;
    vec2  share = (atan(plane) / QUARTER_PI + 1.0) * 0.5;

    if (share.x < u_faceRect.x || share.y < u_faceRect.y || share.x >= u_faceRect.z || share.y >= u_faceRect.w)
        discard;

    vec2 local = (share - u_faceRect.xy) / (u_faceRect.zw - u_faceRect.xy);
    vec3 color = texture(u_tileTexture, mix(u_uvRect.xy, u_uvRect.zw, local)).rgb;

    FragColor = vec4(color * mix(u_shadeFloor, 1.0, max(dot(point, -ray), 0.0)), 1.0);
}
