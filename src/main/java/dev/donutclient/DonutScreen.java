package dev.donutclient;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DonutScreen extends Screen {
    private final List<Module> modules = new ArrayList<>();
    private final Map<String, Float> switchAnimations = new HashMap<>();
    private final Map<String, Float> hoverAnimations = new HashMap<>();
    private String category = "All modules";
    private String page = "Modules";
    private Module selected;
    private String search = "";
    private boolean searchFocused;
    private boolean enabledOnly;
    private float open = 0f;
    private boolean dragging;
    private double dragOffsetX;
    private double dragOffsetY;
    private int windowX = Integer.MIN_VALUE;
    private int windowY = Integer.MIN_VALUE;

    private static final int OUTER = 0xFF10131B;
    private static final int PANEL = 0xFF1B2030;
    private static final int PANEL_DARK = 0xFF151A27;
    private static final int CARD = 0xFF252B3B;
    private static final int CARD_HOVER = 0xFF333D50;
    private static final int BORDER = 0xFF46516A;
    private static final int WHITE = 0xFFE7E9ED;
    private static final int MUTED = 0xFFA5A9B1;
    private static final int DIM = 0xFF737982;
    private static final int LIME = 0xFF9CFB65;
    private static final int GREEN = 0xFF9CFB65;
    private static final int PINK = 0xFFFF79C6;

    private static final String[] CATEGORIES = {
            "All modules", "Visual", "Blatant", "Streaming", "Favorites"
    };

    public DonutScreen() {
        super(Component.literal("Fizzy Client"));
        modules.add(new Module("Base Finder", "Visual", "Finds configured base markers."));
        modules.add(new Module("Fullbright", "Visual", "Makes dark areas easier to see."));
        modules.add(new Module("Fly", "Blatant", "Flight controls. Requires a supported game mode."));
        modules.add(new Module("Progress Bar", "Visual", "Displays progress information."));
        modules.add(new Module("Name Hider", "Streaming", "Hides player names in supported HUD contexts."));
        modules.add(new Module("Hotbar", "Visual", "Displays a configurable hotbar overlay."));
        modules.add(new Module("Amethyst Scanner", "Visual", "Displays amethyst-related information."));
        modules.add(new Module("Light Debug", "Visual", "Displays lighting debug information."));
        modules.add(new Module("Sound Detector", "Visual", "Displays sound event indicators."));
        modules.add(new Module("BlockESP", "Visual", "Highlights the block types you select."));
        modules.add(new Module("Storage ESP", "Visual", "Highlights chests, barrels, ender chests and shulker boxes."));
        modules.add(new Module("Coordinates HUD", "Visual", "Displays your current XYZ coordinates."));
        modules.add(new Module("FPS Counter", "Visual", "Displays your current frame rate."));
        modules.add(new Module("Direction HUD", "Visual", "Shows which compass direction you are facing."));
        modules.add(new Module("Armor HUD", "Visual", "Shows the remaining durability of your equipped armor."));
        modules.add(new Module("Toggle Sprint", "Blatant", "Automatically sprints while moving."));
        selected = findModule("BlockESP");
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private int panelWidth() { return Math.min(826, width - 36); }
    private int panelHeight() { return Math.min(520, height - 36); }
    private int left() {
        if (windowX == Integer.MIN_VALUE) windowX = (width - panelWidth()) / 2;
        return Math.max(4, Math.min(width - panelWidth() - 4, windowX));
    }
    private int top() {
        if (windowY == Integer.MIN_VALUE) windowY = (height - panelHeight()) / 2;
        return Math.max(4, Math.min(height - panelHeight() - 4, windowY));
    }
    private int sideWidth() { return 120; }
    private int settingsWidth() { return Math.max(205, Math.min(250, panelWidth() / 4)); }
    private int mainLeft() { return left() + sideWidth() + 12; }
    private int settingsLeft() { return left() + panelWidth() - settingsWidth() - 12; }
    private int mainWidth() { return settingsLeft() - mainLeft() - 10; }
    private int bodyTop() { return top() + 55; }
    private int bodyBottom() { return top() + panelHeight() - 24; }

    private void fill(GuiGraphics g, int x, int y, int w, int h, int color) {
        if (w > 0 && h > 0) g.fill(x, y, x + w, y + h, color);
    }

    private void roundRect(GuiGraphics g, int x, int y, int w, int h, int radius, int color) {
        if (w <= 0 || h <= 0) return;
        int r = Math.max(1, Math.min(radius, Math.min(w, h) / 2));
        g.fill(x + r, y, x + w - r, y + h, color);
        g.fill(x, y + r, x + w, y + h - r, color);
        for (int row = 0; row < r; row++) {
            int inset = r - 1 - (int) Math.floor(Math.sqrt(r * r - (r - row - 1) * (r - row - 1)));
            g.fill(x + inset, y + row, x + w - inset, y + row + 1, color);
            g.fill(x + inset, y + h - row - 1, x + w - inset, y + h - row, color);
        }
    }

    private Component uiText(String value) {
        return Component.literal(value);
    }

    private int uiWidth(String value) {
        return font.width(uiText(value));
    }

    private void border(GuiGraphics g, int x, int y, int w, int h, int color) {
        fill(g, x, y, w, 1, color);
        fill(g, x, y + h - 1, w, 1, color);
        fill(g, x, y, 1, h, color);
        fill(g, x + w - 1, y, 1, h, color);
    }

    private void text(GuiGraphics g, String value, int x, int y, int color) {
        g.drawString(font, uiText(value), x, y, color, false);
    }

    private void centered(GuiGraphics g, String value, int x, int y, int color) {
        text(g, value, x - uiWidth(value) / 2, y, color);
    }

    private int mix(int a, int b, float t) {
        int r = (int) (((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
        int gr = (int) (((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
        int bl = (int) ((a & 255) * (1 - t) + (b & 255) * t);
        return 0xFF000000 | (r << 16) | (gr << 8) | bl;
    }

    private void donutMark(GuiGraphics g, int x, int y) {
        fill(g, x, y + 2, 18, 14, 0xFFB77C99);
        fill(g, x + 2, y, 14, 18, 0xFFB77C99);
        fill(g, x + 3, y + 3, 12, 12, 0xFF28232A);
        fill(g, x + 6, y + 6, 6, 6, 0xFF101216);
        fill(g, x + 4, y + 2, 4, 2, 0xFFFFD7E7);
    }

    private void drawSwitch(GuiGraphics g, int x, int y, boolean active, String id) {
        float target = active ? 1f : 0f;
        float value = switchAnimations.compute(id, (key, old) ->
                old == null ? target : old + (target - old) * 0.34f);
        int track = mix(0xFF51483F, 0xFF9CCB67, value);
        roundRect(g, x, y, 21, 12, 6, track);
        int knobX = x + 2 + Math.round(9 * value);
        roundRect(g, knobX, y + 2, 8, 8, 4, 0xFFF7F7F3);
    }

    private List<Module> visibleModules() {
        return modules.stream()
                .filter(m -> category.equals("All modules")
                        || (category.equals("Favorites") && m.isFavorite())
                        || m.getCategory().equals(category))
                .filter(m -> !enabledOnly || m.isEnabled())
                .filter(m -> search.isBlank()
                        || m.getName().toLowerCase().contains(search.toLowerCase())
                        || m.getDescription().toLowerCase().contains(search.toLowerCase()))
                .toList();
    }

    private String[] settingsFor(Module module) {
        return switch (module.getName()) {
            case "BlockESP" -> new String[]{"Wide scan"};
            case "Storage ESP" -> new String[]{"Wide scan"};
            case "Fullbright" -> new String[]{"Maximum brightness", "Restore brightness on disable"};
            case "Toggle Sprint" -> new String[]{"Require forward movement", "Stop while sneaking"};
            default -> new String[0];
        };
    }

    private String settingKey(String value) {
        return value.toLowerCase().replaceAll("[^a-z0-9]+", "_");
    }

    private void drawHeader(GuiGraphics g, int x, int y, int w) {
        roundRect(g, x, y, w, 48, 10, PANEL_DARK);
        donutMark(g, x + 13, y + 14);
        text(g, "FIZZY", x + 37, y + 12, 0xFFD6EAF1);
        text(g, "MODULE CONTROL", x + 37, y + 25, MUTED);

        String[] tabs = {"Modules", "HUD", "Appearance"};
        int tabRight = x + w - 18;
        for (int i = tabs.length - 1; i >= 0; i--) {
            String tab = tabs[i];
            int tw = font.width(tab) + 22;
            int tx = tabRight - tw;
            boolean active = page.equals(tab);
            if (active) {
                fill(g, tx, y + 10, tw, 27, 0xFF30342E);
                fill(g, tx + 10, y + 36, tw - 20, 2, LIME);
            }
            centered(g, tab, tx + tw / 2, y + 19, active ? WHITE : MUTED);
            tabRight = tx - 8;
        }
        fill(g, x, y + 47, w, 1, BORDER);
    }

    private void drawSidebar(GuiGraphics g, int x, int y, int h, int mouseX, int mouseY) {
        fill(g, x, y, sideWidth(), h, PANEL_DARK);
        fill(g, x + sideWidth() - 1, y, 1, h, 0xFF292D34);
        text(g, "WORKSPACE", x + 12, y + 16, 0xFF9BD8E5);

        int yy = y + 29;
        for (String item : CATEGORIES) {
            boolean active = category.equals(item);
            boolean hover = mouseX >= x + 7 && mouseX <= x + sideWidth() - 8
                    && mouseY >= yy && mouseY <= yy + 25;
            if (active || hover) {
                fill(g, x + 7, yy, sideWidth() - 15, 25,
                        active ? 0xFF30342E : CARD_HOVER);
            }
            if (active) fill(g, x + 7, yy + 4, 2, 17, LIME);
            String symbol = switch (item) {
                case "All modules" -> "▣";
                case "Visual" -> "◈";
                case "Blatant" -> "△";
                case "Streaming" -> "▢";
                default -> "☆";
            };
            text(g, symbol, x + 15, yy + 8, active ? LIME : DIM);
            text(g, item, x + 31, yy + 8, active ? WHITE : MUTED);
            yy += 29;
        }

        text(g, enabledOnly ? "●" : "○", x + 13, y + h - 23, enabledOnly ? LIME : DIM);
        text(g, "Enabled only", x + 25, y + h - 23, enabledOnly ? WHITE : MUTED);
    }

    private void drawWorkspace(GuiGraphics g, int x, int y, int w, int mouseX, int mouseY) {
        fill(g, x, y, w, 69, PANEL);
        border(g, x, y, w, 69, BORDER);
        text(g, "YOUR MODULE DESK", x + 11, y + 10, LIME);
        String heading = category.equals("Favorites") ? "Favorites"
                : category.equals("All modules") ? "All modules" : category;
        text(g, heading, x + 11, y + 27, WHITE);
        text(g, "Hover to explore. Select to configure.", x + 11, y + 45, MUTED);
        donutMark(g, x + w - 56, y + 15);

        int searchY = y + 80;
        roundRect(g, x, searchY, w, 27, 7, 0xFF252D3D);
        border(g, x, searchY, w, 27, searchFocused ? LIME : BORDER);
        text(g, "⌕", x + 10, searchY + 8, MUTED);
        text(g, search.isEmpty() ? "Search modules..." : search,
                x + 27, searchY + 9, search.isEmpty() ? DIM : WHITE);
        if (searchFocused) fill(g, x + 27 + font.width(search), searchY + 7, 1, 13, LIME);

        int sectionY = searchY + 35;
        int gap = 8;
        int colW = (w - gap) / 2;
        text(g, "INACTIVE", x + 9, sectionY, MUTED);
        text(g, "ACTION", x + colW + gap + 9, sectionY, MUTED);
        fill(g, x, sectionY + 17, colW, 1, BORDER);
        fill(g, x + colW + gap, sectionY + 17, colW, 1, BORDER);

        List<Module> visible = visibleModules();
        int cardY = sectionY + 26;
        int cardH = 49;
        int rowGap = 8;
        int rows = Math.max(1, (bodyBottom() - cardY - 4) / (cardH + rowGap));
        for (int i = 0; i < visible.size() && i < rows * 2; i++) {
            int col = i % 2;
            int row = i / 2;
            int cx = x + col * (colW + gap);
            int cy = cardY + row * (cardH + rowGap);
            Module module = visible.get(i);
            boolean hover = mouseX >= cx && mouseX <= cx + colW
                    && mouseY >= cy && mouseY <= cy + cardH;
            boolean active = module == selected;
            float hoverValue = hoverAnimations.compute(module.getName(), (key, old) ->
                    old == null ? (hover ? 1f : 0f) : old + ((hover ? 1f : 0f) - old) * 0.22f);
            int cardColor = mix(CARD, CARD_HOVER, hoverValue);
            if (active) cardColor = mix(cardColor, 0xFF34432E, 0.65f);
            roundRect(g, cx, cy, colW, cardH, 7, cardColor);
            border(g, cx, cy, colW, cardH, active ? LIME : BORDER);
            if (active) fill(g, cx + 1, cy + 5, 2, cardH - 10, LIME);
            text(g, module.getName(), cx + 9, cy + 10, WHITE);
            text(g, module.isEnabled() ? "ACTIVE" : "INACTIVE",
                    cx + 9, cy + 31, module.isEnabled() ? LIME : MUTED);
            drawSwitch(g, cx + colW - 29, cy + 17, module.isEnabled(), "card." + module.getName());
        }
        if (visible.isEmpty()) {
            text(g, "No modules found", x + 12, cardY + 15, MUTED);
        }
        if (visible.size() > rows * 2) {
            text(g, "Scroll to see more modules", x + 8, bodyBottom() - 10, DIM);
        }
    }

    private void drawModuleSettings(GuiGraphics g, int x, int y, int w, int h) {
        fill(g, x, y, w, h, PANEL);
        border(g, x, y, w, h, BORDER);
        text(g, "MODULE SETTINGS", x + 12, y + 15, MUTED);

        if (selected == null) {
            text(g, "Select a module", x + 12, y + 44, WHITE);
            text(g, "Its settings will appear here.", x + 12, y + 62, MUTED);
            return;
        }

        text(g, selected.getName(), x + 12, y + 39, WHITE);
        text(g, selected.getCategory(), x + 12, y + 57, MUTED);
        String description = selected.getDescription();
        int descY = y + 78;
        for (String line : wrap(description, w - 24)) {
            text(g, line, x + 12, descY, WHITE);
            descY += 12;
        }

        int lineY = descY + 8;
        fill(g, x + 10, lineY, w - 20, 1, BORDER);
        int yy = lineY + 13;
        if (selected.getName().equals("BlockESP")) {
            text(g, "BLOCKS TO HIGHLIGHT", x + 12, yy, MUTED);
            yy += 18;
            for (String name : BlockSelection.names()) {
                if (yy > y + h - 42) break;
                boolean checked = BlockSelection.isSelected(name);
                text(g, checked ? "[x]" : "[ ]", x + 12, yy, checked ? LIME : DIM);
                text(g, name, x + 38, yy, WHITE);
                yy += 17;
            }
            text(g, "Select any combination of blocks", x + 12, y + h - 36, MUTED);
        }
        if (selected.getName().equals("Storage ESP")) {
            text(g, "STORAGE TYPES", x + 12, yy, MUTED);
            yy += 18;
            for (String name : new String[]{"Chest", "Trapped Chest", "Ender Chest", "Barrel", "All Shulker Boxes"}) {
                text(g, "✓", x + 12, yy, 0xFF65D8F2);
                text(g, name, x + 30, yy, WHITE);
                yy += 19;
            }
            text(g, "Cyan outlines mark storage blocks", x + 12, y + h - 36, MUTED);
        }
        if (selected.getName().equals("Fullbright")) {
            text(g, "Brightness", x + 12, yy, WHITE);
            text(g, "100%", x + w - 40, yy, MUTED);
            fill(g, x + 12, yy + 22, w - 24, 3, 0xFF565B61);
            fill(g, x + 12, yy + 22, w - 38, 3, LIME);
            fill(g, x + w - 29, yy + 17, 8, 13, WHITE);
            yy += 48;
        }

        for (String option : settingsFor(selected)) {
            boolean value = ModuleConfig.getBoolean(selected.getName(), settingKey(option), true);
            text(g, option, x + 12, yy + 3, WHITE);
            drawSwitch(g, x + w - 34, yy + 1, value,
                    "setting." + selected.getName() + "." + settingKey(option));
            yy += 31;
        }

        if (selected.getName().equals("Coordinates HUD") || selected.getName().equals("FPS Counter")) {
            text(g, "HUD preview", x + 12, yy + 5, MUTED);
            fill(g, x + 12, yy + 20, w - 24, 37, 0xFF111419);
            text(g, selected.getName().equals("Coordinates HUD") ? "XYZ  120  64  -32" : "FPS  144",
                    x + 20, yy + 34, LIME);
            yy += 69;
        }

        int configY = Math.min(y + h - 83, yy + 8);
        fill(g, x + 10, configY, w - 20, 1, BORDER);
        text(g, "CONFIGURATION", x + 12, configY + 13, MUTED);
        text(g, "Keybind", x + 12, configY + 34, WHITE);
        roundRect(g, x + 12, configY + 48, w - 24, 23, 6, 0xFF30394C);
        text(g, "None", x + 20, configY + 55, MUTED);
        text(g, "Favorite", x + 12, y + h - 20, WHITE);
        drawSwitch(g, x + w - 34, y + h - 23, selected.isFavorite(), "settings.favorite");
    }

    private List<String> wrap(String value, int maxWidth) {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : value.split(" ")) {
            String next = line.isEmpty() ? word : line + " " + word;
            if (font.width(next) > maxWidth && !line.isEmpty()) {
                lines.add(line.toString());
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(next);
            }
        }
        if (!line.isEmpty()) lines.add(line.toString());
        return lines;
    }

    private void drawHudPage(GuiGraphics g, int x, int y, int w, int h) {
        fill(g, x, y, w, h, PANEL);
        border(g, x, y, w, h, BORDER);
        text(g, "HUD ELEMENTS", x + 14, y + 15, LIME);
        text(g, "Choose which information appears in-game.", x + 14, y + 34, MUTED);
        drawHudToggle(g, x + 14, y + 64, "Coordinates HUD", "Shows your XYZ position.");
        drawHudToggle(g, x + 14, y + 112, "FPS Counter", "Shows your current frames per second.");
    }

    private void drawHudToggle(GuiGraphics g, int x, int y, String name, String desc) {
        fill(g, x, y, mainWidth() - 28, 39, CARD);
        text(g, name, x + 10, y + 7, WHITE);
        text(g, desc, x + 10, y + 22, MUTED);
        Module module = findModule(name);
        if (module != null) drawSwitch(g, x + mainWidth() - 53, y + 13,
                module.isEnabled(), "hud." + name);
    }

    private Module findModule(String name) {
        return modules.stream().filter(m -> m.getName().equals(name)).findFirst().orElse(null);
    }

    private void drawAppearancePage(GuiGraphics g, int x, int y, int w, int h) {
        fill(g, x, y, w, h, PANEL);
        border(g, x, y, w, h, BORDER);
        text(g, "APPEARANCE", x + 14, y + 15, LIME);
        text(g, "Fizzy Client interface theme", x + 14, y + 34, MUTED);
        text(g, "Accent colour", x + 14, y + 72, WHITE);
        int[] colors = {LIME, PINK, 0xFF9BD8E5, 0xFFB7A4F5};
        for (int i = 0; i < colors.length; i++) {
            fill(g, x + 14 + i * 31, y + 92, 22, 22, colors[i]);
            border(g, x + 14 + i * 31, y + 92, 22, 22, BORDER);
        }
        text(g, "Compact module cards", x + 14, y + 145, WHITE);
        text(g, "The layout follows the reference workspace.", x + 14, y + 162, MUTED);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        open = Math.min(1f, open + delta * 4f);
        int x = left();
        int y = top() + Math.round((1f - open) * 10f);
        int w = panelWidth();
        int h = panelHeight();

        // Keep the Minecraft world visible; only draw a subtle shadow behind the compact window.
        fill(g, x - 3, y - 3, w + 6, h + 6, 0x66000000);
        fill(g, x, y, w, h, OUTER);
        border(g, x, y, w, h, 0xFF555B65);
        drawHeader(g, x, y, w);

        int contentY = y + 48;
        int contentH = h - 72;
        drawSidebar(g, x, contentY, contentH, mouseX, mouseY);

        int mx = mainLeft();
        int my = bodyTop();
        int mw = mainWidth();
        if (page.equals("Modules")) {
            drawWorkspace(g, mx, my, mw, mouseX, mouseY);
            drawModuleSettings(g, settingsLeft(), my, settingsWidth(), bodyBottom() - my);
        } else if (page.equals("HUD")) {
            drawHudPage(g, mx, my, mw, bodyBottom() - my);
            drawModuleSettings(g, settingsLeft(), my, settingsWidth(), bodyBottom() - my);
        } else {
            drawAppearancePage(g, mx, my, mw, bodyBottom() - my);
            drawModuleSettings(g, settingsLeft(), my, settingsWidth(), bodyBottom() - my);
        }

        fill(g, x, y + h - 23, w, 23, PANEL_DARK);
        fill(g, x, y + h - 24, w, 1, BORDER);
        text(g, "FIZZY  /  YOUR WORKSPACE", x + 14, y + h - 15, MUTED);
        long enabled = modules.stream().filter(Module::isEnabled).count();
        String count = enabled + " modules active";
        text(g, count, x + w - font.width(count) - 14, y + h - 15, MUTED);
        super.render(g, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) return super.mouseClicked(event, doubleClick);
        double mx = event.x();
        double my = event.y();
        int x = left();
        int y = top() + Math.round((1f - open) * 10f);
        int w = panelWidth();
        int h = panelHeight();

        // Drag the window using its title bar, away from the tab buttons.
        if (mx >= x + 150 && mx <= x + w - 150 && my >= y + 2 && my <= y + 46) {
            dragging = true;
            dragOffsetX = mx - x;
            dragOffsetY = my - y;
            return true;
        }

        String[] tabs = {"Modules", "HUD", "Appearance"};
        int tabRight = x + w - 18;
        for (int i = tabs.length - 1; i >= 0; i--) {
            int tw = font.width(tabs[i]) + 22;
            int tx = tabRight - tw;
            if (mx >= tx && mx <= tx + tw && my >= y + 10 && my <= y + 38) {
                page = tabs[i];
                searchFocused = false;
                return true;
            }
            tabRight = tx - 8;
        }

        int sideY = y + 48;
        int yy = sideY + 29;
        for (String item : CATEGORIES) {
            if (mx >= x + 7 && mx <= x + sideWidth() - 8 && my >= yy && my <= yy + 25) {
                category = item;
                selected = null;
                page = "Modules";
                return true;
            }
            yy += 29;
        }
        if (mx >= x + 7 && mx <= x + sideWidth() - 8 && my >= sideY + bodyBottom() - top() - 24) {
            enabledOnly = !enabledOnly;
            return true;
        }

        int mx0 = mainLeft();
        int my0 = bodyTop();
        int mw = mainWidth();
        if (page.equals("Modules")) {
            int searchY = my0 + 80;
            if (mx >= mx0 && mx <= mx0 + mw && my >= searchY && my <= searchY + 27) {
                searchFocused = true;
                return true;
            }
            searchFocused = false;

            List<Module> visible = visibleModules();
            int sectionY = searchY + 35;
            int cardY = sectionY + 26;
            int colW = (mw - 8) / 2;
            int cardH = 49;
            int rows = Math.max(1, (bodyBottom() - cardY - 4) / 57);
            for (int i = 0; i < visible.size() && i < rows * 2; i++) {
                int col = i % 2;
                int row = i / 2;
                int cx = mx0 + col * (colW + 8);
                int cy = cardY + row * 57;
                Module module = visible.get(i);
                if (mx >= cx && mx <= cx + colW && my >= cy && my <= cy + cardH) {
                    selected = module;
                    if (mx >= cx + colW - 34) module.toggle();
                    return true;
                }
            }
        } else {
            searchFocused = false;
            if (page.equals("HUD")) {
                if (my >= my0 + 64 && my <= my0 + 103) {
                    Module module = findModule("Coordinates HUD");
                    if (module != null) module.toggle();
                    return true;
                }
                if (my >= my0 + 112 && my <= my0 + 151) {
                    Module module = findModule("FPS Counter");
                    if (module != null) module.toggle();
                    return true;
                }
            }
        }

        if (page.equals("Modules") && selected != null) {
            int sx = settingsLeft();
            int sy = my0;
            int sw = settingsWidth();
            int sh = bodyBottom() - my0;
            if (mx >= sx && mx <= sx + sw && my >= sy + sh - 31 && my <= sy + sh) {
                selected.toggleFavorite();
                return true;
            }
            if (selected.getName().equals("BlockESP")) {
                int desc = sy + 78 + wrap(selected.getDescription(), sw - 24).size() * 12;
                int optionY = desc + 8 + 13 + 18;
                for (String name : BlockSelection.names()) {
                    if (my >= optionY - 5 && my <= optionY + 10 && mx >= sx + 8 && mx <= sx + sw - 8) {
                        BlockSelection.setSelected(name, !BlockSelection.isSelected(name));
                        return true;
                    }
                    optionY += 17;
                }
            }
            String[] settings = settingsFor(selected);
            int descY = sy + 78 + wrap(selected.getDescription(), sw - 24).size() * 12;
            int lineY = descY + 8;
            int yySettings = lineY + 13;
            if (selected.getName().equals("BlockESP")) yySettings += 42;
            if (selected.getName().equals("Fullbright")) yySettings += 48;
            for (String option : settings) {
                if (mx >= sx + sw - 45 && mx <= sx + sw - 8
                        && my >= yySettings - 2 && my <= yySettings + 20) {
                    String key = settingKey(option);
                    boolean value = ModuleConfig.getBoolean(selected.getName(), key, true);
                    ModuleConfig.setBoolean(selected.getName(), key, !value);
                    return true;
                }
                yySettings += 31;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(net.minecraft.client.input.MouseButtonEvent event, double dragX, double dragY) {
        if (dragging) {
            windowX = (int) (event.x() - dragOffsetX);
            windowY = (int) (event.y() - dragOffsetY);
            windowX = Math.max(4, Math.min(width - panelWidth() - 4, windowX));
            windowY = Math.max(4, Math.min(height - panelHeight() - 4, windowY));
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(net.minecraft.client.input.MouseButtonEvent event) {
        dragging = false;
        return super.mouseReleased(event);
    }

    @Override
    public boolean charTyped(net.minecraft.client.input.CharacterEvent event) {
        if (!searchFocused) return super.charTyped(event);
        if (event.codepoint() == 8) {
            if (!search.isEmpty()) search = search.substring(0, search.length() - 1);
            return true;
        }
        if (!Character.isISOControl(event.codepoint()) && search.length() < 32) {
            search += Character.toString(event.codepoint());
        }
        return true;
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        if (searchFocused) {
            if (event.key() == 256) {
                searchFocused = false;
                return true;
            }
            if (event.key() == 259) {
                if (!search.isEmpty()) search = search.substring(0, search.length() - 1);
                return true;
            }
        }
        return super.keyPressed(event);
    }
}
