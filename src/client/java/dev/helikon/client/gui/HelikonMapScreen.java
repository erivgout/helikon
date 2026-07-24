package dev.helikon.client.gui;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import dev.helikon.client.map.MapMarkerLayout;
import dev.helikon.client.map.MapRegion;
import dev.helikon.client.map.MapStoreStatus;
import dev.helikon.client.map.MapTextureCache;
import dev.helikon.client.map.MapTileStore;
import dev.helikon.client.map.MapViewport;
import dev.helikon.client.module.render.Radar;
import dev.helikon.client.waypoint.WaypointContext;
import dev.helikon.client.waypoint.WaypointLocation;
import dev.helikon.client.waypoint.WaypointLocationProvider;
import dev.helikon.client.waypoint.WaypointRepository;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Non-pausing, north-up view of locally discovered terrain and current-context waypoints. */
public final class HelikonMapScreen extends Screen {
    private static final int BACKGROUND_COLOR = 0xFF0B0D11;
    private static final int TOOLBAR_COLOR = 0xE614161B;
    private static final int GRID_COLOR = 0x403E4652;
    private static final int REGION_GRID_COLOR = 0x80636F80;
    private static final int TEXT_COLOR = 0xFFF4F7FA;
    private static final int DIM_TEXT_COLOR = 0xFFAAB2BE;
    private static final int ACCENT_COLOR = 0xFF9B7BFF;
    private static final int PLAYER_COLOR = 0xFF4FC3F7;
    private static final int TOOLBAR_HEIGHT = 22;
    private static final int FOOTER_HEIGHT = 18;
    private static final double KEYBOARD_PAN_PIXELS = 28.0D;

    private final MapTileStore store;
    private final WaypointRepository waypoints;
    private final WaypointLocationProvider locations;
    private final Radar radar;
    private final WaypointContext context;
    private final MapViewport viewport;
    private MapTextureCache textures;
    private boolean dragging;

    public HelikonMapScreen(MapTileStore store, WaypointRepository waypoints,
                            WaypointLocationProvider locations, Radar radar,
                            WaypointLocation initialLocation) {
        super(Component.translatable("screen.helikon.map.title"));
        this.store = Objects.requireNonNull(store, "store");
        this.waypoints = Objects.requireNonNull(waypoints, "waypoints");
        this.locations = Objects.requireNonNull(locations, "locations");
        this.radar = Objects.requireNonNull(radar, "radar");
        WaypointLocation location = Objects.requireNonNull(initialLocation, "initialLocation");
        context = location.context();
        viewport = new MapViewport(location.x() + 0.5D, location.z() + 0.5D, 1.0D);
        store.activate(context);
    }

    @Override
    protected void init() {
        super.init();
        if (textures == null) {
            textures = new MapTextureCache(minecraft);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean isInGameUi() {
        return true;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(0, 0, width, height, BACKGROUND_COLOR);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(0, 0, width, height, BACKGROUND_COLOR);
        drawRegions(graphics);
        drawGrid(graphics);
        drawWaypoints(graphics, mouseX, mouseY);
        drawPlayer(graphics);
        drawToolbar(graphics);
        drawFooter(graphics, mouseX, mouseY);
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    @Override
    public void tick() {
        super.tick();
        WaypointLocation current = locations.currentLocation().orElse(null);
        if (current == null || !current.context().equals(context)) {
            minecraft.gui.setScreen(null);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            dragging = true;
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (dragging && event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            viewport.panPixels(dragX, dragY);
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT && dragging) {
            dragging = false;
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (vertical != 0.0D) {
            viewport.zoomAt(vertical > 0.0D ? 1.25D : 0.8D, mouseX, mouseY, width, height);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        double blocks = KEYBOARD_PAN_PIXELS / viewport.pixelsPerBlock();
        switch (event.key()) {
            case GLFW.GLFW_KEY_A, GLFW.GLFW_KEY_LEFT -> viewport.panBlocks(-blocks, 0.0D);
            case GLFW.GLFW_KEY_D, GLFW.GLFW_KEY_RIGHT -> viewport.panBlocks(blocks, 0.0D);
            case GLFW.GLFW_KEY_W, GLFW.GLFW_KEY_UP -> viewport.panBlocks(0.0D, -blocks);
            case GLFW.GLFW_KEY_S, GLFW.GLFW_KEY_DOWN -> viewport.panBlocks(0.0D, blocks);
            case GLFW.GLFW_KEY_R -> recenter();
            default -> {
                return super.keyPressed(event);
            }
        }
        return true;
    }

    @Override
    public void removed() {
        dragging = false;
        if (textures != null) {
            textures.close();
            textures = null;
        }
        super.removed();
    }

    public void onResourceReload() {
        if (textures != null) {
            textures.clear();
        }
    }

    private void drawRegions(GuiGraphicsExtractor graphics) {
        if (textures == null) {
            return;
        }
        int renderedRegions = 0;
        for (MapViewport.RegionCoordinate region : viewport.visibleRegions(width, height)) {
            if (renderedRegions >= MapTextureCache.MAXIMUM_TEXTURES) {
                break;
            }
            store.request(context, region.x(), region.z());
            MapRegion.Snapshot snapshot = store.snapshot(context, region.x(), region.z()).orElse(null);
            if (snapshot == null) {
                continue;
            }
            renderedRegions++;
            Identifier texture = textures.texture(context, snapshot);
            MapViewport.ScreenRectangle rectangle =
                    viewport.regionScreenRectangle(region.x(), region.z(), width, height);
            RenderPipeline pipeline = RenderPipelines.GUI_TEXTURED;
            graphics.blit(pipeline, texture, rectangle.x(), rectangle.y(), 0.0F, 0.0F,
                    rectangle.width(), rectangle.height(), MapRegion.SIZE, MapRegion.SIZE,
                    MapRegion.SIZE, MapRegion.SIZE);
        }
    }

    private void drawGrid(GuiGraphicsExtractor graphics) {
        double spacingBlocks = viewport.pixelsPerBlock() >= 2.0D ? 16.0D : MapRegion.SIZE;
        int color = spacingBlocks == 16.0D ? GRID_COLOR : REGION_GRID_COLOR;
        MapViewport.WorldPoint topLeft = viewport.screenToWorld(0, TOOLBAR_HEIGHT, width, height);
        MapViewport.WorldPoint bottomRight = viewport.screenToWorld(width, height - FOOTER_HEIGHT, width, height);
        long firstX = (long) Math.floor(topLeft.x() / spacingBlocks);
        long lastX = (long) Math.ceil(bottomRight.x() / spacingBlocks);
        long firstZ = (long) Math.floor(topLeft.z() / spacingBlocks);
        long lastZ = (long) Math.ceil(bottomRight.z() / spacingBlocks);
        for (long gridX = firstX; gridX <= lastX; gridX++) {
            double worldX = gridX * spacingBlocks;
            int x = (int) Math.round(viewport.worldToScreen(worldX, viewport.centerZ(), width, height).x());
            graphics.fill(x, TOOLBAR_HEIGHT, x + 1, height - FOOTER_HEIGHT, color);
        }
        for (long gridZ = firstZ; gridZ <= lastZ; gridZ++) {
            double worldZ = gridZ * spacingBlocks;
            int y = (int) Math.round(viewport.worldToScreen(viewport.centerX(), worldZ, width, height).y());
            graphics.fill(0, y, width, y + 1, color);
        }
    }

    private void drawWaypoints(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        List<MapMarkerLayout.Marker> markers = MapMarkerLayout.layout(
                waypoints.snapshotForContext(context), context, viewport, width, height, mouseX, mouseY);
        for (MapMarkerLayout.Marker marker : markers) {
            int x = (int) Math.round(marker.screenX());
            int y = (int) Math.round(marker.screenY());
            drawWaypointMarker(graphics, marker, x, y);
            if (marker.labelVisible()) {
                int labelX = x + 6;
                int labelY = y - font.lineHeight / 2;
                int labelWidth = font.width(marker.name()) + 6;
                graphics.fill(labelX, labelY - 2, labelX + labelWidth,
                        labelY + font.lineHeight + 2, 0xD014161B);
                graphics.text(font, marker.name(), labelX + 3, labelY, TEXT_COLOR, true);
            }
        }
    }

    private static void drawWaypointMarker(GuiGraphicsExtractor graphics,
                                           MapMarkerLayout.Marker marker, int x, int y) {
        if (marker.death()) {
            graphics.fill(x - 4, y - 4, x - 2, y - 2, 0xE0000000);
            graphics.fill(x + 2, y - 4, x + 4, y - 2, 0xE0000000);
            graphics.fill(x - 2, y - 2, x + 2, y + 2, 0xE0000000);
            graphics.fill(x - 4, y + 2, x - 2, y + 4, 0xE0000000);
            graphics.fill(x + 2, y + 2, x + 4, y + 4, 0xE0000000);
            graphics.fill(x - 3, y - 3, x - 1, y - 1, marker.color());
            graphics.fill(x + 1, y - 3, x + 3, y - 1, marker.color());
            graphics.fill(x - 1, y - 1, x + 1, y + 1, marker.color());
            graphics.fill(x - 3, y + 1, x - 1, y + 3, marker.color());
            graphics.fill(x + 1, y + 1, x + 3, y + 3, marker.color());
            return;
        }

        // Compact pixel-art map pin: dark outline, waypoint color, white center, and a pointed tip.
        graphics.fill(x - 2, y - 6, x + 3, y - 5, 0xE0000000);
        graphics.fill(x - 4, y - 5, x + 5, y, 0xE0000000);
        graphics.fill(x - 3, y - 4, x + 4, y - 1, marker.color());
        graphics.fill(x - 1, y - 3, x + 2, y - 1, TEXT_COLOR);
        graphics.fill(x - 2, y, x + 3, y + 1, 0xE0000000);
        graphics.fill(x - 1, y + 1, x + 2, y + 2, 0xE0000000);
        graphics.fill(x, y + 2, x + 1, y + 4, 0xE0000000);
    }

    private void drawPlayer(GuiGraphicsExtractor graphics) {
        WaypointLocation player = locations.currentLocation().orElse(null);
        if (player == null || !player.context().equals(context)) {
            return;
        }
        MapViewport.ScreenPoint point = viewport.worldToScreen(
                player.x() + 0.5D, player.z() + 0.5D, width, height);
        int x = (int) Math.round(point.x());
        int y = (int) Math.round(point.y());
        graphics.fill(x - 4, y - 4, x + 5, y + 5, 0xD0000000);
        graphics.fill(x - 2, y - 2, x + 3, y + 3, PLAYER_COLOR);
        graphics.fill(x, y - 5, x + 1, y - 2, TEXT_COLOR);
    }

    private void drawToolbar(GuiGraphicsExtractor graphics) {
        graphics.fill(0, 0, width, TOOLBAR_HEIGHT, TOOLBAR_COLOR);
        graphics.fill(0, TOOLBAR_HEIGHT - 1, width, TOOLBAR_HEIGHT, ACCENT_COLOR);
        graphics.text(font, Component.translatable("screen.helikon.map.title"), 7, 7, ACCENT_COLOR, true);
        String contextText = displayContext(context) + "  •  " + context.dimension();
        int contextX = Math.max(90, width / 2 - font.width(contextText) / 2);
        graphics.text(font, contextText, contextX, 7, DIM_TEXT_COLOR, false);
        String controls = "Drag/WASD pan  •  Wheel zoom  •  R recenter  •  Esc close";
        int controlsX = width - font.width(controls) - 7;
        if (controlsX > contextX + font.width(contextText) + 12) {
            graphics.text(font, controls, controlsX, 7, DIM_TEXT_COLOR, false);
        }
    }

    private void drawFooter(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int y = height - FOOTER_HEIGHT;
        graphics.fill(0, y, width, height, TOOLBAR_COLOR);
        graphics.fill(0, y, width, y + 1, REGION_GRID_COLOR);
        MapViewport.WorldPoint cursor = viewport.screenToWorld(mouseX, mouseY, width, height);
        String position = "Cursor " + (int) Math.floor(cursor.x()) + ", " + (int) Math.floor(cursor.z())
                + "  •  Zoom " + String.format(Locale.ROOT, "%.2fx", viewport.pixelsPerBlock());
        graphics.text(font, position, 7, y + 5, TEXT_COLOR, false);

        MapStoreStatus storeStatus = store.status();
        String statusText = captureActive() ? storeStatus.detail() : "Map recording paused in Radar settings";
        int color = storeStatus.state() == MapStoreStatus.State.READY ? DIM_TEXT_COLOR : 0xFFFFC857;
        graphics.text(font, statusText, Math.max(7, width - font.width(statusText) - 7), y + 5, color, false);
    }

    private void recenter() {
        locations.currentLocation().filter(location -> location.context().equals(context))
                .ifPresent(location -> viewport.recenter(location.x() + 0.5D, location.z() + 0.5D));
    }

    private boolean captureActive() {
        return radar.isEnabled() && radar.minimap() && radar.saveDiscoveredMap();
    }

    private static String displayContext(WaypointContext context) {
        int separator = context.scope().indexOf(':');
        return separator >= 0 ? context.scope().substring(separator + 1) : context.scope();
    }
}
