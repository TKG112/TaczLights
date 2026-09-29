#veil:buffer veil:camera VeilCamera

layout (location = 0) in vec3 Position;
layout (location = 1) in mat4 LightMatrix;
layout (location = 5) in vec3 Color;
layout (location = 6) in vec3 Settings;

#define Radius Settings.x
#define Length Settings.y

out mat4 lightMat;
out vec3 lightColor;
out vec3 beamSettings;

void main() {
    // Veil's inverted cube spans -1..1; squash it into a box around the beam, from its start to its end.
    // The box only picks which pixels get shaded, so keep it a little wide: a laser-thin box would fall between
    // pixels far away. The fragment shader still cuts the light to the real radius
    float boxRadius = max(Radius, 0.1);
    vec3 vertexPos = Position;
    vertexPos.z = clamp(vertexPos.z, 0.0, 1.0);
    vertexPos *= vec3(boxRadius, boxRadius, Length);

    // Same transform as Veil's area and spot lights: LightMatrix is rotation * translation(position)
    mat3 rotationMatrix = mat3(LightMatrix);
    mat3 inverseRotation = inverse(rotationMatrix);
    vec3 lightPos = inverseRotation * LightMatrix[3].xyz;
    vertexPos = inverseRotation * vertexPos + lightPos;
    gl_Position = VeilCamera.ProjMat * VeilCamera.ViewMat * vec4(vertexPos - VeilCamera.CameraPosition, 1.0);

    lightMat = LightMatrix;
    lightColor = Color;
    beamSettings = Settings;
}
