package dev.donutclient;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
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
    private int moduleScroll;
    private float open = 0f;
    private float selectionTransition = 0f;
    private String lastSelectedName = "";
    private boolean dragging;
    private double dragOffsetX;
    private double dragOffsetY;
    private int windowX = Integer.MIN_VALUE;
    private int windowY = Integer.MIN_VALUE;

    private static final Identifier CAT_ICON = Identifier.fromNamespaceAndPath("fizzy", "icon.png");

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
        modules.add(new Module("Sus Chunk Finder", "Visual", "Shows nearby loaded machine blocks as clues, not proof of a base."));
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

    private int panelWidth() { return Math.min(900, width - 32); }
    private int panelHeight() { return Math.min(570, height - 32); }
    private int left() {
        if (windowX == Integer.MIN_VALUE) windowX = (width - panelWidth()) / 2;
        return Math.max(4, Math.min(width - panelWidth() - 4, windowX));
    }
    private int top() {
        if (windowY == Integer.MIN_VALUE) windowY = (height - panelHeight()) / 2;
        return Math.max(4, Math.min(height - panelHeight() - 4, windowY));
    }
    private int sideWidth() { return 142; }
    private int settingsWidth() { return Math.max(224, Math.min(270, panelWidth() / 3)); }
    private int mainLeft() { return left() + sideWidth() + 12; }
    private int settingsLeft() { return left() + panelWidth() - settingsWidth() - 12; }
    private int mainWidth() { return settingsLeft() - mainLeft() - 10; }
    private int bodyTop() { return top() + 68; }
    private int bodyBottom() { return top() + panelHeight() - 24; }

    private void fill(GuiGraphics g, int x, int y, int w, int h, int color) {
        if (w > 0 && h > 0) g.fill(x, y, x + w, y + h, color);
    }

    // Native Minecraft GUI fills: no custom texture, atlas, UVs, or pixel-built corners.
    private void roundRect(GuiGraphics g, int x, int y, int w, int h, int radius, int color) {
        fill(g, x, y, w, h, color);
    }

    private void drawCatIcon(GuiGraphics g, int x, int y, int size) {
        g.blit(RenderPipelines.GUI_TEXTURED, CAT_ICON, x, y, 0, 0, size, size, 64, 64);
    }

    private void largeText(GuiGraphics g, String value, int x, int y, int color, float scale) {
        g.pose().pushMatrix();
        g.pose().translate((float) x, (float) y);
        g.pose().scale(scale, scale);
        g.drawString(font, Component.literal(value), 0, 0, color, false);
        g.pose().popMatrix();
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

    private void donutMark(GuiGraphics g, int x, int y) { drawCatIcon(g, x, y, 36); }

    private void drawSwitch(GuiGraphics g, int x, int y, boolean active, String id) {
        float target = active ? 1f : 0f;
        float value = switchAnimations.compute(id, (key, old) ->
                old == null ? target : old + (target - old) * 0.34f);
        int track = mix(0xFF51483F, 0xFF9CCB67, value);
        roundRect(g, x, y, 21, 12, 6, track);
        int knobX = x + 2 + Math.round(9 * value);
        roundRect(g, knobX, y + 2, 8, 8, 4, 0xFFF7F7F3);
    }

    private boolean isImplemented(Module module) {
        return switch (module.getName()) {
            case "Fullbright", "Toggle Sprint", "BlockESP", "Storage ESP",
                    "Coordinates HUD", "FPS Counter", "Direction HUD", "Armor HUD",
                    "Progress Bar", "Light Debug", "Sus Chunk Finder" -> true;
            default -> false;
        };
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
            case "Progress Bar" -> new String[]{"Show level", "Show percentage"};
            case "Light Debug" -> new String[]{"Show block light", "Show sky light"};
            case "Sus Chunk Finder" -> new String[]{"Wide scan", "Show distance", "Show icons"};
            default -> new String[0];
        };
    }

    private String settingKey(String value) {
        return value.toLowerCase().replaceAll("[^a-z0-9]+", "_");
    }

    private void drawHeader(GuiGraphics g, int x, int y, int w) {
        fill(g, x, y, w, 58, 0xFF171D27);
        donutMark(g, x + 16, y + 10);
        text(g, "FIZZY CLIENT", x + 62, y + 17, WHITE);
        text(g, "MINECRAFT CLIENT  •  FABRIC", x + 62, y + 34, MUTED);

        String[] tabs = {"Modules", "HUD", "Appearance"};
        int tabRight = x + w - 18;
        for (int i = tabs.length - 1; i >= 0; i--) {
            String tab = tabs[i];
            int tw = font.width(tab) + 28;
            int tx = tabRight - tw;
            boolean active = page.equals(tab);
            if (active) {
                fill(g, tx, y + 14, tw, 30, 0xFF29372F);
                fill(g, tx + 10, y + 43, tw - 20, 2, LIME);
            }
            centered(g, tab, tx + tw / 2, y + 25, active ? LIME : MUTED);
            tabRight = tx - 8;
        }
        fill(g, x, y + 57, w, 1, BORDER);
    }

        private void drawSidebar(GuiGraphics g, int x, int y, int h, int mouseX, int mouseY) {
        fill(g, x, y, sideWidth(), h, 0xFF151B24);
        fill(g, x + sideWidth() - 1, y, 1, h, 0xFF293342);
        text(g, "LIBRARY", x + 16, y + 18, DIM);

        int yy = y + 38;
        for (String item : CATEGORIES) {
            boolean active = category.equals(item);
            boolean hover = mouseX >= x + 9 && mouseX <= x + sideWidth() - 10
                    && mouseY >= yy && mouseY <= yy + 32;
            if (active || hover) fill(g, x + 9, yy, sideWidth() - 18, 32,
                    active ? 0xFF29372F : 0xFF202A36);
            if (active) fill(g, x + 9, yy + 6, 3, 20, LIME);
            String symbol = switch (item) {
                case "All modules" -> "▦";
                case "Visual" -> "◈";
                case "Blatant" -> "⌁";
                case "Streaming" -> "▣";
                default -> "☆";
            };
            text(g, symbol, x + 19, yy + 11, active ? LIME : DIM);
            text(g, item, x + 39, yy + 11, active ? WHITE : MUTED);
            yy += 38;
        }

        int bottomY = y + h - 48;
        fill(g, x + 13, bottomY, sideWidth() - 26, 1, 0xFF293342);
        text(g, enabledOnly ? "●" : "○", x + 17, bottomY + 17, enabledOnly ? LIME : DIM);
        text(g, "Enabled only", x + 34, bottomY + 17, enabledOnly ? WHITE : MUTED);
        text(g, "RIGHT SHIFT  •  MENU", x + 13, y + h - 12, DIM);
    }

        private void drawWorkspace(GuiGraphics g, int x, int y, int w, int mouseX, int mouseY) {
        text(g, "MODULE LIBRARY", x + 2, y + 2, LIME);
        String heading = category.equals("Favorites") ? "Your favorites"
                : category.equals("All modules") ? "All modules" : category;
        text(g, heading, x + 2, y + 21, WHITE);
        text(g, "Find a feature and configure it to your play style.", x + 2, y + 40, MUTED);

        int searchY = y + 61;
        fill(g, x, searchY, w, 32, 0xFF1B2430);
        border(g, x, searchY, w, 32, searchFocused ? LIME : 0xFF344153);
        text(g, "⌕", x + 11, searchY + 10, MUTED);
        text(g, search.isEmpty() ? "Search modules..." : search,
                x + 31, searchY + 11, search.isEmpty() ? DIM : WHITE);
        if (searchFocused) fill(g, x + 31 + font.width(search), searchY + 8, 1, 15, LIME);

        List<Module> visible = visibleModules();
        int listY = searchY + 43;
        int rowH = 55;
        int gap = 7;
        int visibleRows = Math.max(1, (bodyBottom() - listY - 2) / (rowH + gap));
        if (moduleScroll > Math.max(0, visible.size() - visibleRows))
            moduleScroll = Math.max(0, visible.size() - visibleRows);

        for (int row = 0; row < visibleRows; row++) {
            int index = moduleScroll + row;
            if (index >= visible.size()) break;
            Module module = visible.get(index);
            int cy = listY + row * (rowH + gap);
            boolean hover = mouseX >= x && mouseX <= x + w && mouseY >= cy && mouseY <= cy + rowH;
            boolean active = module == selected;
            float hoverValue = hoverAnimations.compute(module.getName(), (key, old) ->
                    old == null ? (hover ? 1f : 0f) : old + ((hover ? 1f : 0f) - old) * 0.24f);
            int card = mix(0xFF1B2430, 0xFF273342, hoverValue);
            if (active) card = mix(card, 0xFF293A31, 0.72f);
            fill(g, x, cy, w, rowH, card);
            border(g, x, cy, w, rowH, active ? 0xFF78B95B : 0xFF303D4D);
            fill(g, x + 10, cy + 10, 34, 34, active ? 0xFF344A39 : 0xFF293543);
            centered(g, module.getName().substring(0, 1).toUpperCase(), x + 27, cy + 22, active ? LIME : 0xFFB5C0CC);
            text(g, module.getName(), x + 54, cy + 10, WHITE);
            String desc = module.getDescription();
            if (desc.length() > 47) desc = desc.substring(0, 44) + "...";
            text(g, desc, x + 54, cy + 28, MUTED);
            boolean implemented = isImplemented(module);
            String state = !implemented ? "SOON" : module.isEnabled() ? "ENABLED" : "DISABLED";
            int stateColor = !implemented ? DIM : module.isEnabled() ? LIME : MUTED;
            text(g, state, x + w - 83, cy + 10, stateColor);
            drawSwitch(g, x + w - 42, cy + 31, implemented && module.isEnabled(), "card." + module.getName());
        }
        if (visible.isEmpty()) text(g, "No modules match your search.", x + 4, listY + 14, MUTED);
        if (visible.size() > visibleRows) {
            String count = (moduleScroll + 1) + "–" + Math.min(moduleScroll + visibleRows, visible.size())
                    + " of " + visible.size();
            text(g, count + "  •  scroll to browse", x + 2, bodyBottom() - 10, DIM);
        }
    }

        private void drawModuleSettings(GuiGraphics g, int x, int y, int w, int h) {
        fill(g, x, y, w, h, 0xFF171F2A);
        border(g, x, y, w, h, 0xFF344153);
        fill(g, x, y, w, 3, LIME);
        text(g, "DETAILS", x + 15, y + 18, DIM);

        if (selected == null) {
            text(g, "Choose a module", x + 15, y + 53, WHITE);
            for (String line : wrap("Select any module from the library to view its description and settings.", w - 30))
                text(g, line, x + 15, y + 74 + wrap("Select any module from the library to view its description and settings.", w - 30).indexOf(line) * 14, MUTED);
            return;
        }

        text(g, selected.getName(), x + 15, y + 48, WHITE);
        text(g, isImplemented(selected) ? selected.getCategory().toUpperCase() : "COMING SOON",
                x + 15, y + 66, isImplemented(selected) ? LIME : 0xFFFFC27A);
        int descY = y + 91;
        List<String> descriptionLines = wrap(selected.getDescription(), w - 30);
        for (int i = 0; i < descriptionLines.size(); i++)
            text(g, descriptionLines.get(i), x + 15, descY + i * 14, MUTED);

        int yy = descY + descriptionLines.size() * 14 + 19;
        fill(g, x + 15, yy, w - 30, 1, 0xFF344153);
        yy += 17;

        if (selected.getName().equals("BlockESP")) {
            text(g, "BLOCKS TO HIGHLIGHT", x + 15, yy, DIM);
            yy += 22;
            for (String name : BlockSelection.names()) {
                if (yy > y + h - 42) break;
                boolean checked = BlockSelection.isSelected(name);
                fill(g, x + 15, yy - 3, 13, 13, checked ? 0xFF527A43 : 0xFF273342);
                if (checked) text(g, "✓", x + 17, yy - 4, WHITE);
                text(g, name, x + 37, yy, WHITE);
                yy += 20;
            }
        } else if (selected.getName().equals("Storage ESP")) {
            text(g, "STORAGE TYPES", x + 15, yy, DIM);
            yy += 23;
            for (String name : new String[]{"Chest", "Trapped Chest", "Ender Chest", "Barrel", "All Shulker Boxes"}) {
                text(g, "✓", x + 16, yy, LIME);
                text(g, name, x + 37, yy, WHITE);
                yy += 21;
            }
        } else {
            for (String option : settingsFor(selected)) {
                boolean value = ModuleConfig.getBoolean(selected.getName(), settingKey(option), true);
                text(g, option, x + 15, yy + 4, WHITE);
                drawSwitch(g, x + w - 43, yy + 2, value,
                        "setting." + selected.getName() + "." + settingKey(option));
                yy += 35;
            }
        }

        if (selected.getName().equals("Coordinates HUD") || selected.getName().equals("FPS Counter")) {
            yy += 8;
            text(g, "LIVE PREVIEW", x + 15, yy, DIM);
            fill(g, x + 15, yy + 17, w - 30, 35, 0xFF101720);
            text(g, selected.getName().equals("Coordinates HUD") ? "XYZ  120  64  -32" : "FPS  144",
                    x + 25, yy + 29, LIME);
        }
        if (!selected.getName().equals("BlockESP") && !selected.getName().equals("Storage ESP")) {
            int favoriteY = y + h - 48;
            fill(g, x + 15, favoriteY - 12, w - 30, 1, 0xFF344153);
            text(g, "Add to favorites", x + 15, favoriteY + 3, WHITE);
            drawSwitch(g, x + w - 43, favoriteY, selected.isFavorite(), "settings.favorite");
        }
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
        text(g, "HUD ELEMENTS", x + 2, y + 2, LIME);
        text(g, "Choose the information displayed while playing.", x + 2, y + 22, MUTED);
        drawHudToggle(g, x, y + 57, "Coordinates HUD", "Shows your XYZ position.");
        drawHudToggle(g, x, y + 105, "FPS Counter", "Shows your current frames per second.");
        drawHudToggle(g, x, y + 153, "Direction HUD", "Shows the direction you are facing.");
        drawHudToggle(g, x, y + 201, "Armor HUD", "Shows equipped armor durability.");
        drawHudToggle(g, x, y + 249, "Light Debug", "Shows block and sky light levels.");
        drawHudToggle(g, x, y + 297, "Progress Bar", "Shows your current XP progress.");
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
        text(g, "APPEARANCE", x + 2, y + 2, LIME);
        text(g, "A clean, readable workspace for Fizzy Client.", x + 2, y + 22, MUTED);
        fill(g, x, y + 57, w, 86, 0xFF1B2430);
        border(g, x, y + 57, w, 86, 0xFF344153);
        text(g, "ACCENT COLOUR", x + 14, y + 72, DIM);
        int[] colors = {LIME, PINK, 0xFF75CDE0, 0xFFB7A4F5};
        for (int i = 0; i < colors.length; i++) {
            fill(g, x + 14 + i * 34, y + 98, 24, 24, colors[i]);
            border(g, x + 14 + i * 34, y + 98, 24, 24, 0xFF566477);
        }
        fill(g, x, y + 157, w, 74, 0xFF1B2430);
        border(g, x, y + 157, w, 74, 0xFF344153);
        text(g, "TYPOGRAPHY", x + 14, y + 172, DIM);
        text(g, "Minecraft default font", x + 14, y + 196, WHITE);
        text(g, "Native game font for a consistent feel.", x + 14, y + 213, MUTED);
    }

        @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        float frame = Math.max(0.01f, Math.min(1f, delta * 60f));
        open += (1f - open) * (1f - (float) Math.pow(0.72f, frame));
        if (open > 0.999f) open = 1f;
        String selectedName = selected == null ? "" : selected.getName();
        if (!selectedName.equals(lastSelectedName)) {
            lastSelectedName = selectedName;
            selectionTransition = 0f;
        }
        selectionTransition += (1f - selectionTransition) * (1f - (float) Math.pow(0.65f, frame));
        int x = left();
        int y = top() + Math.round((1f - open) * 18f);
        int w = panelWidth();
        int h = panelHeight();

        // Keep the Minecraft world visible; only draw a subtle shadow behind the compact window.
        fill(g, x - 3, y - 3, w + 6, h + 6, 0x66000000);
        fill(g, x, y, w, h, OUTER);
        border(g, x, y, w, h, 0xFF555B65);
        drawHeader(g, x, y, w);

        int contentY = y + 58;
        int contentH = h - 82;
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
        text(g, "FIZZY CLIENT  /  YOUR WORKSPACE", x + 14, y + h - 15, MUTED);
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
        int y = top() + Math.round((1f - open) * 18f);
        int w = panelWidth();
        int h = panelHeight();

        // Drag the window using its title bar, away from the tab buttons.
        if (mx >= x + 150 && mx <= x + w - 300 && my >= y + 2 && my <= y + 55) {
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
                moduleScroll = 0;
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
            int searchY = my0 + 61;
            if (mx >= mx0 && mx <= mx0 + mw && my >= searchY && my <= searchY + 32) {
                searchFocused = true;
                return true;
            }
            searchFocused = false;

            List<Module> visible = visibleModules();
            int listY = searchY + 43;
            int rowH = 55;
            int gap = 7;
            int rows = Math.max(1, (bodyBottom() - listY - 2) / (rowH + gap));
            for (int row = 0; row < rows; row++) {
                int index = moduleScroll + row;
                if (index >= visible.size()) break;
                int cy = listY + row * (rowH + gap);
                Module module = visible.get(index);
                if (mx >= mx0 && mx <= mx0 + mw && my >= cy && my <= cy + rowH) {
                    selected = module;
                    if (isImplemented(module) && mx >= mx0 + mw - 49) module.toggle();
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
            if (!selected.getName().equals("BlockESP") && !selected.getName().equals("Storage ESP")
                    && mx >= sx && mx <= sx + sw && my >= sy + sh - 31 && my <= sy + sh) {
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
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int mx = mainLeft();
        int my = bodyTop();
        if (page.equals("Modules") && mouseX >= mx && mouseX <= mx + mainWidth()
                && mouseY >= my + 100 && mouseY <= bodyBottom()) {
            int count = visibleModules().size();
            int rows = Math.max(1, (bodyBottom() - (my + 104) - 2) / 62);
            moduleScroll = Math.max(0, Math.min(Math.max(0, count - rows),
                    moduleScroll - (int) Math.signum(verticalAmount)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
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
