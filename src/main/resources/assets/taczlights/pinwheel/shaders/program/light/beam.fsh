#include veil:common
#include veil:space_helper
#include veil:color_utilities
#include veil:light

in mat4 lightMat;
in vec3 lightColor;
in vec3 beamSettings;

uniform sampler2D AlbedoSampler;
uniform sampler2D NormalSampler;
uniform sampler2D DepthSampler;

uniform vec2 ScreenSize;

out vec4 fragColor;

void main() {
    vec2 screenUv = gl_FragCoord.xy / ScreenSize;

    vec4 albedoColor = texture(AlbedoSampler, screenUv);
    if (albedoColor.a == 0) {
        discard;
    }
    vec3 normalVS = texture(NormalSampler, screenUv).xyz;
    float depth = texture(DepthSampler, screenUv).r;
    vec3 pos = screenToWorldSpace(screenUv, depth).xyz;

    float radius = beamSettings.x;
    float beamLength = beamSettings.y;
    float softness = beamSettings.z;

    // Into the beam's space, where it runs along +Z from the origin. Same translation fix as Veil's spot light
    mat4 worldToLight = lightMat;
    worldToLight[3].xyz *= -1.0;
    vec3 localPos = (worldToLight * vec4(pos, 1.0)).xyz;
    if (localPos.z < 0.0 || localPos.z > beamLength) {
        discard;
    }

    // The cylinder: full brightness near the axis, fading out towards the edge
    float axisDistance = length(localPos.xy);
    float radialFalloff = 1.0 - smoothstep(radius * (1.0 - softness), radius, axisDistance);
    if (radialFalloff <= 0.0) {
        discard;
    }

    // Light arrives travelling along the beam, so it comes from the beam's start
    vec3 beamDirection = transpose(mat3(lightMat)) * vec3(0.0, 0.0, 1.0);
    vec3 lightDirection = normalize((VeilCamera.ViewMat * vec4(-beamDirection, 0.0)).xyz);
    float diffuse = (dot(normalVS, lightDirection) + 1.0) * 0.5;
    diffuse = (diffuse + MINECRAFT_AMBIENT_LIGHT) / (1.0 + MINECRAFT_AMBIENT_LIGHT);
    diffuse *= radialFalloff;

    float reflectivity = 0.05;
    vec3 diffuseColor = diffuse * lightColor;

    fragColor = vec4(albedoColor.rgb * diffuseColor * (1.0 - reflectivity) + diffuseColor * reflectivity, 1.0);
}
