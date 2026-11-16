package dev.donutclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;

public final class ModuleFeatures {
    private static boolean fullbrightActive;
    private static double previousGamma = 0.5;
    private static boolean autoSprintApplied;

    private ModuleFeatures() {}

    public static void registerHud() {
        HudElementRegistry.attachElementBefore(
                VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath("donutclient", "client_hud"),
                ModuleFeatures::renderHud
        );
    }

    public static void tick(Minecraft client) {
        if (client.player == null || client.level == null) {
            restoreGamma(client);
            autoSprintApplied = false;
            return;
        }

        updateFullbright(client);
        updateAutoSprint(client);
    }

    private static void updateFullbright(Minecraft client) {
        boolean enabled = ModuleConfig.isEnabled("Fullbright");
        if (enabled && !fullbrightActive) {
            previousGamma = client.options.gamma().get();
            fullbrightActive = true;
        }

        if (enabled) {
            client.options.gamma().set(
                    ModuleConfig.getBoolean("Fullbright", "maximum_brightness", true) ? 1.0 : 0.8
            );
        } else {
            restoreGamma(client);
        }
    }

    private static void restoreGamma(Minecraft client) {
        if (!fullbrightActive) return;
        if (ModuleConfig.getBoolean("Fullbright", "restore_brightness_on_disable", true)) {
            client.options.gamma().set(previousGamma);
        }
        fullbrightActive = false;
    }

    private static void updateAutoSprint(Minecraft client) {
        boolean enabled = ModuleConfig.isEnabled("Toggle Sprint");
        if (!enabled) {
            if (autoSprintApplied && client.player.isSprinting()) {
                client.player.setSprinting(false);
            }
            autoSprintApplied = false;
            return;
        }

        boolean forward = client.options.keyUp.isDown();
        boolean sneaking = client.options.keyShift.isDown();
        boolean requireForward = ModuleConfig.getBoolean("Toggle Sprint", "require_forward_movement", true);
        boolean stopWhileSneaking = ModuleConfig.getBoolean("Toggle Sprint", "stop_while_sneaking", true);
        boolean shouldSprint = (!requireForward || forward) && !(stopWhileSneaking && sneaking)
                && !client.player.isPassenger();

        if (shouldSprint) {
            client.player.setSprinting(true);
            autoSprintApplied = true;
        } else if (autoSprintApplied) {
            client.player.setSprinting(false);
            autoSprintApplied = false;
        }
    }

    private static void renderHud(GuiGraphics graphics, DeltaTracker tickCounter) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.options.hideGui) return;

        int y = 8;
        if (ModuleConfig.isEnabled("Coordinates HUD")) {
            String coordinates = String.format("XYZ  %d  %d  %d",
                    client.player.blockPosition().getX(),
                    client.player.blockPosition().getY(),
                    client.player.blockPosition().getZ());
            graphics.drawString(client.font, coordinates, 8, y, 0xFFF2F4F7, true);
            y += 12;
        }

        if (ModuleConfig.isEnabled("FPS Counter")) {
            graphics.drawString(client.font, "FPS  " + client.getFps(), 8, y, 0xFF63D69A, true);
        }
    }
}