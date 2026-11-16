package dev.donutclient;

import net.minecraft.client.Minecraft;

public final class ModuleFeatures {

    private static boolean fullbrightActive;
    private static double previousGamma = 0.5;

    private ModuleFeatures() {
    }

    public static void tick(Minecraft client) {
        if (client.player == null || client.level == null) {
            fullbrightActive = false;
            return;
        }

        boolean fullbrightEnabled = ModuleConfig.isEnabled("Fullbright");

        if (fullbrightEnabled && !fullbrightActive) {
            previousGamma = client.options.gamma().get();
            fullbrightActive = true;
        }

        if (fullbrightEnabled) {
            client.options.gamma().set(1.0);
        } else if (fullbrightActive) {
            client.options.gamma().set(previousGamma);
            fullbrightActive = false;
        }
    }
}
