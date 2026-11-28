#version 330 core

in vec2 vQuadUV;

uniform sampler2D u_tileTexture;
uniform sampler2D u_daylightTexture;
uniform sampler2D u_weatherTexture;
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

uniform vec2  u_weatherOffset;
uniform float u_showDayNight;
uniform float u_showWeather;
uniform vec4  u_nightColor;
uniform float u_nightStrength;
uniform vec4  u_cloudColor;
uniform float u_cloudStrength;
uniform vec4  u_rainColor;
uniform float u_rainStrength;

uniform float u_shadeFloor;

out vec4 FragColor;

// Shows one map tile on a globe world's globe, a unit sphere at the origin. The quad covers only the window rectangle
// the tile shows in, so each pixel's ray is cast exactly as the camera's pick ray is (GlobeViewStruct): forward, plus
// the pixel's offset from the window's middle along the right and up axes, already scaled to the window's edges. The
// point struck is turned into the block cube's own frame, its face found by the standard cube map rule and its place
// there by the equal-angle mapping the world's blocks are laid by (GlobeUtility), as shares of the face; every pixel
// outside the tile's square of its face is left to the tiles beside it. Inside, the tile texture is read with its
// rows running down the face's t axis, as the tile was generated, from the part of a coarser tile standing in for one
// still generating, and shaded by how squarely the ground faces the eye. The shared overlays lie over the world's
// image, its six faces side by side, each a whole number of overlay texels wide, so the place is read at its share of
// the image, held half a texel inside its own face so the face beside it never bleeds in: the weather, slid by the
// weather flow, tints rain and lays cloud, and day and night shades the night side.

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
const float FACE_COUNT = 6.0;

// A face's place as a share of the image, kept half a texel of the overlay read inside the face
vec2 toImage(int face, vec2 share, sampler2D overlay) {
    float inset = 0.5 / float(textureSize(overlay, 0).x);
    float left  = float(face) / FACE_COUNT;
    return vec2(clamp(left + share.x / FACE_COUNT, left + inset, left + 1.0 / FACE_COUNT - inset), share.y);
}

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

    if (u_showWeather > 0.5) {
        vec2 weather = texture(u_weatherTexture, toImage(face, share, u_weatherTexture) - u_weatherOffset).rg;
        color = mix(color, u_rainColor.rgb, weather.g * u_rainStrength);
        color = mix(color, u_cloudColor.rgb, weather.r * u_cloudStrength);
    }

    if (u_showDayNight > 0.5) {
        float night = texture(u_daylightTexture, toImage(face, share, u_daylightTexture)).r;
        color = mix(color, color * u_nightColor.rgb, night * u_nightStrength);
    }

    FragColor = vec4(color * mix(u_shadeFloor, 1.0, max(dot(point, -ray), 0.0)), 1.0);
}
