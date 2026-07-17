package dev.helikon.client.module.combat;

import dev.helikon.client.combat.CombatAim;
import dev.helikon.client.combat.CombatEntityType;
import dev.helikon.client.combat.CombatTarget;
import dev.helikon.client.combat.CombatTargetTracker;
import dev.helikon.client.combat.PotionCandidate;
import dev.helikon.client.friend.FriendManager;
import dev.helikon.client.mixin.MinecraftAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.SplashPotionItem;
import net.minecraft.world.item.ThrowablePotionItem;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

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
    public static Snapshot observe(FriendManager friends, AntiBot antiBot, TargetFilter targetFilter) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) {
            return Snapshot.unavailable();
        }
        LocalPlayer player = client.player;
        Map<String, Integer> nameCounts = playerNameCounts(client);
        Map<String, LivingEntity> entities = new LinkedHashMap<>();
        List<CombatTarget> targets = observedTargets(client, player, friends, antiBot, targetFilter, nameCounts, entities);
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

    public static void tickAutoPearl(long tick, AutoPearl autoPearl, Snapshot snapshot) {
        Minecraft client = Minecraft.getInstance();
        if (!snapshot.available() || client.player == null || client.gameMode == null) {
            autoPearl.onPlayerUnavailable();
            return;
        }
        LocalPlayer player = client.player;
        boolean screenOpen = client.gui.screen() != null;
        int pearlSlot = firstPearlHotbarSlot(player);
        boolean onCooldown = pearlSlot >= 0 && player.getCooldowns().isOnCooldown(player.getInventory().getItem(pearlSlot));
        AutoPearl.Action action = autoPearl.update(tick, new AutoPearl.Context(player.getInventory().getSelectedSlot(),
                pearlSlot, screenOpen, onCooldown, snapshot.targets()));
        switch (action.type()) {
            case SELECT_AND_THROW -> {
                player.getInventory().setSelectedSlot(action.slot());
                applyRotation(player, action);
                if (!screenOpen) {
                    client.gameMode.useItem(player, net.minecraft.world.InteractionHand.MAIN_HAND);
                }
            }
            case THROW_SELECTED -> {
                applyRotation(player, action);
                if (!screenOpen) {
                    client.gameMode.useItem(player, net.minecraft.world.InteractionHand.MAIN_HAND);
                }
            }
            case RESTORE_SLOT -> player.getInventory().setSelectedSlot(action.slot());
            case NONE -> {
                // No local throw or slot restoration is needed this tick.
            }
        }
    }

    /** Applies AutoSoup's pure decision using one ordinary main-hand use request. */
    public static void tickAutoSoup(long tick, AutoSoup autoSoup) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) {
            autoSoup.onPlayerUnavailable();
            return;
        }
        LocalPlayer player = client.player;
        AutoSoup.Action action = autoSoup.update(tick, new AutoSoup.Context(player.getInventory().getSelectedSlot(),
                player.getHealth(), client.gui.screen() != null, player.isUsingItem(), soupSlots(player)));
        switch (action.type()) {
            case SELECT_AND_USE -> {
                player.getInventory().setSelectedSlot(action.slot());
                if (client.gameMode != null && client.gui.screen() == null) {
                    client.gameMode.useItem(player, net.minecraft.world.InteractionHand.MAIN_HAND);
                }
            }
            case USE_SELECTED -> {
                if (client.gameMode != null && client.gui.screen() == null) {
                    client.gameMode.useItem(player, net.minecraft.world.InteractionHand.MAIN_HAND);
                }
            }
            case RESTORE_SLOT -> player.getInventory().setSelectedSlot(action.slot());
            case NONE -> {
                // The module has no owned local selection to change this tick.
            }
        }
    }

    private static void applyRotation(LocalPlayer player, AutoPearl.Action action) {
        if (action.rotate()) {
            player.setYRot(action.yaw());
            player.setXRot(action.pitch());
        }
    }

    private static int firstPearlHotbarSlot(LocalPlayer player) {
        for (int slot = 0; slot < 9; slot++) {
            if (player.getInventory().getItem(slot).is(Items.ENDER_PEARL)) {
                return slot;
            }
        }
        return -1;
    }

    public static void tickBowAim(BowAimAssist bowAim, Snapshot snapshot) {
        Minecraft client = Minecraft.getInstance();
        if (!snapshot.available() || client.player == null) {
            bowAim.onContextLost();
            return;
        }
        tickBowAim(client, bowAim, snapshot.targets(), client.gui.screen() != null);
    }

    public static void tickAimAssist(AimAssist aimAssist, Snapshot snapshot) {
        Minecraft client = Minecraft.getInstance();
        if (!snapshot.available() || client.player == null) {
            aimAssist.onContextLost();
            return;
        }
        boolean screenOpen = client.gui.screen() != null;
        boolean weaponGate = !aimAssist.requireWeapon() || isMeleeWeapon(client.player.getMainHandItem());
        boolean attackKeyGate = !aimAssist.requireAttackKey() || client.options.keyAttack.isDown();
        List<CombatTarget> targets = screenOpen || !weaponGate || !attackKeyGate ? List.of() : snapshot.targets();
        aimAssist.nextRotation(targets, new CombatAim.Rotation(client.player.getYRot(), client.player.getXRot()))
                .ifPresent(rotation -> {
                    client.player.setYRot(rotation.yaw());
                    client.player.setXRot(rotation.pitch());
                });
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

    public static boolean tickClickAura(long tick, ClickAura clickAura, Snapshot snapshot, CombatTargetTracker tracker) {
        Minecraft client = Minecraft.getInstance();
        if (!readyForAttack(client, snapshot)) {
            return false;
        }
        return clickAura.nextAttack(tick, snapshot.targets(), client.options.keyAttack.isDown(), attackReady(client.player))
                .map(target -> attack(client, snapshot.entities().get(target.id()), target, tracker)).orElse(false);
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

    public static boolean tickMaceDmg(long tick, MaceDmg maceDmg, Snapshot snapshot,
                                      CombatTargetTracker tracker) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null || !snapshot.available()) {
            maceDmg.onContextLost();
            return false;
        }
        if (!readyForAttack(client, snapshot) || snapshot.crosshairTarget() == null) {
            return false;
        }
        LocalPlayer player = client.player;
        MaceDmg.Context context = new MaceDmg.Context(
                player.getMainHandItem().is(Items.MACE),
                client.options.keyAttack.isDown(),
                player.getAttackStrengthScale(0.0F),
                player.onGround(),
                player.isInWater() || player.isInLava(),
                player.onClimbable(),
                player.isFallFlying(),
                player.isPassenger(),
                player.fallDistance,
                player.getDeltaMovement().y
        );
        CombatTarget target = snapshot.crosshairTarget();
        if (maceDmg.shouldAttack(tick, target, context)) {
            return attack(client, snapshot.entities().get(target.id()), target, tracker);
        }
        return false;
    }

    public static boolean tickReach(long tick, Reach reach, Snapshot snapshot, CombatTargetTracker tracker) {
        Minecraft client = Minecraft.getInstance();
        if (!readyForAttack(client, snapshot)) {
            return false;
        }
        Optional<CombatTarget> selected = reach.reachAttack(tick, snapshot.targets(), client.options.keyAttack.isDown(),
                attackReady(client.player), snapshot.crosshairTarget() != null);
        if (selected.isEmpty()) {
            return false;
        }
        CombatTarget target = selected.get();
        return attack(client, snapshot.entities().get(target.id()), target, tracker);
    }

    public static boolean tickHitSelect(long tick, HitSelect hitSelect, Snapshot snapshot, CombatTargetTracker tracker) {
        Minecraft client = Minecraft.getInstance();
        if (!readyForAttack(client, snapshot) || snapshot.crosshairTarget() == null) {
            return false;
        }
        LocalPlayer player = client.player;
        HitSelect.Context context = new HitSelect.Context(client.options.keyAttack.isDown(),
                player.getAttackStrengthScale(0.0F), player.isSprinting(), player.onGround(),
                player.isInWater() || player.isInLava(), player.onClimbable(), player.isFallFlying(),
                player.fallDistance, player.getDeltaMovement().y, isMeleeWeapon(player.getMainHandItem()));
        if (hitSelect.shouldAttack(tick, snapshot.crosshairTarget(), context)) {
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

    public static boolean tickAutoClicker(long timeMillis, AutoClicker autoClicker, Snapshot snapshot,
                                          CombatTargetTracker tracker) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null || client.gameMode == null) {
            autoClicker.onContextLost();
            return false;
        }
        CombatTarget crosshair = snapshot.available() ? snapshot.crosshairTarget() : null;
        AutoClicker.Context context = new AutoClicker.Context(client.options.keyAttack.isDown(),
                client.gui.screen() != null, crosshair != null, crosshair != null && crosshair.friend());
        if (!autoClicker.shouldClick(timeMillis, context)) {
            return false;
        }
        LocalPlayer player = client.player;
        if (crosshair != null && autoClicker.shouldAttackEntity(context) && attackReady(player)) {
            LivingEntity entity = snapshot.entities().get(crosshair.id());
            if (entity != null && !entity.isRemoved() && entity.isAlive() && player.hasLineOfSight(entity)) {
                client.gameMode.attack(player, entity);
                tracker.recordAttack(crosshair);
                player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                return true;
            }
        }
        player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        return true;
    }

    public static void tickBlockHit(long tick, BlockHit blockHit, Snapshot snapshot) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) {
            blockHit.onPlayerUnavailable();
            return;
        }
        LocalPlayer player = client.player;
        boolean shieldReady = holdsBlockingItem(player);
        boolean attackHeld = client.options.keyAttack.isDown();
        boolean screenOpen = client.gui.screen() != null;
        List<CombatTarget> targets = snapshot.available() ? snapshot.targets() : List.of();
        blockHit.tick(tick, new BlockHit.Context(shieldReady, attackHeld, attackReady(player), screenOpen, targets));
    }

    private static boolean holdsBlockingItem(LocalPlayer player) {
        return player.getMainHandItem().has(DataComponents.BLOCKS_ATTACKS)
                || player.getOffhandItem().has(DataComponents.BLOCKS_ATTACKS);
    }

    /**
     * Performs at most one ordinary right-click interaction after RightClicker's pure
     * rate-limit and target policy permits it, mirroring vanilla's own use handling
     * through the public {@code gameMode} interaction methods. It creates no packet of
     * its own; the connected server may reject, ignore, or rate-limit the interaction.
     */
    public static void tickRightClicker(long clientTick, RightClicker module, FriendManager friends) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null || client.level == null || client.gameMode == null) {
            return;
        }
        HitResult hitResult = client.hitResult;
        RightClicker.HitKind hitKind = hitKind(hitResult);
        boolean hitIsFriend = hitKind == RightClicker.HitKind.ENTITY
                && isFriendEntity(friends, ((EntityHitResult) hitResult).getEntity());
        boolean hasHeldItem = !player.getItemInHand(InteractionHand.MAIN_HAND).isEmpty()
                || !player.getItemInHand(InteractionHand.OFF_HAND).isEmpty();
        RightClicker.Context context = new RightClicker.Context(true, client.gui.screen() != null,
                client.options.keyUse.isDown(), player.isUsingItem(), hasHeldItem, hitKind, hitIsFriend);
        RightClicker.Decision decision = module.decide(clientTick, context);
        boolean acted = switch (decision) {
            case USE_ON_BLOCK -> useOnBlock(client, player, (BlockHitResult) hitResult);
            case INTERACT_ENTITY -> interactEntity(client, player, (EntityHitResult) hitResult);
            case USE_ITEM -> useHeldItem(client, player);
            case NONE -> false;
        };
        if (acted) {
            long interval = module.intervalTicks();
            ((MinecraftAccessor) client).helikon$setRightClickDelay((int) Math.min(Integer.MAX_VALUE, interval));
        }
    }

    private static RightClicker.HitKind hitKind(HitResult hitResult) {
        if (hitResult == null) {
            return RightClicker.HitKind.MISS;
        }
        return switch (hitResult.getType()) {
            case BLOCK -> RightClicker.HitKind.BLOCK;
            case ENTITY -> RightClicker.HitKind.ENTITY;
            case MISS -> RightClicker.HitKind.MISS;
        };
    }

    private static boolean isFriendEntity(FriendManager friends, Entity entity) {
        if (entity instanceof Player player) {
            String name = player.getGameProfile().name();
            return name != null && !name.isBlank() && friends.contains(name.trim());
        }
        return false;
    }

    private static boolean useOnBlock(Minecraft client, LocalPlayer player, BlockHitResult hit) {
        for (InteractionHand hand : InteractionHand.values()) {
            InteractionResult result = client.gameMode.useItemOn(player, hand, hit);
            if (result.consumesAction()) {
                player.swing(hand);
                return true;
            }
            if (result instanceof InteractionResult.Fail) {
                return false;
            }
        }
        return false;
    }

    private static boolean interactEntity(Minecraft client, LocalPlayer player, EntityHitResult hit) {
        Entity entity = hit.getEntity();
        for (InteractionHand hand : InteractionHand.values()) {
            InteractionResult result = client.gameMode.interact(player, entity, hit, hand);
            if (result.consumesAction()) {
                player.swing(hand);
                return true;
            }
        }
        return false;
    }

    private static boolean useHeldItem(Minecraft client, LocalPlayer player) {
        for (InteractionHand hand : InteractionHand.values()) {
            if (player.getItemInHand(hand).isEmpty()) {
                continue;
            }
            InteractionResult result = client.gameMode.useItem(player, hand);
            if (result.consumesAction()) {
                player.swing(hand);
                return true;
            }
        }
        return false;
    }

    /**
     * Aims via a well-formed server-facing rotation packet and requests ordinary attacks without moving the
     * visible local camera. The server-side rotation is restored to the real camera after the attacks are sent,
     * and the Minecraft server remains authoritative over reach, cooldown, line of sight, and hit validation.
     */
    public static boolean tickSilentAura(long tick, SilentAura silentAura, Snapshot snapshot, CombatTargetTracker tracker) {
        Minecraft client = Minecraft.getInstance();
        if (!readyForAttack(client, snapshot) || client.player.connection == null) {
            return false;
        }
        List<CombatTarget> targets = silentAura.nextAttacks(tick, snapshot.targets(), attackReady(client.player));
        if (targets.isEmpty()) {
            return false;
        }
        LocalPlayer player = client.player;
        float cameraYaw = player.getYRot();
        float cameraPitch = player.getXRot();
        boolean attacked = false;
        for (CombatTarget target : targets) {
            LivingEntity entity = snapshot.entities().get(target.id());
            if (entity == null || entity.isRemoved() || !entity.isAlive() || !player.hasLineOfSight(entity)) {
                continue;
            }
            CombatAim.Rotation aim = silentAura.serverAim(target);
            sendRotation(player, aim.yaw(), aim.pitch());
            attacked |= attack(client, entity, target, tracker);
        }
        if (attacked) {
            // Return the server-side rotation to the untouched local camera so the visible view stays authoritative.
            sendRotation(player, cameraYaw, cameraPitch);
        }
        return attacked;
    }

    private static void sendRotation(LocalPlayer player, float yaw, float pitch) {
        player.connection.send(new ServerboundMovePlayerPacket.Rot(yaw, pitch, player.onGround(),
                player.horizontalCollision));
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
                                                       AntiBot antiBot, TargetFilter targetFilter,
                                                       Map<String, Integer> nameCounts,
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
            // The shared TargetFilter removes disallowed players before any combat module sees them.
            if (isPlayer && !targetFilter.allowsPlayer(name, friend)) {
                continue;
            }
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

    private static List<Integer> soupSlots(LocalPlayer player) {
        List<Integer> slots = new ArrayList<>();
        for (int slot = 0; slot < 9; slot++) {
            if (player.getInventory().getItem(slot).is(Items.MUSHROOM_STEW)) {
                slots.add(slot);
            }
        }
        return List.copyOf(slots);
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
