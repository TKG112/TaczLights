package net.tkg.taczlights.client.light;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.client.model.bedrock.BedrockPart;
import foundry.veil.api.client.render.light.data.PointLightData;
import foundry.veil.api.client.render.light.data.SpotLightData;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import net.tkg.taczlights.TaczLightsConfig;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Turns attachment renders into Veil lights.
 * <p>
 * While a frame renders, {@link #capture} records where every lit attachment was drawn, in world space.
 * At the start of the next frame, {@link #flush} moves a pool of Veil light handles onto those positions and frees
 * the handles that are no longer needed. The lights therefore trail the models by one frame.
 */
public final class AttachmentLightTracker {
    /** Captures closer than this (squared, in blocks) with the same definition are treated as the same light drawn twice. */
    private static final double DUPLICATE_DISTANCE_SQR = 0.05 * 0.05;

    private static final List<Capture> CAPTURES = new ArrayList<>();
    private static final LightPool<SpotLightData> SPOT_LIGHTS = new LightPool<>(SpotLightData::new);
    private static final LightPool<PointLightData> POINT_LIGHTS = new LightPool<>(PointLightData::new);
    private static final LightPool<SpotLightData> IR_SPOT_LIGHTS = new LightPool<>(SpotLightData::new, IRLightBridge::addLight);
    private static final LightPool<PointLightData> IR_POINT_LIGHTS = new LightPool<>(PointLightData::new, IRLightBridge::addLight);

    private AttachmentLightTracker() {
    }

    /**
     * Records a light for the attachment currently being rendered.
     *
     * @param poseStack The attachment's pose stack, positioned at the model root
     * @param bonePath  The bones from the model root down to the light's bone
     */
    public static void capture(AttachmentLight light, ItemDisplayContext context, PoseStack poseStack, List<BedrockPart> bonePath) {
        boolean enabled = switch (context) {
            case FIRST_PERSON_LEFT_HAND, FIRST_PERSON_RIGHT_HAND -> light.firstPerson();
            case THIRD_PERSON_LEFT_HAND, THIRD_PERSON_RIGHT_HAND -> light.thirdPerson();
            // GUI, dropped items, item frames and so on: not held by anyone
            default -> false;
        };
        if (!enabled) {
            return;
        }

        poseStack.pushPose();
        for (BedrockPart part : bonePath) {
            part.translateAndRotateAndScale(poseStack);
        }
        // Model-view times pose gives view space for both the first person hand pass and the level's entity pass
        Matrix4f toView = new Matrix4f(RenderSystem.getModelViewMatrix()).mul(poseStack.last().pose());
        poseStack.popPose();

        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        Quaternionf viewToWorld = camera.rotation();
        Vec3 cameraPos = camera.getPosition();

        Vector3f offset = viewToWorld.transform(toView.transformPosition(new Vector3f()));
        Vector3f forward = viewToWorld.transform(toView.transformDirection(new Vector3f(0, 0, -1))).normalize();
        Vector3f up = viewToWorld.transform(toView.transformDirection(new Vector3f(0, 1, 0))).normalize();
        Vector3d position = new Vector3d(cameraPos.x + offset.x, cameraPos.y + offset.y, cameraPos.z + offset.z);

        for (Capture existing : CAPTURES) {
            if (existing.light == light && existing.position.distanceSquared(position) < DUPLICATE_DISTANCE_SQR) {
                return;
            }
        }
        CAPTURES.add(new Capture(light, position, forward, up));
    }

    /**
     * Applies the captures of the last frame to Veil's light renderer.
     */
    public static void flush() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || !TaczLightsConfig.ENABLED.get()) {
            CAPTURES.clear();
            clear();
            return;
        }

        Vec3 cameraPos = minecraft.gameRenderer.getMainCamera().getPosition();
        Vector3d camera = new Vector3d(cameraPos.x, cameraPos.y, cameraPos.z);
        CAPTURES.sort(Comparator.comparingDouble(capture -> capture.position.distanceSquared(camera)));
        int budget = TaczLightsConfig.MAX_LIGHTS.get();

        int spots = 0;
        int points = 0;
        int irSpots = 0;
        int irPoints = 0;
        boolean irAvailable = IRLightBridge.isAvailable();
        int used = 0;
        for (int i = 0; i < CAPTURES.size() && used < budget; i++) {
            Capture capture = CAPTURES.get(i);
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
        SPOT_LIGHTS.trim(spots);
        POINT_LIGHTS.trim(points);
        IR_SPOT_LIGHTS.trim(irSpots);
        IR_POINT_LIGHTS.trim(irPoints);
        CAPTURES.clear();
    }

    /**
     * Removes every attachment light.
     */
    public static void clear() {
        SPOT_LIGHTS.trim(0);
        POINT_LIGHTS.trim(0);
        IR_SPOT_LIGHTS.trim(0);
        IR_POINT_LIGHTS.trim(0);
    }

    private static void applySpot(Capture capture, SpotLightData data) {
        AttachmentLight light = capture.light;
        data.getPositionMutable().set(capture.position);
        // Same convention as SpotLightData#setTo(Camera)
        data.getOrientationMutable().identity().lookAlong(new Vector3f(capture.forward).negate(), capture.up);
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
        data.setPosition(capture.position)
                .setColor(light.color())
                .setBrightness(light.brightness())
                .setRadius(light.radius())
                .setOcclusionEnabled(light.occlusion())
                .setInscatteringStrength(light.inscattering());
    }

    private record Capture(AttachmentLight light, Vector3d position, Vector3f forward, Vector3f up) {
    }
}
