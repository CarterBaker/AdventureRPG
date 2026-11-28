#version 330 core

in vec2 vQuadUV;

uniform sampler2D u_tileTexture;
uniform sampler2D u_heightTexture;
uniform float     u_showBiomes;
uniform float     u_showHeights;
uniform float     u_overlayAlpha;
uniform float     u_faceTexels;

uniform vec3 u_globeCamera;
uniform vec3 u_globeForward;
uniform vec3 u_globeRight;
uniform vec3 u_globeUp;

uniform float u_brushFace;
uniform vec2  u_brushCenter;
uniform float u_brushRadius;
uniform float u_shape;
uniform float u_ratio;
uniform float u_angle;

uniform float u_footprintFace;
uniform vec2  u_footprintCenter;
uniform float u_footprintRadius;

uniform float u_showBox;
uniform vec4  u_boxRect;

uniform float u_showCube;
uniform vec3  u_cubeAxisX;
uniform vec3  u_cubeAxisY;
uniform vec3  u_cubeAxisZ;

uniform vec4  u_ringColor;
uniform float u_ringWidth;
uniform vec4  u_gridColor;
uniform float u_gridTexelPixels;
uniform vec4  u_emptyColor;
uniform vec4  u_cubeColor;
uniform vec4  u_cubeEdgeColor;
uniform float u_cubeSurfaceOpacity;
uniform float u_cubeFadeNear;
uniform float u_cubeFadeFar;
uniform float u_cornerRadius;

out vec4 FragColor;

// Shows a globe world's image on the ball it maps, a unit sphere at the origin, by casting each window pixel's ray
// exactly as the camera's pick ray is cast (WorldEditorViewSystem): forward, plus the pixel's offset from the
// window's middle along the right and up axes, already scaled to the window's edges. The point struck is read by the
// standard cube map rule on the image's faces, laid side by side across the image in cube map order, nearest texel,
// so every pixel shows as the square it is however close the view. The height layer lies over the biomes as it does
// on a flat world, a faint grid parts the texels once each spans enough window pixels, and the brush, the design's
// footprint and the selection box are drawn on the plane of the face they are anchored on, run on past its edges as
// the image engine stamps them. While the Cube layer is shown the block grid's cube is drawn inside the ball, seen
// through the surface while the camera stands well off it, fading away as it closes on the ground so painting sees the
// image alone, its edges and corners traced on the surface where the cube's faces fold onto the ball.

const vec3 FACE_NORMAL[6] = vec3[](
        vec3(1.0, 0.0, 0.0), vec3(-1.0, 0.0, 0.0), vec3(0.0, 1.0, 0.0),
        vec3(0.0, -1.0, 0.0), vec3(0.0, 0.0, 1.0), vec3(0.0, 0.0, -1.0));
const vec3 FACE_S[6] = vec3[](
        vec3(0.0, 0.0, -1.0), vec3(0.0, 0.0, 1.0), vec3(1.0, 0.0, 0.0),
        vec3(1.0, 0.0, 0.0), vec3(1.0, 0.0, 0.0), vec3(-1.0, 0.0, 0.0));
const vec3 FACE_T[6] = vec3[](
        vec3(0.0, -1.0, 0.0), vec3(0.0, -1.0, 0.0), vec3(0.0, 0.0, 1.0),
        vec3(0.0, 0.0, -1.0), vec3(0.0, -1.0, 0.0), vec3(0.0, -1.0, 0.0));
const float FACE_COUNT = 6.0;
const float CUBE_HALF   = 0.57735027;

int locateFace(vec3 d) {
    vec3 a = abs(d);

    if (a.x >= a.y && a.x >= a.z)
        return d.x > 0.0 ? 0 : 1;

    if (a.y >= a.z)
        return d.y > 0.0 ? 2 : 3;

    return d.z > 0.0 ? 4 : 5;
}

// A direction on one face's plane, in that face's texels; false when it faces away from the face
bool onFacePlane(vec3 d, int face, out vec2 texel) {
    float major = dot(d, FACE_NORMAL[face]);

    texel = (vec2(dot(d, FACE_S[face]), dot(d, FACE_T[face])) / max(major, 1e-6) + 1.0) * 0.5 * u_faceTexels;

    return major > 0.0;
}

// A ring a window-pixel width wide just inside where a distance reaches one
float ring(float distance) {
    float pixel     = fwidth(distance);
    float halfWidth = u_ringWidth * pixel * 0.5;

    return 1.0 - smoothstep(halfWidth, halfWidth + pixel, abs(distance - (1.0 - halfWidth)));
}

float brushOutline(vec3 d) {
    vec2 texel;

    if (u_brushFace < 0.0 || !onFacePlane(d, int(u_brushFace), texel))
        return 0.0;

    vec2  offset   = (texel - u_brushCenter) / u_brushRadius * vec2(1.0, -1.0);
    float c        = cos(u_angle);
    float s        = sin(u_angle);
    vec2  tip      = vec2(offset.x * c + offset.y * s, (offset.y * c - offset.x * s) / u_ratio);
    float distance = u_shape < 0.5
            ? length(tip)
            : u_shape < 1.5 ? max(abs(tip.x), abs(tip.y)) : abs(tip.x) + abs(tip.y);

    return ring(distance);
}

float footprintOutline(vec3 d) {
    vec2 texel;

    if (u_footprintRadius <= 0.0 || !onFacePlane(d, int(u_footprintFace), texel))
        return 0.0;

    return ring(length(texel - u_footprintCenter) / u_footprintRadius);
}

float boxOutline(int face, vec2 texel) {
    if (u_showBox < 0.5 || float(face) != floor(u_boxRect.x / u_faceTexels))
        return 0.0;

    vec2  image  = vec2(float(face) * u_faceTexels + texel.x, texel.y);
    vec2  inside = min(image - u_boxRect.xy, u_boxRect.zw - image);
    float edge   = min(inside.x, inside.y);
    float pixel  = fwidth(edge);

    return edge < 0.0 ? 0.0 : 1.0 - smoothstep(u_ringWidth * pixel, (u_ringWidth + 1.0) * pixel, edge);
}

// The texel grid, faded in as each texel grows past the pixels set for it
float texelGrid(vec2 texel) {
    vec2  span  = fwidth(texel);
    float shown = smoothstep(1.0 / u_gridTexelPixels, 0.5 / u_gridTexelPixels, max(span.x, span.y));
    vec2  line  = abs(fract(texel + 0.5) - 0.5) / max(span, vec2(1e-6));

    return shown * (1.0 - smoothstep(0.0, 1.0, min(line.x, line.y)));
}

vec3 toCube(vec3 d) {
    return vec3(dot(d, u_cubeAxisX), dot(d, u_cubeAxisY), dot(d, u_cubeAxisZ));
}

// The block cube's face edges and corners where they fold onto the ball
vec4 cubeTrace(vec3 d) {
    vec3  a      = abs(toCube(d));
    float high   = max(a.x, max(a.y, a.z));
    float low    = min(a.x, min(a.y, a.z));
    float middle = a.x + a.y + a.z - high - low;
    float gap    = high - middle;
    float edge   = 1.0 - smoothstep(u_ringWidth * fwidth(gap), (u_ringWidth + 1.0) * fwidth(gap), gap);
    float corner = 1.0 - smoothstep(u_cornerRadius, u_cornerRadius * 1.2, high - low);

    return vec4(u_cubeEdgeColor.rgb, max(edge, corner) * u_cubeEdgeColor.a);
}

// The cube within the ball, struck by the ray: its face shaded toward the eye, its edges traced; alpha zero on a miss
vec4 innerCube(vec3 origin, vec3 ray) {
    vec3 o     = toCube(origin);
    vec3 r     = toCube(ray);
    vec3 inv   = 1.0 / r;
    vec3 enter = (-CUBE_HALF - o) * inv;
    vec3 leave = (CUBE_HALF - o) * inv;
    vec3 lo    = min(enter, leave);
    vec3 hi    = max(enter, leave);
    float t0   = max(lo.x, max(lo.y, lo.z));
    float t1   = min(hi.x, min(hi.y, hi.z));

    if (t0 > t1 || t1 < 0.0)
        return vec4(0.0);

    vec3  q      = abs(o + r * t0) / CUBE_HALF;
    float high   = max(q.x, max(q.y, q.z));
    float middle = q.x + q.y + q.z - high - min(q.x, min(q.y, q.z));
    float edge   = 1.0 - smoothstep(0.0, u_ringWidth * fwidth(middle) + fwidth(middle), 1.0 - middle);
    vec3  normal = step(vec3(high - 1e-4), q) * sign(o + r * t0);
    float shade  = 0.55 + 0.45 * abs(dot(normal, r));

    return vec4(mix(u_cubeColor.rgb * shade, u_cubeEdgeColor.rgb, edge), 1.0);
}

void main() {
    vec2  ndc    = vQuadUV * 2.0 - 1.0;
    vec3  ray    = normalize(u_globeForward + ndc.x * u_globeRight + ndc.y * u_globeUp);
    float along  = dot(u_globeCamera, ray);
    float reach  = along * along - (dot(u_globeCamera, u_globeCamera) - 1.0);

    if (reach < 0.0)
        discard;

    float distance = -along - sqrt(reach);

    if (distance < 0.0)
        discard;

    vec3 point = normalize(u_globeCamera + ray * distance);
    int  face  = locateFace(point);
    vec2 texel;

    onFacePlane(point, face, texel);
    texel = clamp(texel, vec2(0.0), vec2(u_faceTexels - 1e-3));

    vec2 uv    = vec2((float(face) * u_faceTexels + texel.x) / (FACE_COUNT * u_faceTexels), texel.y / u_faceTexels);
    vec3 color = u_showBiomes > 0.5 ? texture(u_tileTexture, uv).rgb : u_emptyColor.rgb;

    if (u_showHeights > 0.5)
        color = mix(color, texture(u_heightTexture, uv).rgb, u_overlayAlpha);

    color *= 0.7 + 0.3 * max(dot(point, -ray), 0.0);
    color  = mix(color, u_gridColor.rgb, texelGrid(texel) * u_gridColor.a);

    if (u_showCube > 0.5) {
        vec4  inner = innerCube(u_globeCamera, ray);
        vec4  trace = cubeTrace(point);
        float shown = smoothstep(u_cubeFadeNear, u_cubeFadeFar, length(u_globeCamera) - 1.0) * inner.a;

        color = mix(inner.rgb, color, mix(1.0, u_cubeSurfaceOpacity, shown));
        color = mix(color, trace.rgb, trace.a);
    }

    float outline = max(brushOutline(point), max(footprintOutline(point), boxOutline(face, texel)));

    FragColor = vec4(mix(color, u_ringColor.rgb, outline * u_ringColor.a), 1.0);
}
