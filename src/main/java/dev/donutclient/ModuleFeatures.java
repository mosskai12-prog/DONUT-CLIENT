package dev.donutclient;

import net.minecraft.client.Minecraft;

public final class ModuleFeatures {

    private static boolean fullbrightActive;
    private static double previousGamma = 0.5;

    private ModuleFeatures() {
    }

    public static void tick(Minecraft client) {
        if (client.player == null || client.level == null) {
            restoreGamma(client);
            return;
        }

        boolean enabled = ModuleConfig.isEnabled("Fullbright");

        if (enabled && !fullbrightActive) {
            previousGamma = client.options.gamma().get();
            fullbrightActive = true;
        }

        if (enabled) {
            double targetGamma = ModuleConfig.getBoolean(
                    "Fullbright", "maximum_brightness", true
            ) ? 1.0 : 0.8;
            client.options.gamma().set(targetGamma);
        } else {
            restoreGamma(client);
        }
    }

    private static void restoreGamma(Minecraft client) {
        if (fullbrightActive) {
            boolean shouldRestore = ModuleConfig.getBoolean(
                    "Fullbright", "restore_brightness_on_disable", true
            );
            if (shouldRestore) {
                client.options.gamma().set(previousGamma);
            }
            fullbrightActive = false;
        }
    }
}
