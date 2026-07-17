package dev.helikon.client.hud;

import dev.helikon.client.panic.PanicState;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.biome.Biome;

import java.util.List;
import java.util.Objects;

/** Opt-in local readouts covering the remaining telemetry elements named by the version-one HUD plan. */
public final class PlanTelemetryHud implements HudElement {
    private static final int PADDING = 3;
    private static final int TEXT_COLOR = 0xFFE5EDF5;
    private static final int BACKGROUND = 0xB014161B;
    private static final List<HudElementId> ELEMENTS = List.of(
            HudElementId.DIRECTION, HudElementId.FPS, HudElementId.PING, HudElementId.TPS, HudElementId.SPEED,
            HudElementId.ARMOR_DURABILITY, HudElementId.HELD_ITEM_DURABILITY, HudElementId.POTION_EFFECTS,
            HudElementId.CLOCK, HudElementId.BIOME, HudElementId.SERVER_ADDRESS, HudElementId.TOTEM_COUNT
    );

    private final HudLayout layout;
    private final PanicState panicState;
    private final ClientTpsEstimate tpsEstimate;

    public PlanTelemetryHud(HudLayout layout, PanicState panicState, ClientTpsEstimate tpsEstimate) {
        this.layout = Objects.requireNonNull(layout, "layout");
        this.panicState = Objects.requireNonNull(panicState, "panicState");
        this.tpsEstimate = Objects.requireNonNull(tpsEstimate, "tpsEstimate");
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        if (panicState.customHudHidden()) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) {
            return;
        }
        for (HudElementId element : ELEMENTS) {
            HudElementPlacement placement = layout.element(element);
            if (!placement.enabled()) {
                continue;
            }
            String line = line(element, client);
            if (line == null) {
                continue;
            }
            int width = client.font.width(line) + PADDING * 2;
            int height = client.font.lineHeight + PADDING * 2;
            HudBounds bounds = placement.bounds(graphics.guiWidth(), graphics.guiHeight(), width, height);
            graphics.fill(bounds.x(), bounds.y(), bounds.x() + width, bounds.y() + height, BACKGROUND);
            graphics.text(client.font, line, bounds.x() + PADDING, bounds.y() + PADDING, TEXT_COLOR, true);
        }
    }

    private String line(HudElementId element, Minecraft client) {
        return switch (element) {
            case DIRECTION -> TelemetryText.direction(client.player.getYRot());
            case FPS -> TelemetryText.fps(client.getFps());
            case PING -> ping(client);
            case TPS -> TelemetryText.tps(tpsEstimate.tps());
            case SPEED -> TelemetryText.speed(client.player.getDeltaMovement().horizontalDistance() * 20.0D);
            case ARMOR_DURABILITY -> armorDurability(client.player.getItemBySlot(EquipmentSlot.HEAD),
                    client.player.getItemBySlot(EquipmentSlot.CHEST), client.player.getItemBySlot(EquipmentSlot.LEGS),
                    client.player.getItemBySlot(EquipmentSlot.FEET));
            case HELD_ITEM_DURABILITY -> durability("Held", client.player.getMainHandItem());
            case POTION_EFFECTS -> potionEffects(client.player.getActiveEffects());
            case CLOCK -> TelemetryText.clock(client.level.getOverworldClockTime());
            case BIOME -> biome(client);
            case SERVER_ADDRESS -> server(client);
            case TOTEM_COUNT -> "Totems " + totemCount(client.player.getInventory().getNonEquipmentItems(),
                    client.player.getOffhandItem());
            default -> throw new IllegalArgumentException("Not a plan telemetry element: " + element);
        };
    }

    private static String ping(Minecraft client) {
        PlayerInfo info = client.getConnection() == null ? null : client.getConnection().getPlayerInfo(client.player.getUUID());
        return TelemetryText.ping(info == null ? -1 : info.getLatency());
    }

    private static String armorDurability(ItemStack... armor) {
        int totalRemaining = 0;
        int totalMaximum = 0;
        for (ItemStack stack : armor) {
            if (!stack.isDamageableItem()) {
                continue;
            }
            totalRemaining += Math.max(0, stack.getMaxDamage() - stack.getDamageValue());
            totalMaximum += stack.getMaxDamage();
        }
        return totalMaximum == 0 ? null : TelemetryText.durability("Armor", totalRemaining, totalMaximum);
    }

    private static String durability(String label, ItemStack stack) {
        if (!stack.isDamageableItem()) {
            return null;
        }
        return TelemetryText.durability(label, Math.max(0, stack.getMaxDamage() - stack.getDamageValue()),
                stack.getMaxDamage());
    }

    private static String potionEffects(Iterable<MobEffectInstance> effects) {
        StringBuilder line = new StringBuilder("Effects ");
        int count = 0;
        for (MobEffectInstance effect : effects) {
            if (count == 3) {
                line.append(" +");
                break;
            }
            if (count > 0) {
                line.append(", ");
            }
            line.append(effect.getEffect().value().getDisplayName().getString());
            if (effect.getAmplifier() > 0) {
                line.append(' ').append(effect.getAmplifier() + 1);
            }
            count++;
        }
        return count == 0 ? null : line.toString();
    }

    private static String biome(Minecraft client) {
        Holder<Biome> biome = client.level.getBiomeManager().getBiome(client.player.blockPosition());
        String identifier = biome.unwrapKey().map(key -> key.identifier().toString()).orElse("unknown");
        return "Biome " + TelemetryText.titleCaseIdentifier(identifier);
    }

    private static String server(Minecraft client) {
        ServerData server = client.getCurrentServer();
        return "Server " + (server == null ? "Singleplayer" : server.ip);
    }

    private static int totemCount(Iterable<ItemStack> inventory, ItemStack offhand) {
        int total = 0;
        for (ItemStack stack : inventory) {
            if (stack.getItem() == Items.TOTEM_OF_UNDYING) {
                total += stack.getCount();
            }
        }
        if (offhand.getItem() == Items.TOTEM_OF_UNDYING) {
            total += offhand.getCount();
        }
        return total;
    }
}
