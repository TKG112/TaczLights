uniform sampler2D DiffuseSampler0;
uniform sampler2D HandDepth;

in vec2 texCoord;

out vec4 fragColor;

void main() {
    // Nothing glows where the first person hand or gun was drawn: it's in front of every beam in the world
    fragColor = texture(HandDepth, texCoord).r < 1.0 ? vec4(0.0) : texture(DiffuseSampler0, texCoord);
}
