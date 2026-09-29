package net.tkg.taczlights.api;

import net.tkg.taczlights.client.beam.LaserBloomTarget;

/**
 * Lets another mod choose when laser beams' glow is added to the frame.
 * <p>
 * By default TaCZ Lights adds the glow of beams in the world at the end of the level render and the glow of first
 * person beams right after the hand. A mod with its own post effects over the finished frame (e.g. night vision or
 * thermal goggles) can instead call {@link #composite()} at the point the glow belongs, such as after its darkness and
 * before its goggle shaders, so the goggles process the glow like the rest of the scene. Once it has been called, TaCZ
 * Lights stops adding the glow on its own.
 * <p>
 * Client only, render thread only. Call it every frame from the end of {@code GameRenderer#renderLevel}, after the
 * first person hand has been drawn; both kinds of beams have been recorded by then. Check
 * {@code ModList.get().isLoaded("taczlights")} before touching this class.
 */
public final class LaserGlow {
    private static boolean compositedExternally;

    private LaserGlow() {
    }

    /**
     * Adds the glow of every laser beam drawn this frame to the main render target.
     */
    public static void composite() {
        compositedExternally = true;
        // Called after the first person hand, which the world's beams don't share depth with
        LaserBloomTarget.WORLD.composite(true);
        LaserBloomTarget.FIRST_PERSON.composite();
    }

    /**
     * @return Whether another mod adds the glow through {@link #composite()}, so TaCZ Lights shouldn't on its own
     */
    public static boolean isCompositedExternally() {
        return compositedExternally;
    }
}
