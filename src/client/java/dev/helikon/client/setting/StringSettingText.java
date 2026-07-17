package dev.helikon.client.setting;

/** Validated text-field application shared by the ClickGUI and local command. */
public final class StringSettingText {
    private StringSettingText() {
    }

    public static boolean tryApply(StringSetting setting, String text) {
        try {
            setting.set(text);
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
