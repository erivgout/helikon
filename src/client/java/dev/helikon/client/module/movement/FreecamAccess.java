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
        Freecam current = freecam;
        if (camera != null && current != null && current.isEnabled()) {
            Freecam.Rotation rotation = current.turn(camera.getYRot(), camera.getXRot(), deltaX, deltaY);
            synchronizeCameraRotation(rotation.yaw(), rotation.pitch());
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
            synchronizeCameraRotation(client.player.getYRot(), client.player.getXRot());
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
        double forwardInput = (capturedInput.forward() ? 1.0D : 0.0D) - (capturedInput.backward() ? 1.0D : 0.0D);
        double sideInput = (capturedInput.left() ? 1.0D : 0.0D) - (capturedInput.right() ? 1.0D : 0.0D);
        double vertical = (capturedInput.jump() ? 1.0D : 0.0D) - (capturedInput.shift() ? 1.0D : 0.0D);
        HorizontalVelocity horizontal = MovementDirections.fromView(
                horizontalForward.x, horizontalForward.z, sideInput, forwardInput).scale(speed);
        Vec3 movement = new Vec3(horizontal.x(), vertical * speed, horizontal.z());
        if (movement.lengthSqr() > 0.0D) {
            camera.setPos(camera.position().add(movement));
        }
    }

    /**
     * ArmorStand inherits LivingEntity.getViewYRot, which renders from head
     * yaw rather than Entity yaw. Keep every current/previous head, body, and
     * entity rotation aligned so frame interpolation cannot fight mouse input.
     */
    private static void synchronizeCameraRotation(float yaw, float pitch) {
        if (camera == null) {
            return;
        }
        camera.setYRot(yaw);
        camera.setXRot(pitch);
        camera.setYHeadRot(yaw);
        camera.setYBodyRot(yaw);
        camera.yRotO = yaw;
        camera.xRotO = pitch;
        camera.yHeadRotO = yaw;
        camera.yBodyRotO = yaw;
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
