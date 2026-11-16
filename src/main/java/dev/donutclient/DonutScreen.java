package dev.donutclient;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DonutScreen extends Screen {
    private final List<Module> modules = new ArrayList<>();
    private final Map<String, Float> animations = new HashMap<>();
    private String category = "All";
    private Module selected;
    private float open = 0.0f;

    private static final int BG = 0xFF0B0D11;
    private static final int PANEL = 0xFF12151B;
    private static final int PANEL_ALT = 0xFF171B23;
    private static final int CARD = 0xFF1B2029;
    private static final int CARD_HOVER = 0xFF252B36;
    private static final int BORDER = 0xFF2A303B;
    private static final int WHITE = 0xFFF2F4F7;
    private static final int MUTED = 0xFF8993A2;
    private static final int RED = 0xFFE34B5B;
    private static final int GREEN = 0xFF63D69A;
    private static final String[] CATEGORIES = {
            "All", "Combat", "Movement", "Render", "World", "Player", "HUD", "Favorites"
    };

    public DonutScreen() {
        super(Component.literal("DONUT CLIENT"));
        modules.add(new Module("Base Finder", "World", "Finds configured bases."));
        modules.add(new Module("Fullbright", "Render", "Brightens dark areas."));
        modules.add(new Module("Fly", "Movement", "Flight movement module."));
        modules.add(new Module("Progress Bar", "HUD", "Displays a progress bar."));
        modules.add(new Module("Name Hider", "Render", "Hides selected names."));
        modules.add(new Module("Hotbar", "HUD", "Custom hotbar display."));
        modules.add(new Module("Amethyst Scanner", "World", "Scans amethyst blocks."));
        modules.add(new Module("Light Debug", "Render", "Displays light information."));
        modules.add(new Module("Sound Detector", "World", "Detects nearby sounds."));
        modules.add(new Module("BlockESP", "Render", "Highlights selected ores."));
        modules.add(new Module("StorageESP", "Render", "Highlights nearby storage blocks."));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private int panelWidth() { return Math.min(1080, width - 28); }
    private int panelHeight() { return Math.min(660, height - 28); }
    private int left() { return (width - panelWidth()) / 2; }
    private int top() { return (height - panelHeight()) / 2; }
    private int navHeight() { return 66; }
    private int contentTop() { return top() + 126; }
    private int listLeft() { return left() + 26; }
    private int listWidth() { return Math.max(250, (panelWidth() - 390) / 2); }
    private int detailsLeft() { return listLeft() + listWidth() + 18; }
    private int detailsWidth() { return left() + panelWidth() - 26 - detailsLeft(); }

    private List<Module> visibleModules() {
        return modules.stream().filter(m ->
                category.equals("All")
                        || (category.equals("Favorites") && m.isFavorite())
                        || m.getCategory().equals(category)
        ).toList();
    }

    private String[] options(Module module) {
        return switch (module.getName()) {
            case "Fullbright" -> new String[]{"Maximum brightness", "Restore brightness on disable"};
            case "Base Finder" -> new String[]{"Show distance", "Show coordinates"};
            case "Fly" -> new String[]{"Disable when grounded", "Show flight indicator"};
            case "Progress Bar" -> new String[]{"Show percentage", "Compact display"};
            case "Name Hider" -> new String[]{"Hide player names", "Hide mob names"};
            case "Hotbar" -> new String[]{"Show item counts", "Compact layout"};
            case "Amethyst Scanner" -> new String[]{"Show cluster markers", "Show distance"};
            case "Light Debug" -> new String[]{"Show block light", "Show sky light"};
            case "Sound Detector" -> new String[]{"Show direction", "Show distance"};
            case "BlockESP" -> new String[]{
                    "Diamond Ore", "Deepslate Diamond Ore", "Ancient Debris", "Emerald Ore",
                    "Gold Ore", "Iron Ore", "Copper Ore", "Redstone Ore", "Lapis Lazuli Ore", "Coal Ore"
            };
            case "StorageESP" -> new String[]{
                    "Chests", "Trapped Chests", "Ender Chests", "Barrels", "Shulker Boxes"
            };
            default -> new String[]{"Show module indicator", "Enable notifications"};
        };
    }

    private String settingKey(String value) {
        return value.toLowerCase().replaceAll("[^a-z0-9]+", "_");
    }

    private void text(GuiGraphics g, String value, int x, int y, int color) {
        g.drawString(font, value, x, y, color, false);
    }

    private void pill(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + h, color);
    }

    private int mix(int a, int b, float amount) {
        int r = (int) (((a >> 16) & 255) * (1 - amount) + ((b >> 16) & 255) * amount);
        int gr = (int) (((a >> 8) & 255) * (1 - amount) + ((b >> 8) & 255) * amount);
        int bl = (int) ((a & 255) * (1 - amount) + (b & 255) * amount);
        return 0xFF000000 | (r << 16) | (gr << 8) | bl;
    }

    private void drawSwitch(GuiGraphics g, int x, int y, boolean enabled, String id) {
        float target = enabled ? 1.0f : 0.0f;
        float value = animations.compute(id, (k, old) ->
                old == null ? target : old + (target - old) * 0.32f);
        pill(g, x, y, 34, 18, mix(0xFF343B47, 0xFF286747, value));
        int knob = x + 2 + Math.round(16 * value);
        pill(g, knob, y + 2, 14, 14, WHITE);
    }

    private void drawModuleRow(GuiGraphics g, Module module, int index, int mouseX, int mouseY) {
        int x = listLeft();
        int y = contentTop() + index * 49;
        int w = listWidth();
        boolean hover = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + 43;
        boolean active = module == selected;
        pill(g, x, y, w, 43, active ? 0xFF302129 : hover ? CARD_HOVER : CARD);
        pill(g, x, y, 3, 43, active ? RED : 0xFF343B47);
        pill(g, x + 14, y + 12, 19, 19, active ? 0xFF63303A : 0xFF303744);
        text(g, module.getName().substring(0, 1), x + 20, y + 18, active ? WHITE : MUTED);
        text(g, module.getName(), x + 43, y + 9, WHITE);
        text(g, module.getCategory().toUpperCase(), x + 43, y + 25, MUTED);
        text(g, module.isFavorite() ? "★" : "☆", x + w - 78, y + 14,
                module.isFavorite() ? RED : MUTED);
        drawSwitch(g, x + w - 51, y + 12, module.isEnabled(), "module." + module.getName());
    }

    private void drawSettings(GuiGraphics g, int mouseX, int mouseY) {
        int x = detailsLeft();
        int y = contentTop();
        int w = detailsWidth();
        int h = panelHeight() - 156;
        pill(g, x, y, w, h, PANEL);
        pill(g, x, y, w, 1, BORDER);

        if (selected == null) {
            text(g, "MODULE DETAILS", x + 20, y + 24, RED);
            text(g, "Choose a module", x + 20, y + 64, WHITE);
            text(g, "Select something from the list", x + 20, y + 86, MUTED);
            text(g, "to see its controls and settings.", x + 20, y + 101, MUTED);
            pill(g, x + 20, y + 132, w - 40, 1, BORDER);
            text(g, "YOUR CLIENT", x + 20, y + 154, MUTED);
            long enabled = modules.stream().filter(Module::isEnabled).count();
            text(g, enabled + " modules currently enabled", x + 20, y + 177, WHITE);
            return;
        }

        text(g, "MODULE SETTINGS", x + 20, y + 23, RED);
        text(g, selected.getName(), x + 20, y + 52, WHITE);
        text(g, selected.getDescription(), x + 20, y + 71, MUTED);

        int toggleY = y + 96;
        pill(g, x + 16, toggleY, w - 32, 42, PANEL_ALT);
        text(g, "Module enabled", x + 29, toggleY + 16, WHITE);
        drawSwitch(g, x + w - 66, toggleY + 12, selected.isEnabled(), "settings.enabled");

        text(g, "OPTIONS", x + 20, y + 164, MUTED);
        String[] settings = options(selected);
        int rowY = y + 180;
        int rowHeight = settings.length > 6 ? 29 : 42;
        for (int i = 0; i < settings.length; i++) {
            int yy = rowY + i * (rowHeight + 5);
            if (yy + rowHeight > y + h - 30) break;
            boolean value = ModuleConfig.getBoolean(selected.getName(), settingKey(settings[i]), true);
            pill(g, x + 16, yy, w - 32, rowHeight, PANEL_ALT);
            text(g, settings[i], x + 28, yy + (rowHeight - 9) / 2, WHITE);
            drawSwitch(g, x + w - 66, yy + (rowHeight - 18) / 2, value,
                    "setting." + selected.getName() + "." + settingKey(settings[i]));
        }
        text(g, "Changes save automatically", x + 20, y + h - 18, MUTED);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        open = Math.min(1.0f, open + delta * 4.0f);
        int offset = Math.round((1.0f - open) * 15.0f);
        int x = left();
        int y = top() + offset;
        int w = panelWidth();
        int h = panelHeight();
        int bottom = y + h;

        g.fill(0, 0, width, height, 0xD9000000);
        pill(g, x, y, w, h, BG);

        // Header: brand, status and close hint.
        pill(g, x, y, w, navHeight(), PANEL);
        pill(g, x, y, 4, navHeight(), RED);
        pill(g, x + 22, y + 16, 34, 34, RED);
        text(g, "D", x + 34, y + 27, WHITE);
        text(g, "DONUT", x + 68, y + 17, WHITE);
        text(g, "CLIENT  /  UNIVERSAL", x + 68, y + 36, MUTED);
        long active = modules.stream().filter(Module::isEnabled).count();
        pill(g, x + w - 190, y + 19, 104, 28, PANEL_ALT);
        pill(g, x + w - 178, y + 30, 6, 6, GREEN);
        text(g, active + " ACTIVE", x + w - 164, y + 29, WHITE);
        text(g, "RIGHT SHIFT  ×", x + w - 76, y + 29, MUTED);
        pill(g, x, y + navHeight(), w, 1, BORDER);

        // Horizontal category navigation replaces the old crowded sidebar.
        int tabX = x + 24;
        int tabY = y + 83;
        text(g, "BROWSE", tabX, y + 74, MUTED);
        for (String item : CATEGORIES) {
            int tabW = Math.max(47, font.width(item) + 20);
            boolean chosen = category.equals(item);
            boolean hover = mouseX >= tabX && mouseX <= tabX + tabW
                    && mouseY >= tabY && mouseY <= tabY + 27;
            pill(g, tabX, tabY, tabW, 27,
                    chosen ? 0xFF482630 : hover ? CARD_HOVER : PANEL);
            if (chosen) pill(g, tabX, tabY + 25, tabW, 2, RED);
            text(g, item, tabX + 10, tabY + 9, chosen ? WHITE : MUTED);
            tabX += tabW + 7;
        }

        // Main workspace.
        int headY = y + 121;
        text(g, "MODULE LIBRARY", x + 26, headY, WHITE);
        text(g, "Pick a feature, then configure it on the right.", x + 26, headY + 17, MUTED);
        text(g, "FEATURE", listLeft(), headY + 42, MUTED);
        text(g, "CONFIGURATION", detailsLeft(), headY + 42, MUTED);

        List<Module> list = visibleModules();
        int maxRows = Math.max(1, (h - 205) / 49);
        for (int i = 0; i < Math.min(list.size(), maxRows); i++) {
            drawModuleRow(g, list.get(i), i, mouseX, mouseY);
        }
        if (list.isEmpty()) {
            text(g, "No favourites yet", listLeft() + 16, contentTop() + 18, MUTED);
        }
        drawSettings(g, mouseX, mouseY);

        // Bottom utility strip.
        pill(g, x, bottom - 35, w, 35, PANEL);
        pill(g, x, bottom - 36, w, 1, BORDER);
        text(g, "DONUT CLIENT", x + 22, bottom - 22, MUTED);
        text(g, "11 MODULES", x + w - 100, bottom - 22, MUTED);
        super.render(g, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) return super.mouseClicked(event, doubleClick);

        double mx = event.x();
        double my = event.y();
        int x = left();
        int y = top();
        int tabX = x + 24;
        int tabY = y + 83;

        for (String item : CATEGORIES) {
            int tabW = Math.max(47, font.width(item) + 20);
            if (mx >= tabX && mx <= tabX + tabW && my >= tabY && my <= tabY + 27) {
                category = item;
                selected = null;
                return true;
            }
            tabX += tabW + 7;
        }

        List<Module> list = visibleModules();
        int maxRows = Math.max(1, (panelHeight() - 205) / 49);
        for (int i = 0; i < Math.min(list.size(), maxRows); i++) {
            Module module = list.get(i);
            int rowX = listLeft();
            int rowY = contentTop() + i * 49;
            int rowW = listWidth();
            if (mx >= rowX && mx <= rowX + rowW && my >= rowY && my <= rowY + 43) {
                if (mx >= rowX + rowW - 82 && mx < rowX + rowW - 56) {
                    module.toggleFavorite();
                } else if (mx >= rowX + rowW - 53) {
                    module.toggle();
                } else {
                    selected = module;
                }
                return true;
            }
        }

        if (selected != null) {
            int x2 = detailsLeft();
            int y2 = contentTop();
            int w2 = detailsWidth();
            int h2 = panelHeight() - 156;
            int toggleY = y2 + 96;
            if (mx >= x2 + 16 && mx <= x2 + w2 - 16
                    && my >= toggleY && my <= toggleY + 42) {
                selected.toggle();
                return true;
            }

            String[] settings = options(selected);
            int rowHeight = settings.length > 6 ? 29 : 42;
            int rowY = y2 + 180;
            for (int i = 0; i < settings.length; i++) {
                int yy = rowY + i * (rowHeight + 5);
                if (yy + rowHeight > y2 + h2 - 30) break;
                if (mx >= x2 + 16 && mx <= x2 + w2 - 16
                        && my >= yy && my <= yy + rowHeight) {
                    String setting = settingKey(settings[i]);
                    boolean value = ModuleConfig.getBoolean(selected.getName(), setting, true);
                    ModuleConfig.setBoolean(selected.getName(), setting, !value);
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }
}
