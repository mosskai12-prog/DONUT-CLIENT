package dev.donutclient;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public final class BlockSelection {
    private static final Map<String, Block> OPTIONS = new LinkedHashMap<>();
    static {
        OPTIONS.put("Diamond Ore", Blocks.DIAMOND_ORE);
        OPTIONS.put("Deepslate Diamond Ore", Blocks.DEEPSLATE_DIAMOND_ORE);
        OPTIONS.put("Ancient Debris", Blocks.ANCIENT_DEBRIS);
        OPTIONS.put("Emerald Ore", Blocks.EMERALD_ORE);
        OPTIONS.put("Deepslate Emerald Ore", Blocks.DEEPSLATE_EMERALD_ORE);
        OPTIONS.put("Gold Ore", Blocks.GOLD_ORE);
        OPTIONS.put("Iron Ore", Blocks.IRON_ORE);
        OPTIONS.put("Coal Ore", Blocks.COAL_ORE);
        OPTIONS.put("Redstone Ore", Blocks.REDSTONE_ORE);
        OPTIONS.put("Lapis Ore", Blocks.LAPIS_ORE);
        OPTIONS.put("Copper Ore", Blocks.COPPER_ORE);
        OPTIONS.put("Netherite Block", Blocks.NETHERITE_BLOCK);
        OPTIONS.put("Spawner", Blocks.SPAWNER);
        OPTIONS.put("Obsidian", Blocks.OBSIDIAN);
        OPTIONS.put("Chest", Blocks.CHEST);
        OPTIONS.put("Barrel", Blocks.BARREL);
    }
    private BlockSelection() {}
    public static Set<String> names() { return OPTIONS.keySet(); }
    public static Set<Block> selectedBlocks() {
        Set<Block> result = new LinkedHashSet<>();
        for (String name : OPTIONS.keySet()) {
            if (isSelected(name)) result.add(OPTIONS.get(name));
        }
        return result;
    }
    public static boolean isSelected(String name) {
        return ModuleConfig.getBoolean("BlockESP", "block_" + key(name), false);
    }
    public static void setSelected(String name, boolean value) {
        ModuleConfig.setBoolean("BlockESP", "block_" + key(name), value);
    }
    private static String key(String value) {
        return value.toLowerCase().replaceAll("[^a-z0-9]+", "_");
    }
}
