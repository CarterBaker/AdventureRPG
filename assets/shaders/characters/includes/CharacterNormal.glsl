#ifndef CHARACTER_NORMAL_GLSL
#define CHARACTER_NORMAL_GLSL

// A character's normal is the smooth normal MeshBuilder bakes for every corner, carried through the skinning and
// interpolated across each face, so a low-poly body shades as one continuous surface. Authored winding is free to
// vary, so the baked normal lies on either side of its face: it is turned toward the side the flat face normal,
// rebuilt from screen-space derivatives, shows the camera. A face with no smooth normal keeps the flat one.

const float CHARACTER_NORMAL_EPSILON = 0.000001;

vec3 resolveCharacterNormal(vec3 viewNormal, vec3 viewPosition) {
    vec3 faceNormal = normalize(cross(dFdx(viewPosition), dFdy(viewPosition)));

    if (dot(viewNormal, viewNormal) < CHARACTER_NORMAL_EPSILON)
    return faceNormal;

    vec3 normal = normalize(viewNormal);

    return dot(normal, faceNormal) < 0.0 ? -normal : normal;
}

#endif
