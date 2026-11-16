package dev.donutclient;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;

import java.util.ArrayList;
import java.util.List;

public final class ModuleFeatures {

    private static boolean fullbrightActive;
    private static double previousGamma = 0.5;
    private static final List<EspBlock> ESP_BLOCKS = new ArrayList<>();
    private static int scanCooldown;

    private record EspBlock(BlockPos pos, int color) {}

    private ModuleFeatures() {}

    public static void tick(Minecraft client) {
        if (client.player == null || client.level == null) {
            restoreGamma(client);
            ESP_BLOCKS.clear();
            return;
        }

        boolean enabled = ModuleConfig.isEnabled("Fullbright");
        if (enabled && !fullbrightActive) {
            previousGamma = client.options.gamma().get();
            fullbrightActive = true;
        }
        if (enabled) {
            double targetGamma = ModuleConfig.getBoolean("Fullbright", "maximum_brightness", true) ? 1.0 : 0.8;
            client.options.gamma().set(targetGamma);
        } else {
            restoreGamma(client);
        }

        if (scanCooldown-- <= 0) {
            scanCooldown = 10;
            scanEspBlocks(client);
        }
    }

    private static void scanEspBlocks(Minecraft client) {
        ESP_BLOCKS.clear();
        boolean ores = ModuleConfig.isEnabled("BlockESP");
        boolean storage = ModuleConfig.isEnabled("StorageESP");
        if (!ores && !storage) return;

        BlockPos center = client.player.blockPosition();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int radius = 16;
        int vertical = 12;

        for (int x = center.getX() - radius; x <= center.getX() + radius; x++) {
            for (int y = center.getY() - vertical; y <= center.getY() + vertical; y++) {
                for (int z = center.getZ() - radius; z <= center.getZ() + radius; z++) {
                    cursor.set(x, y, z);
                    if (!client.level.hasChunkAt(cursor)) continue;
                    BlockState state = client.level.getBlockState(cursor);
                    Block block = state.getBlock();
                    int color = ores ? oreColor(block) : 0;
                    if (color == 0 && storage) color = storageColor(block);
                    if (color != 0) ESP_BLOCKS.add(new EspBlock(cursor.immutable(), color));
                }
            }
        }
    }

    private static int oreColor(Block block) {
        if (isSelected("Diamond Ore", block, Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE)) return 0xFF35D9F5;
        if (isSelected("Deepslate Diamond Ore", block, Blocks.DEEPSLATE_DIAMOND_ORE)) return 0xFF35D9F5;
        if (isSelected("Ancient Debris", block, Blocks.ANCIENT_DEBRIS)) return 0xFFFF8A35;
        if (isSelected("Emerald Ore", block, Blocks.EMERALD_ORE, Blocks.DEEPSLATE_EMERALD_ORE)) return 0xFF39E879;
        if (isSelected("Gold Ore", block, Blocks.GOLD_ORE, Blocks.DEEPSLATE_GOLD_ORE, Blocks.NETHER_GOLD_ORE)) return 0xFFFFD447;
        if (isSelected("Iron Ore", block, Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE)) return 0xFFD7DCE5;
        if (isSelected("Copper Ore", block, Blocks.COPPER_ORE, Blocks.DEEPSLATE_COPPER_ORE)) return 0xFFFF965C;
        if (isSelected("Redstone Ore", block, Blocks.REDSTONE_ORE, Blocks.DEEPSLATE_REDSTONE_ORE)) return 0xFFFF4545;
        if (isSelected("Lapis Lazuli Ore", block, Blocks.LAPIS_ORE, Blocks.DEEPSLATE_LAPIS_ORE)) return 0xFF477CFF;
        if (isSelected("Coal Ore", block, Blocks.COAL_ORE, Blocks.DEEPSLATE_COAL_ORE)) return 0xFF858B96;
        return 0;
    }

    private static boolean isSelected(String setting, Block actual, Block... variants) {
        if (!ModuleConfig.getBoolean("BlockESP", key(setting), true)) return false;
        for (Block variant : variants) if (actual == variant) return true;
        return false;
    }

    private static int storageColor(Block block) {
        if (block == Blocks.CHEST && ModuleConfig.getBoolean("StorageESP", "chests", true)) return 0xFFFFB347;
        if (block == Blocks.TRAPPED_CHEST && ModuleConfig.getBoolean("StorageESP", "trapped_chests", true)) return 0xFFFF5555;
        if (block == Blocks.ENDER_CHEST && ModuleConfig.getBoolean("StorageESP", "ender_chests", true)) return 0xFFB46BFF;
        if (block == Blocks.BARREL && ModuleConfig.getBoolean("StorageESP", "barrels", true)) return 0xFFB98A5A;
        if (block == Blocks.SHULKER_BOX && ModuleConfig.getBoolean("StorageESP", "shulker_boxes", true)) return 0xFFDD78E8;
        return 0;
    }

    private static String key(String value) {
        return value.toLowerCase().replaceAll("[^a-z0-9]+", "_");
    }

    public static void renderEsp(WorldRenderContext context) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null || ESP_BLOCKS.isEmpty()) return;
        var camera = client.gameRenderer.getMainCamera().position();
        var matrices = context.matrices();
        var queue = context.commandQueue();
        if (matrices == null || queue == null) return;

        for (EspBlock target : ESP_BLOCKS) {
            matrices.pushPose();
            matrices.translate(target.pos().getX() - camera.x, target.pos().getY() - camera.y, target.pos().getZ() - camera.z);
            int color = target.color();
            float red = ((color >> 16) & 255) / 255.0f;
            float green = ((color >> 8) & 255) / 255.0f;
            float blue = (color & 255) / 255.0f;
            queue.submitCustomGeometry(matrices, RenderTypes.lines(), (pose, consumer) ->
                    LevelRenderer.renderLineBox(consumer, 0, 0, 0, 1, 1, 1, red, green, blue, 1.0f));
            matrices.popPose();
        }
    }

    private static void restoreGamma(Minecraft client) {
        if (fullbrightActive) {
            if (ModuleConfig.getBoolean("Fullbright", "restore_brightness_on_disable", true)) {
                client.options.gamma().set(previousGamma);
            }
            fullbrightActive = false;
        }
    }
}
