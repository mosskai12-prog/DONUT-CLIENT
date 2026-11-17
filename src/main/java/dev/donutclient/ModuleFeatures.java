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

public final class ModuleFeatures {
    private static boolean fullbrightActive;
    private static double previousGamma = 0.5;
    private static boolean autoSprintApplied;
    private static final List<BlockPos> blockTargets = new ArrayList<>();
    private static final List<BlockPos> storageTargets = new ArrayList<>();
    private static BlockPos lastScanCenter = null;
    private static int scanCooldown = 0;

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
            int opacity = ModuleConfig.getInt("BlockESP", "opacity", 210);
            float alpha = Math.max(0.15f, Math.min(1.0f, opacity / 255.0f));
            for (BlockPos pos : blockTargets) {
                AABB box = new AABB(pos).inflate(0.002);
                ShapeRenderer.renderShape(matrices, lines, client.level.getBlockState(pos).getShape(client.level, pos), pos.getX(), pos.getY(), pos.getZ(), (Math.max(0, Math.min(255, opacity)) << 24) | 0x00FFD13B, 1.5f);
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
