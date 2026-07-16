package dev.helikon.client.command;

import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleRegistry;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.NumberSetting;
import dev.helikon.client.setting.NumberSettingText;
import dev.helikon.client.setting.Setting;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/** Edits a module setting by ID, using each setting type's validated path. */
public final class SettingCommand implements HelikonCommand {
    private final ModuleRegistry registry;

    public SettingCommand(ModuleRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "registry");
    }

    @Override
    public String name() {
        return "setting";
    }

    @Override
    public String usage() {
        return CommandDispatcher.PREFIX + "setting <module> <setting> <value>";
    }

    @Override
    public String description() {
        return "Changes a module setting.";
    }

    @Override
    public void execute(List<String> arguments, CommandFeedback feedback) {
        if (arguments.size() != 3) {
            feedback.error("Usage: " + usage());
            return;
        }

        Optional<Module> foundModule = ModuleArguments.requireModule(registry, arguments.get(0), feedback);
        if (foundModule.isEmpty()) {
            return;
        }
        Module module = foundModule.get();

        String settingId = arguments.get(1).toLowerCase(Locale.ROOT);
        Optional<Setting<?>> foundSetting = module.settings().stream()
                .filter(setting -> setting.id().equals(settingId))
                .findFirst();
        if (foundSetting.isEmpty()) {
            feedback.error("Module '" + module.id() + "' has no setting '" + settingId + "'.");
            return;
        }

        String value = arguments.get(2);
        switch (foundSetting.get()) {
            case BooleanSetting booleanSetting -> applyBoolean(module, booleanSetting, value, feedback);
            case NumberSetting numberSetting -> applyNumber(module, numberSetting, value, feedback);
            default -> feedback.error("Setting '" + settingId + "' has an unsupported type for this command.");
        }
    }

    private static void applyBoolean(Module module, BooleanSetting setting, String value, CommandFeedback feedback) {
        String normalized = value.toLowerCase(Locale.ROOT);
        if (!normalized.equals("true") && !normalized.equals("false")) {
            feedback.error("Expected true or false for '" + setting.id() + "', got '" + value + "'.");
            return;
        }
        setting.set(normalized.equals("true"));
        feedback.info("'" + module.id() + "." + setting.id() + "' set to " + setting.value() + ".");
    }

    private static void applyNumber(Module module, NumberSetting setting, String value, CommandFeedback feedback) {
        if (!NumberSettingText.tryApply(setting, value)) {
            feedback.error("Expected a number between " + NumberSettingText.format(setting.minimum())
                    + " and " + NumberSettingText.format(setting.maximum())
                    + " for '" + setting.id() + "', got '" + value + "'.");
            return;
        }
        feedback.info("'" + module.id() + "." + setting.id() + "' set to " + NumberSettingText.format(setting.value()) + ".");
    }
}
