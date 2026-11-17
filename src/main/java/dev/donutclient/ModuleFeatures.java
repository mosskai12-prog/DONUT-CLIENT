package dev.donutclient;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import com.mojang.blaze3d.vertex.VertexConsumer;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.LinkedHashMap;

public final class ModuleFeatures {
    private static boolean fullbrightActive;
    private static double previousGamma = 0.5;
    private static boolean autoSprintApplied;
    private static final List<BlockPos> blockTargets = new ArrayList<>();
    private static final List<BlockPos> storageTargets = new ArrayList<>();
    private static BlockPos lastScanCenter = null;
    private static int scanCooldown = 0;
    private static int discoveryCooldown = 0;
    private static final Map<Block, Integer> discoveredMachines = new LinkedHashMap<>();
    private static final Map<Block, BlockPos> nearestMachinePositions = new LinkedHashMap<>();
    private static int discoveredTotal = 0;
    private static int nearestMachineDistance = -1;

    private ModuleFeatures() {}

    public static void registerHud() {
        net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry.attachElementBefore(
                net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements.CHAT,
                net.minecraft.resources.Identifier.fromNamespaceAndPath("donutclient", "client_hud"),
                ModuleFeatures::renderHud
        );
        WorldRenderEvents.END_MAIN.register(ModuleFeatures::renderWorldEsp);
    }

    public static void tick(Minecraft client) {
        if (client.player == null || client.level == null) {
            restoreGamma(client);
            autoSprintApplied = false;
            blockTargets.clear();
            storageTargets.clear();
            lastScanCenter = null;
            return;
        }
        updateFullbright(client);
        updateAutoSprint(client);
        scanForTargets(client);
        scanForDiscoveries(client);
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
        } else restoreGamma(client);
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
            if (autoSprintApplied && client.player.isSprinting()) client.player.setSprinting(false);
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

    private static void scanForTargets(Minecraft client) {
        boolean blockEsp = ModuleConfig.isEnabled("BlockESP");
        boolean storageEsp = ModuleConfig.isEnabled("Storage ESP");
        if (!blockEsp && !storageEsp) {
            blockTargets.clear();
            storageTargets.clear();
            lastScanCenter = null;
            return;
        }

        BlockPos center = client.player.blockPosition();
        if (center.equals(lastScanCenter) && scanCooldown-- > 0) return;
        lastScanCenter = center.immutable();
        scanCooldown = 8;
        blockTargets.clear();
        storageTargets.clear();

        boolean wideBlock = blockEsp && ModuleConfig.getBoolean("BlockESP", "wide_scan", false);
        boolean wideStorage = storageEsp && ModuleConfig.getBoolean("Storage ESP", "wide_scan", false);
        int radius = (wideBlock || wideStorage) ? 32 : 20;
        int vertical = 12;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        Set<Block> selected = BlockSelection.selectedBlocks();

        for (int x = center.getX() - radius; x <= center.getX() + radius; x++) {
            for (int y = Math.max(client.level.getMinY(), center.getY() - vertical);
                 y <= Math.min(client.level.getMaxY(), center.getY() + vertical); y++) {
                for (int z = center.getZ() - radius; z <= center.getZ() + radius; z++) {
                    cursor.set(x, y, z);
                    Block block = client.level.getBlockState(cursor).getBlock();
                    if (blockEsp && selected.contains(block)) blockTargets.add(cursor.immutable());
                    if (storageEsp && isStorage(block)) storageTargets.add(cursor.immutable());
                }
            }
        }
    }

    private static void scanForDiscoveries(Minecraft client) {
        if (!ModuleConfig.isEnabled("Sus Chunk Finder")) {
            discoveredMachines.clear();
            nearestMachinePositions.clear();
            discoveredTotal = 0;
            nearestMachineDistance = -1;
            return;
        }
        if (discoveryCooldown-- > 0) return;
        discoveryCooldown = 20;
        discoveredMachines.clear();
        nearestMachinePositions.clear();
        discoveredTotal = 0;
        nearestMachineDistance = -1;

        BlockPos center = client.player.blockPosition();
        int radius = ModuleConfig.getBoolean("Sus Chunk Finder", "wide_scan", false) ? 48 : 24;
        int vertical = 8;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        double nearestSqr = Double.MAX_VALUE;

        for (int x = center.getX() - radius; x <= center.getX() + radius; x++) {
            for (int y = Math.max(client.level.getMinY(), center.getY() - vertical);
                 y <= Math.min(client.level.getMaxY(), center.getY() + vertical); y++) {
                for (int z = center.getZ() - radius; z <= center.getZ() + radius; z++) {
                    cursor.set(x, y, z);
                    if (!client.level.hasChunkAt(cursor)) continue;
                    Block block = client.level.getBlockState(cursor).getBlock();
                    if (!isDiscoveryMachine(block)) continue;
                    BlockPos found = cursor.immutable();
                    discoveredMachines.merge(block, 1, Integer::sum);
                    nearestMachinePositions.putIfAbsent(block, found);
                    discoveredTotal++;
                    double dx = x + 0.5 - client.player.getX();
                    double dy = y + 0.5 - client.player.getY();
                    double dz = z + 0.5 - client.player.getZ();
                    double distanceSqr = dx * dx + dy * dy + dz * dz;
                    if (distanceSqr < nearestSqr) {
                        nearestSqr = distanceSqr;
                        nearestMachineDistance = (int) Math.round(Math.sqrt(distanceSqr));
                    }
                }
            }
        }
    }

    private static boolean isDiscoveryMachine(Block block) {
        return block == Blocks.FURNACE || block == Blocks.BLAST_FURNACE
                || block == Blocks.SMOKER || block == Blocks.CRAFTING_TABLE
                || block == Blocks.ANVIL || block == Blocks.CHIPPED_ANVIL
                || block == Blocks.DAMAGED_ANVIL || block == Blocks.ENCHANTING_TABLE
                || block == Blocks.BREWING_STAND || block == Blocks.SMITHING_TABLE
                || block == Blocks.STONECUTTER || block == Blocks.LOOM
                || block == Blocks.CARTOGRAPHY_TABLE;
    }

    private static String discoveryName(Block block) {
        if (block == Blocks.FURNACE) return "Furnace";
        if (block == Blocks.BLAST_FURNACE) return "Blast Furnace";
        if (block == Blocks.SMOKER) return "Smoker";
        if (block == Blocks.CRAFTING_TABLE) return "Crafting Table";
        if (block == Blocks.ANVIL || block == Blocks.CHIPPED_ANVIL || block == Blocks.DAMAGED_ANVIL) return "Anvil";
        if (block == Blocks.ENCHANTING_TABLE) return "Enchanting Table";
        if (block == Blocks.BREWING_STAND) return "Brewing Stand";
        if (block == Blocks.SMITHING_TABLE) return "Smithing Table";
        if (block == Blocks.STONECUTTER) return "Stonecutter";
        if (block == Blocks.LOOM) return "Loom";
        return "Cartography Table";
    }

    private static void renderDiscoveriesPanel(GuiGraphics graphics, Minecraft client) {
        if (!ModuleConfig.isEnabled("Sus Chunk Finder")) return;
        int panelW = 356;
        int panelH = 194;
        int x = client.getWindow().getGuiScaledWidth() - panelW - 18;
        int y = client.getWindow().getGuiScaledHeight() - panelH - 18;
        graphics.fill(x, y, x + panelW, y + panelH, 0xEE172A38);
        graphics.fill(x, y, x + panelW, y + 1, 0xFF55D8D0);
        graphics.fill(x, y, x + 1, y + panelH, 0xFF55D8D0);
        graphics.fill(x + panelW - 1, y, x + panelW, y + panelH, 0xFF55D8D0);
        graphics.fill(x, y + panelH - 1, x + panelW, y + panelH, 0xFF55D8D0);

        graphics.fill(x + 14, y + 15, x + 18, y + 34, 0xFF55D8D0);
        graphics.drawString(client.font, "SPECTRE / DISCOVERIES", x + 28, y + 17, 0xFFF0F4F6, false);
        if (ModuleConfig.getBoolean("Sus Chunk Finder", "show_distance", true) && nearestMachineDistance >= 0) {
            String distance = nearestMachineDistance + "m";
            graphics.drawString(client.font, distance, x + panelW - 14 - client.font.width(distance), y + 17, 0xFF8DE8DF, false);
        }

        String clue = discoveredMachines.isEmpty() ? "No machine clues nearby" :
                discoveryName(discoveredMachines.keySet().iterator().next());
        graphics.drawString(client.font, clue, x + 16, y + 45, 0xFFFFA36C, false);
        graphics.drawString(client.font,
                discoveredMachines.size() + " clue types together / nearby objects",
                x + 16, y + 83, 0xFFD5E0E7, false);

        int itemX = x + 18;
        int itemY = y + 105;
        int shown = 0;
        for (Map.Entry<Block, Integer> entry : discoveredMachines.entrySet()) {
            if (shown >= 5) break;
            if (ModuleConfig.getBoolean("Sus Chunk Finder", "show_icons", true)) {
                graphics.renderItem(new ItemStack(entry.getKey().asItem()), itemX, itemY);
            }
            graphics.drawString(client.font, Integer.toString(entry.getValue()),
                    itemX + 20, itemY + 5, 0xFFF2F5F7, false);
            graphics.fill(itemX, itemY + 23, itemX + 35, itemY + 25, 0xFFFF9A62);
            itemX += 43;
            shown++;
        }

        graphics.fill(x + 16, y + 153, x + 21, y + 165, 0xFFFF9A62);
        graphics.drawString(client.font, discoveredTotal + " Machines", x + 27, y + 154, 0xFFD5E0E7, false);
        graphics.drawString(client.font, "Loaded objects / not proof of a base",
                x + 16, y + 177, 0xFF9EAFBA, false);
    }

    private static boolean isStorage(Block block) {
        return block == Blocks.CHEST || block == Blocks.TRAPPED_CHEST
                || block == Blocks.ENDER_CHEST || block == Blocks.BARREL
                || block instanceof ShulkerBoxBlock;
    }

    private static void renderWorldEsp(WorldRenderContext context) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null || client.options.hideGui) return;
        boolean blockOn = ModuleConfig.isEnabled("BlockESP");
        boolean storageOn = ModuleConfig.isEnabled("Storage ESP");
        if (!blockOn && !storageOn) return;

        PoseStack matrices = context.matrices();
        VertexConsumer lines = context.consumers().getBuffer(RenderTypes.lines());
        Vec3 camera = client.gameRenderer.getMainCamera().position();
        matrices.pushPose();
        matrices.translate(-camera.x, -camera.y, -camera.z);

        if (blockOn) {
            int opacity = Math.max(0, Math.min(255, ModuleConfig.getInt("BlockESP", "opacity", 210)));
            int color = (opacity << 24) | 0x00FFD13B;
            for (BlockPos pos : blockTargets) {
                ShapeRenderer.renderShape(matrices, lines, client.level.getBlockState(pos).getShape(client.level, pos),
                        pos.getX(), pos.getY(), pos.getZ(), color, 1.5f);
            }
        }
        if (storageOn) {
            for (BlockPos pos : storageTargets) {
                AABB box = new AABB(pos).inflate(0.004);
                ShapeRenderer.renderShape(matrices, lines, client.level.getBlockState(pos).getShape(client.level, pos), pos.getX(), pos.getY(), pos.getZ(), 0xFF33D9FF, 1.5f);
            }
        }
        matrices.popPose();
    }

    private static void renderModuleList(GuiGraphics graphics, Minecraft client) {
        if (!ModuleConfig.getBoolean("Module List", "enabled", true)) return;
        List<String> active = new ArrayList<>();
        for (String name : new String[]{"Amethyst Bypass", "Sus Chunk Finder", "Fullbright",
                "BlockESP", "Storage ESP", "Toggle Sprint", "Coordinates HUD", "FPS Counter",
                "Direction HUD", "Armor HUD", "Progress Bar", "Light Debug"}) {
            if (ModuleConfig.isEnabled(name)) active.add(name);
        }
        int y = 8;
        int right = client.getWindow().getGuiScaledWidth() - 8;
        for (String name : active) {
            int textWidth = client.font.width(name);
            int x = right - textWidth - 8;
            graphics.fill(x - 3, y - 2, right, y + 11, 0x990D1119);
            graphics.fill(right - 2, y - 2, right, y + 11, 0xFF9CFB65);
            graphics.drawString(client.font, name, right - textWidth - 6, y, 0xFFF4F5F7, true);
            y += 15;
        }
    }

    private static void renderHud(GuiGraphics graphics, DeltaTracker tickCounter) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.options.hideGui) return;
        int y = 8;
        renderModuleList(graphics, client);
        renderDiscoveriesPanel(graphics, client);
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
            y += 12;
        }
        if (ModuleConfig.isEnabled("Direction HUD")) {
            graphics.drawString(client.font, "FACING  " + client.player.getDirection().getName().toUpperCase(),
                    8, y, 0xFFE9A9C5, true);
            y += 12;
        }
        if (ModuleConfig.isEnabled("Armor HUD")) {
            StringBuilder armor = new StringBuilder("ARMOR  ");
            for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
                var stack = client.player.getItemBySlot(slot);
                if (stack.isEmpty()) armor.append("--  ");
                else {
                    int max = stack.getMaxDamage();
                    int remaining = max <= 0 ? 100 : (max - stack.getDamageValue()) * 100 / max;
                    armor.append(remaining).append("%  ");
                }
            }
            graphics.drawString(client.font, armor.toString(), 8, y, 0xFF80D9F2, true);
            y += 12;
        }
        if (ModuleConfig.isEnabled("Light Debug")) {
            BlockPos pos = client.player.blockPosition();
            boolean showBlock = ModuleConfig.getBoolean("Light Debug", "show_block_light", true);
            boolean showSky = ModuleConfig.getBoolean("Light Debug", "show_sky_light", true);
            StringBuilder light = new StringBuilder("LIGHT  ");
            if (showBlock) light.append("BLOCK ").append(client.level.getBrightness(LightLayer.BLOCK, pos)).append("  ");
            if (showSky) light.append("SKY ").append(client.level.getBrightness(LightLayer.SKY, pos));
            graphics.drawString(client.font, light.toString(), 8, y, 0xFFFFD27A, true);
            y += 12;
        }
        if (ModuleConfig.isEnabled("Progress Bar")) {
            int barWidth = 142;
            int barX = (client.getWindow().getGuiScaledWidth() - barWidth) / 2;
            int barY = client.getWindow().getGuiScaledHeight() - 46;
            float progress = Math.max(0.0f, Math.min(1.0f, client.player.experienceProgress));
            graphics.fill(barX, barY, barX + barWidth, barY + 7, 0xCC111722);
            graphics.fill(barX + 1, barY + 1, barX + 1 + Math.round((barWidth - 2) * progress), barY + 6, 0xFF9CFB65);
            if (ModuleConfig.getBoolean("Progress Bar", "show_level", true)) {
                String level = "LEVEL " + client.player.experienceLevel;
                graphics.drawString(client.font, level, barX, barY - 12, 0xFFF1F3F5, true);
            }
            if (ModuleConfig.getBoolean("Progress Bar", "show_percentage", true)) {
                String percent = Math.round(progress * 100) + "%";
                graphics.drawString(client.font, percent, barX + barWidth - client.font.width(percent), barY - 12, 0xFF9CFB65, true);
            }
        }
    }
}
