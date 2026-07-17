package dev.helikon.client.module.player;

/** Narrow static bridge for the portal screen-allowance redirect. */
public final class PortalGuiAccess {
    private static volatile PortalGui module;

    private PortalGuiAccess() {
    }

    public static void install(PortalGui portalGui) {
        module = portalGui;
    }

    public static boolean allows(boolean vanillaAllows) {
        PortalGui current = module;
        return current == null ? vanillaAllows : current.allowsScreenInPortal(vanillaAllows);
    }
}
