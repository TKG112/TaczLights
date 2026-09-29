package net.tkg.taczlights.client.light;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.client.model.bedrock.BedrockPart;
import foundry.veil.api.client.render.light.data.PointLightData;
import foundry.veil.api.client.render.light.data.SpotLightData;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.tkg.taczlights.TaczLightsConfig;
import net.tkg.taczlights.client.beam.BeamLightData;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Turns attachment renders into Veil lights.
 * <p>
 * While a frame renders, {@link #capture} records where every lit attachment was drawn, and {@link #captureLaser}
 * every laser that has a {@code laser_light}. Early in the next frame's level render, {@link #flush} moves a pool of
 * Veil light handles onto those positions and frees the handles no longer needed.
 * <p>
 * Third person captures are stored in world space, so they trail their model by a frame. First person captures are
 * stored relative to the camera and placed with the current camera when flushed, since the gun moves with it: that
 * keeps them from lagging behind when turning.
 */
public final class AttachmentLightTracker {
    /** Captures closer than this (squared, in blocks) with the same definition are treated as the same light drawn twice. */
    private static final double DUPLICATE_DISTANCE_SQR = 0.05 * 0.05;

    /** How far past the surface it hits a beam light reaches, in blocks. */
    private static final double BEAM_SURFACE_MARGIN = 0.25;

    private static final List<Capture> CAPTURES = new ArrayList<>();
    private static final List<LaserCapture> LASER_CAPTURES = new ArrayList<>();

    private static final LightPool<SpotLightData> SPOT_LIGHTS = new LightPool<>(SpotLightData::new);
    private static final LightPool<PointLightData> POINT_LIGHTS = new LightPool<>(PointLightData::new);
    private static final LightPool<SpotLightData> IR_SPOT_LIGHTS = new LightPool<>(SpotLightData::new, IRLightBridge::addLight);
    private static final LightPool<PointLightData> IR_POINT_LIGHTS = new LightPool<>(PointLightData::new, IRLightBridge::addLight);
    private static final LightPool<BeamLightData> BEAM_LIGHTS = new LightPool<>(BeamLightData::new);
    private static final LightPool<BeamLightData> IR_BEAM_LIGHTS = new LightPool<>(BeamLightData::new, IRLightBridge::addLight);

    /** The projection the level is drawn with, captured each frame. Null until the first level render. */
    @Nullable
    private static Matrix4f worldProjection;

    private AttachmentLightTracker() {
    }

    /**
     * Records a light for the attachment currently being rendered.
     *
     * @param poseStack The attachment's pose stack, positioned at the model root
     * @param bonePath  The bones from the model root down to the light's bone
     */
    public static void capture(AttachmentLight light, ItemDisplayContext context, PoseStack poseStack, List<BedrockPart> bonePath) {
        if (!isEnabled(context, light.firstPerson(), light.thirdPerson())) {
            return;
        }
        Placement placement = place(poseStack, bonePath, context.firstPerson());
        for (Capture existing : CAPTURES) {
            if (existing.light == light && existing.placement.isNear(placement)) {
                return;
            }
        }
        CAPTURES.add(new Capture(light, placement));
    }

    /**
     * Records the light of a laser TaCZ is about to draw.
     *
     * @param poseStack The pose stack TaCZ draws the beam with
     * @param bonePath  The bones from the model root down to the {@code laser_beam} bone
     * @param color     The laser's color, used unless the light overrides it
     * @param length    The laser's length, used unless the light overrides it
     * @param ir        Whether the laser is in its infrared setting
     */
    public static void captureLaser(LaserLight light, ItemDisplayContext context, PoseStack poseStack, List<BedrockPart> bonePath,
                                    int color, float length, boolean ir) {
        if (!isEnabled(context, light.firstPerson(), light.thirdPerson())) {
            return;
        }
        Placement placement = place(poseStack, bonePath, context.firstPerson());
        for (LaserCapture existing : LASER_CAPTURES) {
            if (existing.light == light && existing.placement.isNear(placement)) {
                return;
            }
        }
        LASER_CAPTURES.add(new LaserCapture(light, placement, light.color().orElse(color), light.distance().orElse(length), ir, context.firstPerson()));
    }

    private static boolean isEnabled(ItemDisplayContext context, boolean firstPerson, boolean thirdPerson) {
        return switch (context) {
            case FIRST_PERSON_LEFT_HAND, FIRST_PERSON_RIGHT_HAND -> firstPerson;
            case THIRD_PERSON_LEFT_HAND, THIRD_PERSON_RIGHT_HAND -> thirdPerson;
            // GUI, dropped items, item frames and so on: not held by anyone
            default -> false;
        };
    }

    /**
     * @return Where the end of the bone path is and which way its -Z axis points: relative to the camera in first
     * person, in the world otherwise
     */
    private static Placement place(PoseStack poseStack, List<BedrockPart> bonePath, boolean firstPerson) {
        poseStack.pushPose();
        for (BedrockPart part : bonePath) {
            part.translateAndRotateAndScale(poseStack);
        }
        // Model-view times pose gives view space for both the first person hand pass and the level's entity pass
        Matrix4f toView = new Matrix4f(RenderSystem.getModelViewMatrix()).mul(poseStack.last().pose());
        poseStack.popPose();

        Vector3f origin = toView.transformPosition(new Vector3f());
        Vector3f ahead = toView.transformPosition(new Vector3f(0, 0, -1));
        Vector3f above = toView.transformPosition(new Vector3f(0, 1, 0));
        if (firstPerson) {
            matchWorldFov(origin);
            matchWorldFov(ahead);
            matchWorldFov(above);
        }
        Vector3f forward = ahead.sub(origin).normalize();
        Vector3f up = above.sub(origin).normalize();

        Placement viewSpace = new Placement(new Vector3d(origin), forward, up, true);
        // The gun moves with the camera in first person, so it's placed with the camera it's flushed with. Everything
        // else is placed in the world now, with the camera it was drawn with
        return firstPerson ? viewSpace : viewSpace.resolve(Minecraft.getInstance().gameRenderer.getMainCamera());
    }

    /**
     * Moves a view space point from the first person hand pass to where it shows up on screen in the world.
     * <p>
     * The hand is drawn with its own field of view, so a point on the gun only lines up with the world at the center
     * of the screen. Both projections share the same near and far planes, so the point keeps its depth and only its
     * sideways and vertical offsets scale by the ratio of the two. The scaling is linear, so a line along the gun
     * (like a laser) stays a line that covers the same pixels.
     */
    private static void matchWorldFov(Vector3f viewPos) {
        if (worldProjection == null) {
            return;
        }
        Matrix4f handProjection = RenderSystem.getProjectionMatrix();
        viewPos.x *= handProjection.m00() / worldProjection.m00();
        viewPos.y *= handProjection.m11() / worldProjection.m11();
    }

    /**
     * Records the projection the level is drawn with this frame, for {@link #matchWorldFov}.
     */
    public static void setWorldProjection(Matrix4fc projection) {
        if (worldProjection == null) {
            worldProjection = new Matrix4f();
        }
        worldProjection.set(projection);
    }

    /**
     * Applies the captures of the last frame to Veil's light renderer. Call once the camera is set up for the frame,
     * before the level's lights are drawn.
     */
    public static void flush() {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || !TaczLightsConfig.ENABLED.get()) {
            CAPTURES.clear();
            LASER_CAPTURES.clear();
            clear();
            return;
        }

        Camera camera = minecraft.gameRenderer.getMainCamera();
        Vec3 cameraPos = camera.getPosition();
        Vector3d cameraPosition = new Vector3d(cameraPos.x, cameraPos.y, cameraPos.z);
        List<Capture> captures = new ArrayList<>(CAPTURES.size());
        for (Capture capture : CAPTURES) {
            captures.add(new Capture(capture.light, capture.placement.resolve(camera)));
        }
        List<LaserCapture> lasers = new ArrayList<>(LASER_CAPTURES.size());
        for (LaserCapture capture : LASER_CAPTURES) {
            Placement placement = capture.placement.resolve(camera);
            if (capture.firstPerson) {
                placement = centerOnScreen(camera, placement);
            }
            lasers.add(new LaserCapture(capture.light, placement, capture.color, capture.length, capture.ir, capture.firstPerson));
        }
        CAPTURES.clear();
        LASER_CAPTURES.clear();
        captures.sort(Comparator.comparingDouble(capture -> capture.placement.position.distanceSquared(cameraPosition)));
        lasers.sort(Comparator.comparingDouble(capture -> capture.placement.position.distanceSquared(cameraPosition)));
        int budget = TaczLightsConfig.MAX_LIGHTS.get();

        int spots = 0;
        int points = 0;
        int irSpots = 0;
        int irPoints = 0;
        boolean irAvailable = IRLightBridge.isAvailable();
        int used = 0;
        for (int i = 0; i < captures.size() && used < budget; i++) {
            Capture capture = captures.get(i);
            if (capture.light.ir()) {
                // Nothing could see it without ModernMayhem's night vision, so don't create it
                if (!irAvailable) {
                    continue;
                }
                switch (capture.light.type()) {
                    case SPOT -> applySpot(capture, IR_SPOT_LIGHTS.get(irSpots++));
                    case POINT -> applyPoint(capture, IR_POINT_LIGHTS.get(irPoints++));
                }
            } else {
                switch (capture.light.type()) {
                    case SPOT -> applySpot(capture, SPOT_LIGHTS.get(spots++));
                    case POINT -> applyPoint(capture, POINT_LIGHTS.get(points++));
                }
            }
            used++;
        }

        int beams = 0;
        int irBeams = 0;
        for (int i = 0; i < lasers.size() && used < budget; i++) {
            LaserCapture capture = lasers.get(i);
            if (capture.ir && !irAvailable) {
                continue;
            }
            float hit = clipBeam(level, capture.placement, capture.length);
            applyBeamLight(capture, hit, capture.ir ? IR_BEAM_LIGHTS.get(irBeams++) : BEAM_LIGHTS.get(beams++));
            used++;
        }

        SPOT_LIGHTS.trim(spots);
        POINT_LIGHTS.trim(points);
        IR_SPOT_LIGHTS.trim(irSpots);
        IR_POINT_LIGHTS.trim(irPoints);
        BEAM_LIGHTS.trim(beams);
        IR_BEAM_LIGHTS.trim(irBeams);
    }

    /**
     * Removes every attachment light.
     */
    public static void clear() {
        SPOT_LIGHTS.trim(0);
        POINT_LIGHTS.trim(0);
        IR_SPOT_LIGHTS.trim(0);
        IR_POINT_LIGHTS.trim(0);
        BEAM_LIGHTS.trim(0);
        IR_BEAM_LIGHTS.trim(0);
    }

    private static void applySpot(Capture capture, SpotLightData data) {
        AttachmentLight light = capture.light;
        Placement placement = capture.placement;
        data.getPositionMutable().set(placement.position);
        // Same convention as SpotLightData#setTo(Camera)
        data.getOrientationMutable().identity().lookAlong(new Vector3f(placement.forward).negate(), placement.up);
        data.setColor(light.color())
                .setBrightness(light.brightness())
                .setDistance(light.distance())
                // Veil's spot "size" is the cone angle and its "angle" is the soft edge inside it
                .setSize((float) Math.toRadians(light.angle()))
                .setAngle((float) Math.toRadians(Math.min(light.falloff(), light.angle())))
                .setOcclusionEnabled(light.occlusion())
                .setInscatteringStrength(light.inscattering());
    }

    private static void applyPoint(Capture capture, PointLightData data) {
        AttachmentLight light = capture.light;
        data.setPosition(capture.placement.position)
                .setColor(light.color())
                .setBrightness(light.brightness())
                .setRadius(light.radius())
                .setOcclusionEnabled(light.occlusion())
                .setInscatteringStrength(light.inscattering());
    }

    /**
     * @param hit How far along the laser it hits a block, or its full length if it hits nothing
     */
    private static void applyBeamLight(LaserCapture capture, float hit, BeamLightData data) {
        Placement placement = capture.placement;
        // Reach slightly past the surface, so the whole of it lies inside the beam
        float end = (float) Math.min(capture.length, hit + BEAM_SURFACE_MARGIN);
        // Start a little ahead, so the gun itself isn't lit, but never past the surface the beam hits
        float start = (float) Math.min(capture.startOffset(), Math.max(end - BEAM_SURFACE_MARGIN * 2, 0.0));
        Vector3d origin = new Vector3d(placement.forward).mul(start).add(placement.position);
        data.set(origin, placement.forward, placement.up)
                .setLength(end - start)
                .setRadius(capture.light.radius())
                .setSoftness(capture.light.softness())
                .setColor(capture.color)
                .setBrightness(capture.light.brightness());
    }

    /**
     * Moves a first person laser to the center of the screen, keeping the direction its bone points.
     * <p>
     * TaCZ's first person laser lines up with the crosshair, but its bone sits below it: near a wall the bone's own
     * line would land visibly low. Starting from the camera keeps the dot on the crosshair, and the bone's direction
     * still carries the gun's inspect and other animations.
     */
    private static Placement centerOnScreen(Camera camera, Placement placement) {
        Vec3 cameraPos = camera.getPosition();
        return new Placement(new Vector3d(cameraPos.x, cameraPos.y, cameraPos.z), placement.forward, placement.up, false);
    }

    /**
     * @return How far along the laser the first block stops it, so its light can't reach through walls
     */
    private static float clipBeam(ClientLevel level, Placement placement, float length) {
        Vec3 from = new Vec3(placement.position.x, placement.position.y, placement.position.z);
        Vec3 to = from.add(placement.forward.x * length, placement.forward.y * length, placement.forward.z * length);
        BlockHitResult hit = level.clip(new ClipContext(from, to, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, CollisionContext.empty()));
        if (hit.getType() == HitResult.Type.MISS) {
            return length;
        }
        return (float) Math.min(length, hit.getLocation().distanceTo(from));
    }

    /**
     * @param cameraRelative Whether the position and directions are in view space, to be placed with the camera
     *                       when used, instead of in the world
     */
    private record Placement(Vector3d position, Vector3f forward, Vector3f up, boolean cameraRelative) {
        Placement resolve(Camera camera) {
            if (!this.cameraRelative) {
                return this;
            }
            Quaternionf viewToWorld = camera.rotation();
            Vec3 cameraPos = camera.getPosition();
            Vector3f offset = viewToWorld.transform(new Vector3f((float) this.position.x, (float) this.position.y, (float) this.position.z));
            return new Placement(
                    new Vector3d(cameraPos.x + offset.x, cameraPos.y + offset.y, cameraPos.z + offset.z),
                    viewToWorld.transform(new Vector3f(this.forward)),
                    viewToWorld.transform(new Vector3f(this.up)),
                    false);
        }

        boolean isNear(Placement other) {
            return this.cameraRelative == other.cameraRelative && this.position.distanceSquared(other.position) < DUPLICATE_DISTANCE_SQR;
        }
    }

    private record Capture(AttachmentLight light, Placement placement) {
    }

    /**
     * @param placement   The line the laser's light follows
     * @param firstPerson Whether it was drawn in first person, where its light comes from the center of the screen
     */
    private record LaserCapture(LaserLight light, Placement placement, int color, float length, boolean ir, boolean firstPerson) {
        /**
         * @return How far along the laser its light starts, so it doesn't light up the gun
         */
        float startOffset() {
            return this.firstPerson ? this.light.firstPersonOffset() : this.light.startOffset();
        }
    }
}
