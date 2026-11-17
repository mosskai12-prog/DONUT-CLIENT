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
    private final Map<String, Integer> columnScroll = new HashMap<>();
    private int accentColor = 0xFFCE83F5;
    private int themeIndex = 0;
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

    private static final String[] CATEGORIES = {"Combat", "Movement", "Render", "Player", "Misc"};

    public DonutScreen() {
        super(Component.literal("Fizzy Client"));
        modules.add(new Module("Base Finder", "Misc", "Finds configured base markers."));
        modules.add(new Module("Sus Chunk Finder", "Render", "Shows nearby loaded machine blocks as clues, not proof of a base."));
        modules.add(new Module("Fullbright", "Render", "Makes dark areas easier to see."));
        modules.add(new Module("Fly", "Movement", "Flight controls. Requires a supported game mode."));
        modules.add(new Module("Progress Bar", "Misc", "Displays progress information."));
        modules.add(new Module("Name Hider", "Player", "Hides player names in supported HUD contexts."));
        modules.add(new Module("Hotbar", "Player", "Displays a configurable hotbar overlay."));
        modules.add(new Module("Amethyst Scanner", "Render", "Displays amethyst-related information."));
        modules.add(new Module("Light Debug", "Render", "Displays lighting debug information."));
        modules.add(new Module("Sound Detector", "Misc", "Displays sound event indicators."));
        modules.add(new Module("BlockESP", "Render", "Highlights the block types you select."));
        modules.add(new Module("Storage ESP", "Render", "Highlights chests, barrels, ender chests and shulker boxes."));
        modules.add(new Module("Coordinates HUD", "Player", "Displays your current XYZ coordinates."));
        modules.add(new Module("FPS Counter", "Player", "Displays your current frame rate."));
        modules.add(new Module("Direction HUD", "Player", "Shows which compass direction you are facing."));
        modules.add(new Module("Armor HUD", "Player", "Shows the remaining durability of your equipped armor."));
        modules.add(new Module("Toggle Sprint", "Movement", "Automatically sprints while moving."));
        modules.add(new Module("Freecam", "Movement", "Detaches the camera from your player for remote inspection."));
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
        open += (1f - open) * (1f - (float) Math.pow(0.76f, frame));
        if (open > 0.999f) open = 1f;

        int colGap = 10;
        int colWidth = Math.min(166, (width - 70 - colGap * 4) / 5);
        int totalWidth = colWidth * 5 + colGap * 4;
        int startX = (width - totalWidth) / 2;
        int colY = Math.max(28, (height - 340) / 2);
        int colHeight = Math.min(350, height - colY - 24);
        int slide = Math.round((1f - open) * 16f);
        int[] accents = {0xFFE78BC8, 0xFFC68AF3, accentColor, 0xFFCE9BFF, 0xFFB68CEB};

        for (int c = 0; c < CATEGORIES.length; c++) {
            String cat = CATEGORIES[c];
            int x = startX + c * (colWidth + colGap);
            int y = colY + slide;
            fill(g, x - 2, y - 2, colWidth + 4, colHeight + 4, 0x33000000);
            fill(g, x, y, colWidth, colHeight, 0xE9121019);
            border(g, x, y, colWidth, colHeight, 0x665E5268);
            fill(g, x, y, colWidth, 27, 0xF21A1522);
            String icon = switch (cat) {
                case "Combat" -> "⚔";
                case "Movement" -> "✥";
                case "Render" -> "◈";
                case "Player" -> "♟";
                default -> "⚙";
            };
            centered(g, icon + " " + cat, x + colWidth / 2, y + 9, accents[c]);

            List<Module> entries = modules.stream().filter(m -> m.getCategory().equals(cat)).toList();
            int rowY = y + 34;
            int scroll = columnScroll.getOrDefault(cat, 0);
            int rowHeight = 27;
            int available = colHeight - 44;
            int rows = Math.max(1, available / rowHeight);
            if (scroll > Math.max(0, entries.size() - rows)) {
                scroll = Math.max(0, entries.size() - rows);
                columnScroll.put(cat, scroll);
            }
            for (int n = 0; n < rows; n++) {
                int index = scroll + n;
                if (index >= entries.size()) break;
                Module module = entries.get(index);
                int ry = rowY + n * rowHeight;
                boolean hover = mouseX >= x + 3 && mouseX <= x + colWidth - 3
                        && mouseY >= ry && mouseY <= ry + rowHeight - 2;
                boolean selectedRow = selected == module;
                float hv = hoverAnimations.compute(module.getName(), (k, old) ->
                        old == null ? (hover ? 1f : 0f) : old + ((hover ? 1f : 0f) - old) * 0.28f);
                if (hover || selectedRow) fill(g, x + 3, ry, colWidth - 6, rowHeight - 2,
                        mix(0x001F1928, 0xAA493452, Math.max(hv, selectedRow ? 0.8f : 0f)));
                String label = module.getName();
                int textColor = module.isEnabled() ? accents[c] : (hover ? WHITE : 0xFFB7AFBE);
                if (font.width(label) > colWidth - 35) {
                    while (label.length() > 3 && font.width(label + "...") > colWidth - 35)
                        label = label.substring(0, label.length() - 1);
                    label += "...";
                }
                text(g, label, x + 10, ry + 8, textColor);
                int dotColor = !isImplemented(module) ? 0xFF665F6C
                        : module.isEnabled() ? accents[c] : 0xFF514958;
                fill(g, x + colWidth - 15, ry + 10, 5, 5, dotColor);
                if (selectedRow) {
                    fill(g, x + 5, ry + rowHeight - 3, colWidth - 10, 1, accents[c]);
                    String detail = module.getDescription();
                    if (detail.length() > 45) detail = detail.substring(0, 42) + "...";
                    text(g, detail, x + 9, ry + 17, 0xFFAAA0B0);
                }
            }
            if (entries.isEmpty()) centered(g, "No modules yet", x + colWidth / 2, rowY + 10, DIM);
            if (entries.size() > rows) {
                String hint = "SCROLL  " + (scroll + 1) + "/" + entries.size();
                centered(g, hint, x + colWidth / 2, y + colHeight - 15, 0xFF887C91);
            }
        }

        // Small theme picker, positioned like the reference client.
        int themeW = 142, themeH = 82;
        int tx = width - themeW - 8, ty = height - themeH - 8;
        fill(g, tx - 2, ty - 2, themeW + 4, themeH + 4, 0x33000000);
        fill(g, tx, ty, themeW, themeH, 0xE914111B);
        border(g, tx, ty, themeW, themeH, 0x775E5268);
        text(g, "•  THEMES", tx + 9, ty + 8, WHITE);
        int[] swatches = {0xFFE78BC8, 0xFFC68AF3, 0xFF8F9BFF, 0xFF75D6C4};
        String[] names = {"Pink", "Purple", "Blue", "Mint"};
        for (int i = 0; i < swatches.length; i++) {
            int sy = ty + 25 + i * 13;
            fill(g, tx + 10, sy, 8, 8, swatches[i]);
            text(g, names[i], tx + 24, sy, themeIndex == i ? WHITE : MUTED);
            if (themeIndex == i) fill(g, tx + themeW - 12, sy + 2, 3, 3, swatches[i]);
        }

        super.render(g, mouseX, mouseY, delta);
    }

    
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) return super.mouseClicked(event, doubleClick);
        int mx = (int) event.x(), my = (int) event.y();

        int colGap = 10;
        int colWidth = Math.min(166, (width - 70 - colGap * 4) / 5);
        int totalWidth = colWidth * 5 + colGap * 4;
        int startX = (width - totalWidth) / 2;
        int colY = Math.max(28, (height - 340) / 2);
        int colHeight = Math.min(350, height - colY - 24);

        int themeW = 142, themeH = 82;
        int tx = width - themeW - 8, ty = height - themeH - 8;
        if (mx >= tx && mx <= tx + themeW && my >= ty + 22 && my <= ty + themeH) {
            int index = Math.max(0, Math.min(3, (my - (ty + 25)) / 13));
            themeIndex = index;
            accentColor = new int[]{0xFFE78BC8, 0xFFC68AF3, 0xFF8F9BFF, 0xFF75D6C4}[index];
            return true;
        }

        for (int c = 0; c < CATEGORIES.length; c++) {
            String cat = CATEGORIES[c];
            int x = startX + c * (colWidth + colGap);
            if (mx < x || mx > x + colWidth || my < colY || my > colY + colHeight) continue;
            List<Module> entries = modules.stream().filter(m -> m.getCategory().equals(cat)).toList();
            int rowY = colY + 34;
            int scroll = columnScroll.getOrDefault(cat, 0);
            int rows = Math.max(1, (colHeight - 44) / 27);
            for (int n = 0; n < rows; n++) {
                int index = scroll + n;
                if (index >= entries.size()) break;
                int ry = rowY + n * 27;
                if (my >= ry && my <= ry + 25) {
                    Module module = entries.get(index);
                    selected = selected == module ? null : module;
                    if (isImplemented(module)) module.toggle();
                    return true;
                }
            }
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int colGap = 10;
        int colWidth = Math.min(166, (width - 70 - colGap * 4) / 5);
        int totalWidth = colWidth * 5 + colGap * 4;
        int startX = (width - totalWidth) / 2;
        int colY = Math.max(28, (height - 340) / 2);
        int colHeight = Math.min(350, height - colY - 24);
        for (int c = 0; c < CATEGORIES.length; c++) {
            String cat = CATEGORIES[c];
            int x = startX + c * (colWidth + colGap);
            if (mouseX < x || mouseX > x + colWidth || mouseY < colY || mouseY > colY + colHeight) continue;
            int count = (int) modules.stream().filter(m -> m.getCategory().equals(cat)).count();
            int rows = Math.max(1, (colHeight - 44) / 27);
            int next = columnScroll.getOrDefault(cat, 0) - (int) Math.signum(verticalAmount);
            columnScroll.put(cat, Math.max(0, Math.min(Math.max(0, count - rows), next)));
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
