package dev.helikon.client.module.chat;

import dev.helikon.client.module.ModuleRegistry;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.NumberSetting;
import dev.helikon.client.setting.StringSetting;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnnouncerTest {
    @Test
    void requiresAnEnabledIndividuallySelectedTriggerAndUsesSafeTemplates() {
        Announcer announcer = enabled();
        assertTrue(announcer.messageFor(AnnouncementTrigger.DEATH, "died", 1_000L, false).isEmpty());

        booleanSetting(announcer, "death").set(true);
        assertEquals("death: died", announcer.messageFor(AnnouncementTrigger.DEATH, "died", 1_000L, false).orElseThrow());
        assertTrue(announcer.messageFor(AnnouncementTrigger.DEATH, "again", 2_000L, false).isEmpty());
        assertTrue(announcer.messageFor(AnnouncementTrigger.JOIN, "joined", 20_000L, false).isEmpty());

        stringSetting(announcer, "message_template").set("/unsafe {detail}");
        assertTrue(announcer.messageFor(AnnouncementTrigger.DEATH, "died", 20_000L, false).isEmpty());
    }

    @Test
    void honorsScreenPauseAndAllowsTheUserToTurnItOff() {
        Announcer announcer = enabled();
        booleanSetting(announcer, "totem_use").set(true);
        assertTrue(announcer.messageFor(AnnouncementTrigger.TOTEM_USE, "used a totem", 1_000L, true).isEmpty());

        booleanSetting(announcer, "pause_in_gui").set(false);
        assertEquals("totem use: used a totem",
                announcer.messageFor(AnnouncementTrigger.TOTEM_USE, "used a totem", 1_000L, true).orElseThrow());
    }

    @Test
    void exposesEveryPlanTriggerAsAnIndividuallyDisabledSetting() {
        Announcer announcer = new Announcer();
        for (AnnouncementTrigger trigger : AnnouncementTrigger.values()) {
            assertFalse(announcer.triggerEnabled(trigger));
        }
    }

    @Test
    void keepsCooldownAndSessionCapAcrossModuleToggles() {
        Announcer announcer = new Announcer();
        ModuleRegistry registry = new ModuleRegistry();
        registry.register(announcer);
        booleanSetting(announcer, "death").set(true);
        numberSetting(announcer, "session_message_cap").set(1.0D);
        registry.setEnabled(announcer, true);
        assertTrue(announcer.messageFor(AnnouncementTrigger.DEATH, "died", 1_000L, false).isPresent());

        registry.setEnabled(announcer, false);
        registry.setEnabled(announcer, true);
        assertTrue(announcer.messageFor(AnnouncementTrigger.DEATH, "died again", 2_000L, false).isEmpty());
        assertTrue(announcer.messageFor(AnnouncementTrigger.DEATH, "much later", 20_000L, false).isEmpty());
    }

    @Test
    void boundsHookQueuesAndDropsPreEnableAttackState() {
        AnnouncerAccess.reset();
        Announcer announcer = enabled();
        ModuleRegistry registry = new ModuleRegistry();
        registry.register(announcer);
        booleanSetting(announcer, "kill").set(true);
        booleanSetting(announcer, "death").set(true);
        registry.setEnabled(announcer, true);
        AnnouncerAccess.install(announcer, new ArrayList<String>()::add);

        UUID target = UUID.randomUUID();
        AnnouncerAccess.recordAttack(target, "target", 1_000L);
        registry.setEnabled(announcer, false);
        AnnouncerAccess.tick(new dev.helikon.client.chat.AnnouncerObservationTracker.Fact(0, 64, 0, 20,
                "minecraft:overworld"), false, 1_001L);
        registry.setEnabled(announcer, true);
        AnnouncerAccess.observeEntityUnload(target, true, 1_002L);
        assertEquals(0, AnnouncerAccess.pendingObservationCount());

        for (int index = 0; index < AnnouncerAccess.MAXIMUM_PENDING_OBSERVATIONS + 20; index++) {
            AnnouncerAccess.enqueue(AnnouncementTrigger.DEATH, "died");
        }
        assertEquals(AnnouncerAccess.MAXIMUM_PENDING_OBSERVATIONS, AnnouncerAccess.pendingObservationCount());

        booleanSetting(announcer, "advancement").set(false);
        for (int index = 0; index < AnnouncerAccess.MAXIMUM_REMEMBERED_ADVANCEMENTS + 20; index++) {
            AnnouncerAccess.observeAdvancement("minecraft:disabled_" + index);
        }
        assertEquals(0, AnnouncerAccess.rememberedAdvancementCount());
        booleanSetting(announcer, "advancement").set(true);
        for (int index = 0; index < AnnouncerAccess.MAXIMUM_REMEMBERED_ADVANCEMENTS + 20; index++) {
            AnnouncerAccess.observeAdvancement("minecraft:enabled_" + index);
        }
        assertTrue(AnnouncerAccess.rememberedAdvancementCount() <= AnnouncerAccess.MAXIMUM_REMEMBERED_ADVANCEMENTS);
        AnnouncerAccess.reset();
    }

    private static Announcer enabled() {
        Announcer announcer = new Announcer();
        ModuleRegistry registry = new ModuleRegistry();
        registry.register(announcer);
        registry.setEnabled(announcer, true);
        return announcer;
    }

    private static BooleanSetting booleanSetting(Announcer announcer, String id) {
        return (BooleanSetting) announcer.settings().stream().filter(setting -> setting.id().equals(id)).findFirst().orElseThrow();
    }

    private static StringSetting stringSetting(Announcer announcer, String id) {
        return (StringSetting) announcer.settings().stream().filter(setting -> setting.id().equals(id)).findFirst().orElseThrow();
    }

    private static NumberSetting numberSetting(Announcer announcer, String id) {
        return (NumberSetting) announcer.settings().stream().filter(setting -> setting.id().equals(id)).findFirst().orElseThrow();
    }
}
