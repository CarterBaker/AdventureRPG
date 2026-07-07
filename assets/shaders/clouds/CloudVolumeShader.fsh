#version 330 core

in vec3 vWorldNormal;
out vec4 FragColor;

void main()
{
    vec3 n = abs(normalize(vWorldNormal));
    
    // Determine dominant axis
    if (n.x > n.y && n.x > n.z)
        FragColor = vec4(1.0, 0.0, 0.0, 1.0); // X axis → Red
    else if (n.y > n.x && n.y > n.z)
        FragColor = vec4(0.0, 1.0, 0.0, 1.0); // Y axis → Green
    else
        FragColor = vec4(0.0, 0.0, 1.0, 1.0); // Z axis → Blue
}