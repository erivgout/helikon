package dev.helikon.client.module.miscellaneous;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Pose;

/** Minecraft-only adapter for the bounded legacy fun effects. */
public final class MinecraftLegacyFunAccess {
    private float savedYaw;
    private float savedPitch;
    private boolean ownsRotation;
    private boolean ownsPose;
    private boolean ownsNausea;

    public void tick(long tick, LegacyFunModules.Derp derp, LegacyFunModules.HeadRoll headRoll,
                     LegacyFunModules.Lsd lsd, LegacyFunModules.MileyCyrus mileyCyrus,
                     LegacyFunModules.Tired tired, LegacyFunModules.Headless headless) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) {
            reset();
            return;
        }

        boolean rotating = derp.isEnabled() || headRoll.isEnabled() || headless.isEnabled();
        if (rotating && !ownsRotation) {
            savedYaw = client.player.getYRot();
            savedPitch = client.player.getXRot();
            ownsRotation = true;
        }
        if (derp.isEnabled()) {
            LegacyFunModules.Rotation rotation = derp.rotation(tick);
            client.player.setYRot(rotation.yaw());
            client.player.setXRot(rotation.pitch());
        } else if (headRoll.isEnabled()) {
            LegacyFunModules.Rotation rotation = headRoll.rotation(tick, savedYaw);
            client.player.setYRot(rotation.yaw());
            client.player.setXRot(rotation.pitch());
        } else if (headless.isEnabled()) {
            client.player.setXRot(headless.displacedPitch());
        } else if (ownsRotation) {
            client.player.setYRot(savedYaw);
            client.player.setXRot(savedPitch);
            ownsRotation = false;
        }

        if (tired.isEnabled()) {
            client.player.setPose(Pose.SWIMMING);
            ownsPose = true;
        } else if (ownsPose) {
            client.player.setPose(Pose.STANDING);
            ownsPose = false;
        }

        if (lsd.isEnabled()) {
            MobEffectInstance current = client.player.getEffect(MobEffects.NAUSEA);
            if (current == null || current.getDuration() < 30) {
                client.player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, lsd.refreshTicks(), 0,
                        false, false, false));
                ownsNausea = true;
            }
        } else if (ownsNausea) {
            client.player.removeEffect(MobEffects.NAUSEA);
            ownsNausea = false;
        }

        if (mileyCyrus.shouldSwing(tick)) {
            client.player.swing(InteractionHand.MAIN_HAND);
            client.player.swing(InteractionHand.OFF_HAND);
        }
    }

    public void reset() {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            if (ownsRotation) {
                client.player.setYRot(savedYaw);
                client.player.setXRot(savedPitch);
            }
            if (ownsPose) {
                client.player.setPose(Pose.STANDING);
            }
            if (ownsNausea) {
                client.player.removeEffect(MobEffects.NAUSEA);
            }
        }
        ownsRotation = false;
        ownsPose = false;
        ownsNausea = false;
    }
}
