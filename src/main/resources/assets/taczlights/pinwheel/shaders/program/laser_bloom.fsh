uniform sampler2D Sampler0;
uniform float BloomStrength;

in vec4 vertexColor;
in vec2 texCoord0;

out vec4 fragColor;

void main() {
    vec4 color = texture(Sampler0, texCoord0) * vertexColor;
    // Only feeds the blurred glow: Veil's bloom also brightens the scene itself where the bloom alpha is high,
    // which isn't wanted for a thin beam
    fragColor = vec4(color.rgb * color.a * BloomStrength, 0.0);
}
