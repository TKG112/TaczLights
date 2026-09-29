package net.tkg.taczlights.client.beam;

import foundry.veil.api.client.registry.LightTypeRegistry;
import foundry.veil.api.client.render.CullFrustum;
import foundry.veil.api.client.render.light.InstancedLightData;
import foundry.veil.api.client.render.light.data.LightData;
import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4d;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Vector3f;

import java.nio.ByteBuffer;

/**
 * A cylinder of light: it starts at {@link #getPosition()} and shines along the orientation's forward axis for
 * {@link #getLength()} blocks, lighting only what lies within {@link #getRadius()} of that axis. Unlike a spot light
 * it doesn't widen with distance, which is what a laser needs.
 */
public class BeamLightData extends LightData implements InstancedLightData {
    private final Vector3d position = new Vector3d();
    private final Quaternionf orientation = new Quaternionf();
    private final Matrix4d matrix = new Matrix4d();

    private float radius = 0.05F;
    private float length = 16.0F;
    private float softness = 0.5F;

    @Override
    public LightTypeRegistry.LightType<?> getType() {
        return TaczLightsLightTypes.BEAM.get();
    }

    public Vector3dc getPosition() {
        return this.position;
    }

    /**
     * Uses the same convention as Veil's spot lights: the orientation turns the beam's direction into +Z.
     */
    public Quaternionfc getOrientation() {
        return this.orientation;
    }

    public float getRadius() {
        return this.radius;
    }

    public float getLength() {
        return this.length;
    }

    /**
     * @return How much of the radius, from the outside in, fades out. 0 is a hard edge, 1 fades all the way to the center
     */
    public float getSoftness() {
        return this.softness;
    }

    /**
     * Points the beam from a position along a direction.
     *
     * @param up Any direction not parallel to {@code direction}, to fix the beam's roll
     */
    public BeamLightData set(Vector3dc position, Vector3f direction, Vector3f up) {
        this.position.set(position);
        this.orientation.identity().lookAlong(new Vector3f(direction).negate(), up);
        this.markDirty();
        return this;
    }

    public BeamLightData setRadius(float radius) {
        this.radius = radius;
        this.markDirty();
        return this;
    }

    public BeamLightData setLength(float length) {
        this.length = length;
        this.markDirty();
        return this;
    }

    public BeamLightData setSoftness(float softness) {
        this.softness = softness;
        this.markDirty();
        return this;
    }

    @Override
    public BeamLightData setColor(int color) {
        super.setColor(color);
        return this;
    }

    @Override
    public BeamLightData setBrightness(float brightness) {
        super.setBrightness(brightness);
        return this;
    }

    @Override
    public LightData setTo(Camera camera) {
        Vec3 pos = camera.getPosition();
        return this.set(new Vector3d(pos.x, pos.y, pos.z), camera.getLookVector(), camera.getUpVector());
    }

    @Override
    public void store(ByteBuffer buffer) {
        // Layout read by BeamLightRenderer: world-to-light matrix, color, then radius/length/softness
        this.matrix.identity().rotation(this.orientation).translate(this.position).getFloats(buffer.position(), buffer);
        buffer.position(buffer.position() + Float.BYTES * 16);

        buffer.putFloat(this.color.red() * this.brightness);
        buffer.putFloat(this.color.green() * this.brightness);
        buffer.putFloat(this.color.blue() * this.brightness);

        buffer.putFloat(this.radius);
        buffer.putFloat(this.length);
        buffer.putFloat(this.softness);
    }

    @Override
    public boolean isVisible(CullFrustum frustum) {
        Vector3f direction = this.orientation.transformInverse(new Vector3f(0, 0, 1));
        double endX = this.position.x + direction.x * this.length;
        double endY = this.position.y + direction.y * this.length;
        double endZ = this.position.z + direction.z * this.length;
        return frustum.testAab(
                new Vector3d(Math.min(this.position.x, endX) - this.radius, Math.min(this.position.y, endY) - this.radius, Math.min(this.position.z, endZ) - this.radius),
                new Vector3d(Math.max(this.position.x, endX) + this.radius, Math.max(this.position.y, endY) + this.radius, Math.max(this.position.z, endZ) + this.radius));
    }
}
