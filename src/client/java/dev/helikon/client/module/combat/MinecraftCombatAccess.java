package dev.helikon.client.module.combat;

import dev.helikon.client.combat.CombatAim;
import dev.helikon.client.combat.CombatEntityType;
import dev.helikon.client.combat.CombatTarget;
import dev.helikon.client.combat.CombatTargetTracker;
import dev.helikon.client.combat.PotionCandidate;
import dev.helikon.client.friend.FriendManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.SplashPotionItem;
import net.minecraft.world.item.ThrowablePotionItem;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Narrow 26.2 bridge for ordinary attacks, held-potion use, and local aim rotation. */
public final class MinecraftCombatAccess {
    /** Version-bound local observations shared across the independently guarded combat module callbacks. */
    public record Snapshot(boolean available, List<CombatTarget> targets, Map<String, LivingEntity> entities,
                           CombatTarget crosshairTarget) {
        public Snapshot {
            targets = List.copyOf(targets);
            entities = Map.copyOf(entities);
        }

        public static Snapshot unavailable() {
            return new Snapshot(false, List.of(), Map.of(), null);
        }
    }

    private MinecraftCombatAccess() {
    }

    /** Reads current local entities once; no decision or state change occurs here. */
    public static Snapshot observe(FriendManager friends, AntiBot antiBot) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) {
            return Snapshot.unavailable();
        }
        LocalPlayer player = client.player;
        Map<String, Integer> nameCounts = playerNameCounts(client);
        Map<String, LivingEntity> entities = new LinkedHashMap<>();
        List<CombatTarget> targets = observedTargets(client, player, friends, antiBot, nameCounts, entities);
        CombatTarget crosshairTarget = crosshairTarget(client, targets);
        return new Snapshot(true, targets, entities, crosshairTarget);
    }

    public static void observeTarget(TargetHud targetHud, Snapshot snapshot, CombatTargetTracker tracker) {
        if (!snapshot.available()) {
            tracker.clear();
            return;
        }
        if (targetHud.isEnabled() && snapshot.crosshairTarget() != null) {
            tracker.observe(snapshot.crosshairTarget());
        }
        tracker.clearIfAbsent(snapshot.targets());
    }

    public static void tickAutoPotion(long tick, AutoPotion autoPotion) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) {
            autoPotion.onPlayerUnavailable();
            return;
        }
        tickAutoPotion(tick, client, autoPotion, client.gui.screen() != null);
    }

    public static void tickBowAim(BowAimAssist bowAim, Snapshot snapshot) {
        Minecraft client = Minecraft.getInstance();
        if (!snapshot.available() || client.player == null) {
            bowAim.onContextLost();
            return;
        }
        tickBowAim(client, bowAim, snapshot.targets(), client.gui.screen() != null);
    }

    public static boolean tickTriggerBot(long tick, TriggerBot triggerBot, Snapshot snapshot, CombatTargetTracker tracker) {
        Minecraft client = Minecraft.getInstance();
        if (!readyForAttack(client, snapshot) || snapshot.crosshairTarget() == null) {
            return false;
        }
        LocalPlayer player = client.player;
        if (triggerBot.shouldAttack(tick, snapshot.crosshairTarget(), attackReady(player), isMeleeWeapon(player.getMainHandItem()))) {
            return attack(client, snapshot.entities().get(snapshot.crosshairTarget().id()), snapshot.crosshairTarget(), tracker);
        }
        return false;
    }

    public static boolean tickCriticalAssist(long tick, CriticalAssist criticalAssist, Snapshot snapshot,
                                             CombatTargetTracker tracker) {
        Minecraft client = Minecraft.getInstance();
        if (!readyForAttack(client, snapshot) || snapshot.crosshairTarget() == null) {
            return false;
        }
        LocalPlayer player = client.player;
        CriticalAssist.Context context = new CriticalAssist.Context(client.options.keyAttack.isDown(), attackReady(player),
                player.onGround(), player.isInWater() || player.isInLava(), player.onClimbable(), player.isFallFlying(),
                player.fallDistance, player.getDeltaMovement().y);
        if (criticalAssist.shouldAttack(tick, snapshot.crosshairTarget(), context)) {
            return attack(client, snapshot.entities().get(snapshot.crosshairTarget().id()), snapshot.crosshairTarget(), tracker);
        }
        return false;
    }

    public static boolean tickKillAura(long tick, KillAura killAura, Snapshot snapshot, CombatTargetTracker tracker) {
        Minecraft client = Minecraft.getInstance();
        if (!readyForAttack(client, snapshot)) {
            return false;
        }
        List<CombatTarget> targets = killAura.nextAttacks(tick, snapshot.targets(), attackReady(client.player));
        if (targets.isEmpty()) {
            return false;
        }
        CombatAim.Rotation rotation = killAura.rotateToward(targets.getFirst(),
                new CombatAim.Rotation(client.player.getYRot(), client.player.getXRot()));
        client.player.setYRot(rotation.yaw());
        client.player.setXRot(rotation.pitch());
        boolean attacked = false;
        for (CombatTarget target : targets) {
            attacked |= attack(client, snapshot.entities().get(target.id()), target, tracker);
        }
        return attacked;
    }

    private static boolean readyForAttack(Minecraft client, Snapshot snapshot) {
        return snapshot.available() && client.player != null && client.gameMode != null && client.gui.screen() == null;
    }

    private static boolean attackReady(LocalPlayer player) {
        return player.getAttackStrengthScale(0.0F) >= 0.9F;
    }

    private static void tickAutoPotion(long tick, Minecraft client, AutoPotion autoPotion, boolean screenOpen) {
        LocalPlayer player = client.player;
        AutoPotion.Action action = autoPotion.update(tick, new AutoPotion.Context(player.getInventory().getSelectedSlot(),
                player.getHealth(), screenOpen, player.isUsingItem(), potionCandidates(player)));
        switch (action.type()) {
            case SELECT_AND_USE -> {
                player.getInventory().setSelectedSlot(action.slot());
                if (client.gameMode != null && !screenOpen) {
                    client.gameMode.useItem(player, net.minecraft.world.InteractionHand.MAIN_HAND);
                }
            }
            case USE_SELECTED -> {
                if (client.gameMode != null && !screenOpen) {
                    client.gameMode.useItem(player, net.minecraft.world.InteractionHand.MAIN_HAND);
                }
            }
            case RESTORE_SLOT -> player.getInventory().setSelectedSlot(action.slot());
            case NONE -> {
                // No local selection or normal use request is needed this tick.
            }
        }
    }

    private static void tickBowAim(Minecraft client, BowAimAssist bowAim, List<CombatTarget> targets, boolean screenOpen) {
        ItemStack held = client.player.getMainHandItem();
        if (screenOpen || !(held.getItem() instanceof BowItem) || !client.options.keyUse.isDown()) {
            bowAim.nextRotation(List.of(), new CombatAim.Rotation(client.player.getYRot(), client.player.getXRot()));
            return;
        }
        bowAim.nextRotation(targets, new CombatAim.Rotation(client.player.getYRot(), client.player.getXRot())).ifPresent(rotation -> {
            client.player.setYRot(rotation.yaw());
            client.player.setXRot(rotation.pitch());
        });
    }

    private static boolean attack(Minecraft client, LivingEntity entity, CombatTarget target, CombatTargetTracker tracker) {
        if (entity == null || entity.isRemoved() || !entity.isAlive() || !client.player.hasLineOfSight(entity)) {
            return false;
        }
        client.gameMode.attack(client.player, entity);
        tracker.recordAttack(target);
        return true;
    }

    private static List<CombatTarget> observedTargets(Minecraft client, LocalPlayer localPlayer, FriendManager friends,
                                                       AntiBot antiBot, Map<String, Integer> nameCounts,
                                                       Map<String, LivingEntity> entities) {
        List<CombatTarget> targets = new ArrayList<>();
        for (Entity entity : client.level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity living) || entity == localPlayer) {
                continue;
            }
            CombatEntityType type = entityType(living);
            if (type == null) {
                continue;
            }
            boolean isPlayer = living instanceof Player;
            String profileName = isPlayer ? playerName((Player) living) : "";
            boolean hasProfile = !isPlayer || !profileName.isBlank();
            String name = isPlayer && !hasProfile ? "unknown" : (isPlayer ? profileName : displayName(living));
            boolean suspectedBot = isPlayer && antiBot.isSuspected(new AntiBot.Facts(
                    client.getConnection() != null && client.getConnection().getPlayerInfo(living.getUUID()) != null,
                    living.isRemoved() || living.level() != client.level,
                    Math.max(0, living.tickCount),
                    nameCounts.getOrDefault(name.toLowerCase(Locale.ROOT), 0) > 1,
                    living.isInvisible(), hasProfile
            ));
            boolean friend = isPlayer && hasProfile && friends.contains(profileName);
            CombatTarget target = toTarget(localPlayer, living, name, type, friend, suspectedBot);
            targets.add(target);
            entities.put(target.id(), living);
        }
        return List.copyOf(targets);
    }

    private static CombatTarget crosshairTarget(Minecraft client, List<CombatTarget> targets) {
        if (!(client.hitResult instanceof EntityHitResult hit)) {
            return null;
        }
        String id = hit.getEntity().getUUID().toString();
        return targets.stream().filter(target -> target.id().equals(id)).findFirst().orElse(null);
    }

    private static CombatTarget toTarget(LocalPlayer localPlayer, LivingEntity entity, String name, CombatEntityType type,
                                         boolean friend, boolean suspectedBot) {
        Vec3 eye = localPlayer.getEyePosition();
        double targetX = entity.getX();
        double targetY = entity.getY() + entity.getBbHeight() * 0.5D;
        double targetZ = entity.getZ();
        double x = targetX - eye.x;
        double y = targetY - eye.y;
        double z = targetZ - eye.z;
        double distance = Math.sqrt(x * x + y * y + z * z);
        Vec3 view = localPlayer.getViewVector(1.0F);
        double angle = distance == 0.0D ? 0.0D : Math.toDegrees(Math.acos(Math.max(-1.0D, Math.min(1.0D,
                (view.x * x + view.y * y + view.z * z) / distance))));
        Vec3 velocity = entity.getDeltaMovement();
        List<String> effects = new ArrayList<>();
        for (MobEffectInstance effect : entity.getActiveEffects()) {
            effects.add(effect.getEffect().value().getDescriptionId());
            if (effects.size() == 4) {
                break;
            }
        }
        return new CombatTarget(entity.getUUID().toString(), name, type, friend, suspectedBot, entity.isAlive(),
                !entity.isInvisible(), localPlayer.hasLineOfSight(entity), distance, angle, x, y, z, velocity.x, velocity.y,
                velocity.z, entity.getHealth(), entity.getArmorValue(), itemId(entity.getMainHandItem()), effects);
    }

    private static Map<String, Integer> playerNameCounts(Minecraft client) {
        Map<String, Integer> counts = new HashMap<>();
        for (Entity entity : client.level.entitiesForRendering()) {
            if (entity instanceof Player player) {
                String rawName = playerName(player);
                String name = (rawName.isBlank() ? "unknown" : rawName).toLowerCase(Locale.ROOT);
                counts.merge(name, 1, Integer::sum);
            }
        }
        return counts;
    }

    private static List<PotionCandidate> potionCandidates(LocalPlayer player) {
        List<PotionCandidate> candidates = new ArrayList<>();
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            PotionCandidate.Kind kind;
            if (stack.getItem() instanceof SplashPotionItem) {
                kind = PotionCandidate.Kind.SPLASH;
            } else if (stack.getItem() instanceof PotionItem && !(stack.getItem() instanceof ThrowablePotionItem)) {
                kind = PotionCandidate.Kind.DRINK;
            } else {
                continue;
            }
            PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
            if (contents == null) {
                continue;
            }
            String potionId = contents.potion().map(holder -> holder.value().name()).orElse("");
            boolean restorative = false;
            for (MobEffectInstance effect : contents.getAllEffects()) {
                if (effect.getEffect().equals(MobEffects.INSTANT_HEALTH)) {
                    restorative = true;
                    break;
                }
            }
            if (!potionId.isBlank()) {
                candidates.add(new PotionCandidate(slot, potionId, kind, restorative));
            }
        }
        return List.copyOf(candidates);
    }

    private static boolean isMeleeWeapon(ItemStack stack) {
        String id = itemId(stack);
        return id.endsWith("_sword") || id.endsWith("_axe") || id.endsWith("_mace") || id.endsWith("trident");
    }

    private static CombatEntityType entityType(LivingEntity entity) {
        if (entity instanceof Player) {
            return CombatEntityType.PLAYER;
        }
        if (entity instanceof Monster) {
            return CombatEntityType.HOSTILE;
        }
        return CombatEntityType.PASSIVE;
    }

    private static String displayName(LivingEntity entity) {
        if (entity instanceof Player player) {
            String name = playerName(player);
            return name.isBlank() ? "unknown" : name;
        }
        return entity.getName().getString();
    }

    private static String playerName(Player player) {
        String name = player.getGameProfile().name();
        return name == null ? "" : name.trim();
    }

    private static String itemId(ItemStack stack) {
        return stack.isEmpty() ? "empty" : BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }
}
