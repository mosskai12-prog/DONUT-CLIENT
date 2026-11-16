package dev.donutclient;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class DonutScreen extends Screen {

    private final List<Module> modules = new ArrayList<>();
    private String category = "All";
    private Module selected;

    private static final int BG = 0xFF101216;
    private static final int PANEL = 0xFF191D23;
    private static final int CARD = 0xFF20252D;
    private static final int CARD_HOVER = 0xFF292F39;
    private static final int LINE = 0xFF303741;
    private static final int WHITE = 0xFFF3F4F6;
    private static final int MUTED = 0xFF929BA8;
    private static final int RED = 0xFFE45B66;
    private static final int GREEN = 0xFF75D69A;

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
        modules.add(new Module("BlockESP", "Render", "Highlights selected blocks."));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private int panelWidth() {
        return Math.min(980, width - 24);
    }

    private int panelHeight() {
        return Math.min(570, height - 24);
    }

    private int panelX() {
        return (width - panelWidth()) / 2;
    }

    private int panelY() {
        return (height - panelHeight()) / 2;
    }

    private int sidebarWidth() {
        return 154;
    }

    private int settingsWidth() {
        return 244;
    }

    private int contentX() {
        return panelX() + sidebarWidth() + 18;
    }

    private int contentWidth() {
        return panelWidth() - sidebarWidth() - settingsWidth() - 48;
    }

    private int settingsX() {
        return panelX() + panelWidth() - settingsWidth();
    }

    private int cardWidth() {
        return (contentWidth() - 12) / 2;
    }

    private List<Module> visibleModules() {
        return modules.stream()
                .filter(m -> category.equals("All")
                        || (category.equals("Favorites") && m.isFavorite())
                        || m.getCategory().equals(category))
                .toList();
    }

    private String[] settingsFor(Module module) {
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
            case "BlockESP" -> new String[]{"Highlight ores", "Highlight containers"};
            default -> new String[]{"Show module indicator", "Enable notifications"};
        };
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        g.fill(0, 0, width, height, 0xB9000000);

        int x = panelX();
        int y = panelY();
        int w = panelWidth();
        int h = panelHeight();
        int bottom = y + h;

        g.fill(x, y, x + w, bottom, BG);
        g.fill(x, y, x + w, y + 66, PANEL);
        g.fill(x, y, x + 4, bottom, RED);
        g.fill(x, y + 65, x + w, y + 67, LINE);

        g.drawString(font, "DONUT", x + 22, y + 17, WHITE, false);
        g.drawString(font, "CLIENT", x + 22 + font.width("DONUT") + 5, y + 17, RED, false);
        g.drawString(font, "A clean, modular Minecraft client", x + 22, y + 37, MUTED, false);

        long active = modules.stream().filter(Module::isEnabled).count();
        g.fill(x + w - 164, y + 19, x + w - 18, y + 47, CARD);
        g.drawString(font, active + " ACTIVE MODULES", x + w - 153, y + 29, GREEN, false);

        int sideX = x;
        int sideY = y + 67;
        g.fill(sideX, sideY, sideX + sidebarWidth(), bottom - 30, PANEL);
        g.drawString(font, "MENU", sideX + 18, sideY + 20, MUTED, false);

        for (int i = 0; i < CATEGORIES.length; i++) {
            int cy = sideY + 38 + i * 38;
            boolean chosen = category.equals(CATEGORIES[i]);
            if (chosen) {
                g.fill(sideX + 10, cy - 7, sideX + sidebarWidth() - 10, cy + 22, 0xFF35252B);
                g.fill(sideX + 10, cy - 7, sideX + 13, cy + 22, RED);
            }
            g.drawString(font, CATEGORIES[i], sideX + 23, cy + 2, chosen ? WHITE : MUTED, false);
        }

        int cx = contentX();
        int top = y + 91;
        g.drawString(font, "MODULE LIBRARY", cx, top, RED, false);
        g.drawString(font, category, cx, top + 19, WHITE, false);
        g.drawString(font, "Choose a module to view its options", cx, top + 36, MUTED, false);

        List<Module> visible = visibleModules();
        int cw = cardWidth();
        int cardTop = top + 60;
        int cardH = 62;

        for (int i = 0; i < visible.size(); i++) {
            Module m = visible.get(i);
            int col = i % 2;
            int row = i / 2;
            int bx = cx + col * (cw + 12);
            int by = cardTop + row * 72;
            boolean chosen = selected == m;
            boolean hovered = mouseX >= bx && mouseX <= bx + cw && mouseY >= by && mouseY <= by + cardH;

            g.fill(bx, by, bx + cw, by + cardH, chosen ? 0xFF30252B : (hovered ? CARD_HOVER : CARD));
            g.fill(bx, by, bx + 3, by + cardH, chosen ? RED : LINE);
            g.drawString(font, m.getName(), bx + 13, by + 12, WHITE, false);
            g.drawString(font, m.getCategory().toUpperCase(), bx + 13, by + 34, MUTED, false);

            g.drawString(font, m.isFavorite() ? "★" : "☆", bx + cw - 68, by + 12,
                    m.isFavorite() ? RED : MUTED, false);

            int sx = bx + cw - 43;
            int sy = by + 24;
            g.fill(sx, sy, sx + 31, sy + 15, m.isEnabled() ? 0xFF326C50 : 0xFF454C56);
            g.fill(m.isEnabled() ? sx + 17 : sx + 2, sy + 2,
                    m.isEnabled() ? sx + 29 : sx + 14, sy + 13, WHITE);
        }

        int sx = settingsX();
        int sy = y + 67;
        g.fill(sx, sy, x + w, bottom - 30, PANEL);
        g.fill(sx, sy, sx + 1, bottom - 30, LINE);
        g.drawString(font, "MODULE SETTINGS", sx + 17, sy + 22, RED, false);

        if (selected == null) {
            g.fill(sx + 16, sy + 52, x + w - 16, sy + 132, CARD);
            g.drawString(font, "Nothing selected", sx + 28, sy + 69, WHITE, false);
            g.drawString(font, "Click a module card", sx + 28, sy + 89, MUTED, false);
            g.drawString(font, "to configure it.", sx + 28, sy + 103, MUTED, false);
        } else {
            g.drawString(font, selected.getName(), sx + 17, sy + 53, WHITE, false);
            g.drawString(font, selected.getDescription(), sx + 17, sy + 72, MUTED, false);

            int toggleY = sy + 96;
            g.fill(sx + 16, toggleY, x + w - 16, toggleY + 34, CARD);
            g.drawString(font, "Module enabled", sx + 27, toggleY + 12, WHITE, false);
            drawSmallToggle(g, x + w - 55, toggleY + 10, selected.isEnabled());

            g.drawString(font, "OPTIONS", sx + 17, sy + 151, MUTED, false);
            String[] options = settingsFor(selected);
            for (int i = 0; i < options.length; i++) {
                int oy = sy + 166 + i * 48;
                boolean value = ModuleConfig.getBoolean(selected.getName(), settingKey(options[i]), defaultSetting(options[i]));
                g.fill(sx + 16, oy, x + w - 16, oy + 38, CARD);
                g.drawString(font, options[i], sx + 27, oy + 13, WHITE, false);
                drawSmallToggle(g, x + w - 55, oy + 11, value);
            }

            g.drawString(font, "Changes are saved automatically", sx + 17, bottom - 54, MUTED, false);
        }

        g.fill(x, bottom - 30, x + w, bottom, PANEL);
        g.fill(x, bottom - 31, x + w, bottom - 30, LINE);
        g.drawString(font, "DONUT CLIENT  •  RIGHT SHIFT TO CLOSE", x + 17, bottom - 19, MUTED, false);
        g.drawString(font, "v1.0.0", x + w - 48, bottom - 19, MUTED, false);

        super.render(g, mouseX, mouseY, delta);
    }

    private String settingKey(String label) {
        return label.toLowerCase().replaceAll("[^a-z0-9]+", "_");
    }

    private boolean defaultSetting(String label) {
        return true;
    }

    private void drawSmallToggle(GuiGraphics g, int x, int y, boolean enabled) {
        g.fill(x, y, x + 28, y + 14, enabled ? 0xFF326C50 : 0xFF454C56);
        g.fill(enabled ? x + 15 : x + 2, y + 2, enabled ? x + 26 : x + 13, y + 12, WHITE);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x();
        double my = event.y();

        if (event.button() == 0) {
            int x = panelX();
            int y = panelY();
            int w = panelWidth();
            int h = panelHeight();
            int bottom = y + h;

            int sideY = y + 67;
            for (int i = 0; i < CATEGORIES.length; i++) {
                int cy = sideY + 38 + i * 38;
                if (mx >= x && mx <= x + sidebarWidth() && my >= cy - 7 && my <= cy + 22) {
                    category = CATEGORIES[i];
                    selected = null;
                    return true;
                }
            }

            int cx = contentX();
            int top = y + 91;
            int cardTop = top + 60;
            int cw = cardWidth();
            List<Module> visible = visibleModules();

            for (int i = 0; i < visible.size(); i++) {
                Module m = visible.get(i);
                int col = i % 2;
                int row = i / 2;
                int bx = cx + col * (cw + 12);
                int by = cardTop + row * 72;
                if (mx >= bx && mx <= bx + cw && my >= by && my <= by + 62) {
                    if (mx >= bx + cw - 78 && mx < bx + cw - 48) {
                        m.toggleFavorite();
                    } else if (mx >= bx + cw - 47) {
                        m.toggle();
                    } else {
                        selected = m;
                    }
                    return true;
                }
            }

            if (selected != null) {
                int sx = settingsX();
                int sy = y + 67;
                int toggleY = sy + 96;
                if (mx >= sx + 16 && mx <= x + w - 16 && my >= toggleY && my <= toggleY + 34) {
                    selected.toggle();
                    return true;
                }

                String[] options = settingsFor(selected);
                for (int i = 0; i < options.length; i++) {
                    int oy = sy + 166 + i * 48;
                    if (mx >= sx + 16 && mx <= x + w - 16 && my >= oy && my <= oy + 38) {
                        String key = settingKey(options[i]);
                        boolean current = ModuleConfig.getBoolean(selected.getName(), key, defaultSetting(options[i]));
                        ModuleConfig.setBoolean(selected.getName(), key, !current);
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }
}
