package dev.helikon.client.module.movement;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec3;

import java.util.Objects;

/** Local-only detached camera bridge; it never moves the player or constructs network traffic. */
public final class FreecamAccess {
    private static volatile Freecam freecam;
    private static ArmorStand camera;
    private static Input capturedInput = new Input(false, false, false, false, false, false, false);

    private FreecamAccess() {
    }

    public static void install(Freecam module) {
        freecam = Objects.requireNonNull(module, "module");
    }

    /** Captures local keys for the camera and suppresses player movement while detached. */
    public static Input captureAndSuppress(Input input) {
        Objects.requireNonNull(input, "input");
        if (!isActive()) {
            return input;
        }
        capturedInput = input;
        return new Input(false, false, false, false, false, false, input.sprint());
    }

    /** Uses the verified normal mouse-turn path on the local-only camera while freecam is active. */
    public static void turn(LocalPlayer player, double deltaX, double deltaY) {
        if (camera != null && isActive()) {
            camera.turn(deltaX, deltaY);
            return;
        }
        player.turn(deltaX, deltaY);
    }

    public static void tick(Minecraft client) {
        Objects.requireNonNull(client, "client");
        if (!isActive() || client.player == null || client.level == null) {
            stop(client);
            return;
        }
        if (camera == null || camera.level() != client.level) {
            camera = new ArmorStand(client.level, client.player.getX(), client.player.getEyeY(), client.player.getZ());
            camera.noPhysics = true;
            camera.setNoGravity(true);
            camera.setInvisible(true);
            camera.setYRot(client.player.getYRot());
            camera.setXRot(client.player.getXRot());
        }
        // The camera entity is never added to the level, so nothing ticks it: without
        // this per-tick old-pose sync the renderer interpolates from the constructor
        // default (0,0,0) and the camera flickers wildly.
        camera.setOldPosAndRot();
        if (client.getCameraEntity() != camera) {
            client.setCameraEntity(camera);
        }
        moveCamera();
    }

    public static boolean isActive() {
        Freecam current = freecam;
        return current != null && current.isEnabled();
    }

    private static void moveCamera() {
        if (camera == null) {
            return;
        }
        Freecam current = freecam;
        double speed = current.speed();
        Vec3 forward = camera.getViewVector(1.0F);
        Vec3 horizontalForward = new Vec3(forward.x, 0.0D, forward.z);
        if (horizontalForward.lengthSqr() > 0.0D) {
            horizontalForward = horizontalForward.normalize();
        }
        Vec3 side = new Vec3(-horizontalForward.z, 0.0D, horizontalForward.x);
        double forwardInput = (capturedInput.forward() ? 1.0D : 0.0D) - (capturedInput.backward() ? 1.0D : 0.0D);
        double sideInput = (capturedInput.left() ? 1.0D : 0.0D) - (capturedInput.right() ? 1.0D : 0.0D);
        double vertical = (capturedInput.jump() ? 1.0D : 0.0D) - (capturedInput.shift() ? 1.0D : 0.0D);
        Vec3 movement = horizontalForward.scale(forwardInput * speed).add(side.scale(sideInput * speed))
                .add(0.0D, vertical * speed, 0.0D);
        if (movement.lengthSqr() > 0.0D) {
            camera.setPos(camera.position().add(movement));
        }
    }

    /** Restores the normal camera when possible and always discards the unadded local camera entity. */
    public static void stop(Minecraft client) {
        Objects.requireNonNull(client, "client");
        if (camera != null && client.getCameraEntity() == camera) {
            client.setCameraEntity(client.player);
        }
        if (camera != null) {
            camera.discard();
        }
        camera = null;
        capturedInput = new Input(false, false, false, false, false, false, false);
    }
}
