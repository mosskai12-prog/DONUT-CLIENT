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
    private static final String[] CATEGORIES = {"All", "Movement", "Render", "HUD", "Favorites"};

    public DonutScreen() {
        super(Component.literal("DONUT CLIENT"));
        modules.add(new Module("Toggle Sprint", "Movement", "Automatically sprint while moving."));
        modules.add(new Module("Fullbright", "Render", "Makes dark areas easier to see."));
        modules.add(new Module("Coordinates HUD", "HUD", "Shows your current block coordinates."));
        modules.add(new Module("FPS Counter", "HUD", "Displays your current frame rate."));
        selected = modules.get(0);
    }

    @Override public boolean isPauseScreen() { return false; }

    private int panelWidth() { return Math.min(1050, width - 24); }
    private int panelHeight() { return Math.min(620, height - 24); }
    private int left() { return (width - panelWidth()) / 2; }
    private int top() { return (height - panelHeight()) / 2; }
    private int listLeft() { return left() + 26; }
    private int listWidth() { return Math.max(240, (panelWidth() - 410) / 2); }
    private int detailsLeft() { return listLeft() + listWidth() + 18; }
    private int detailsWidth() { return left() + panelWidth() - 26 - detailsLeft(); }
    private int contentTop() { return top() + 174; }

    private List<Module> visibleModules() {
        return modules.stream().filter(m -> category.equals("All")
                || (category.equals("Favorites") && m.isFavorite())
                || m.getCategory().equals(category)).toList();
    }

    private String[] options(Module module) {
        return switch (module.getName()) {
            case "Fullbright" -> new String[]{"Maximum brightness", "Restore brightness on disable"};
            case "Toggle Sprint" -> new String[]{"Require forward movement", "Stop while sneaking"};
            case "Coordinates HUD" -> new String[0];
            case "FPS Counter" -> new String[0];
            default -> new String[0];
        };
    }

    private String settingKey(String value) {
        return value.toLowerCase().replaceAll("[^a-z0-9]+", "_");
    }

    private void fill(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + h, color);
    }

    private void drawText(GuiGraphics g, String value, int x, int y, int color) {
        g.drawString(font, value, x, y, color, false);
    }

    private int mix(int a, int b, float t) {
        int r = (int)(((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
        int gr = (int)(((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
        int bl = (int)((a & 255) * (1 - t) + (b & 255) * t);
        return 0xFF000000 | (r << 16) | (gr << 8) | bl;
    }

    private void drawSwitch(GuiGraphics g, int x, int y, boolean enabled, String id) {
        float target = enabled ? 1f : 0f;
        float value = animations.compute(id, (key, old) ->
                old == null ? target : old + (target - old) * 0.3f);
        fill(g, x, y, 36, 19, mix(0xFF343B47, 0xFF286747, value));
        fill(g, x + 2 + Math.round(17 * value), y + 2, 15, 15, WHITE);
    }

    private void drawModuleCard(GuiGraphics g, Module module, int index, int mouseX, int mouseY) {
        int x = listLeft();
        int y = contentTop() + index * 58;
        int w = listWidth();
        boolean hover = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + 50;
        boolean active = module == selected;
        fill(g, x, y, w, 50, active ? 0xFF302129 : hover ? CARD_HOVER : CARD);
        fill(g, x, y, 3, 50, active ? RED : BORDER);
        fill(g, x + 13, y + 13, 25, 25, active ? 0xFF63303A : 0xFF303744);
        drawText(g, module.getName().substring(0, 1), x + 22, y + 21, WHITE);
        drawText(g, module.getName(), x + 49, y + 11, WHITE);
        drawText(g, module.getCategory().toUpperCase(), x + 49, y + 29, MUTED);
        drawText(g, module.isFavorite() ? "★" : "☆", x + w - 91, y + 18,
                module.isFavorite() ? RED : MUTED);
        drawSwitch(g, x + w - 52, y + 15, module.isEnabled(), "module." + module.getName());
    }

    private void drawSettings(GuiGraphics g) {
        int x = detailsLeft(), y = contentTop(), w = detailsWidth(), h = panelHeight() - 205;
        fill(g, x, y, w, h, PANEL);
        fill(g, x, y, w, 1, BORDER);
        if (selected == null) {
            drawText(g, "SELECT A MODULE", x + 20, y + 26, RED);
            drawText(g, "Choose a feature from the library.", x + 20, y + 57, MUTED);
            return;
        }

        drawText(g, "MODULE CONTROL", x + 20, y + 23, RED);
        drawText(g, selected.getName(), x + 20, y + 53, WHITE);
        drawText(g, selected.getDescription(), x + 20, y + 72, MUTED);

        int toggleY = y + 100;
        fill(g, x + 16, toggleY, w - 32, 43, PANEL_ALT);
        drawText(g, "Enabled", x + 29, toggleY + 17, WHITE);
        drawSwitch(g, x + w - 69, toggleY + 12, selected.isEnabled(), "details.enabled");

        drawText(g, "SETTINGS", x + 20, y + 166, MUTED);
        String[] settings = options(selected);
        for (int i = 0; i < settings.length; i++) {
            int yy = y + 183 + i * 43;
            String option = settings[i];
            boolean value = ModuleConfig.getBoolean(selected.getName(), settingKey(option), true);
            fill(g, x + 16, yy, w - 32, 37, PANEL_ALT);
            drawText(g, option, x + 28, yy + 13, WHITE);
            drawSwitch(g, x + w - 69, yy + 9, value, "setting." + selected.getName() + "." + settingKey(option));
        }
        drawText(g, "Settings are saved automatically", x + 20, y + h - 19, MUTED);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        open = Math.min(1f, open + delta * 4f);
        int offset = Math.round((1f - open) * 13f);
        int x = left(), y = top() + offset, w = panelWidth(), h = panelHeight();
        g.fill(0, 0, width, height, 0xD9000000);
        fill(g, x, y, w, h, BG);

        fill(g, x, y, w, 70, PANEL);
        fill(g, x, y, 4, h, RED);
        fill(g, x + 22, y + 17, 36, 36, RED);
        drawText(g, "D", x + 35, y + 29, WHITE);
        drawText(g, "DONUT CLIENT", x + 72, y + 19, WHITE);
        drawText(g, "UNIVERSAL FABRIC CLIENT", x + 72, y + 39, MUTED);
        long enabled = modules.stream().filter(Module::isEnabled).count();
        fill(g, x + w - 190, y + 20, 112, 28, PANEL_ALT);
        fill(g, x + w - 176, y + 31, 6, 6, GREEN);
        drawText(g, enabled + " ENABLED", x + w - 162, y + 29, WHITE);
        drawText(g, "RIGHT SHIFT  ×", x + w - 70, y + 29, MUTED);

        drawText(g, "CATEGORIES", x + 26, y + 85, MUTED);
        int tabX = x + 26, tabY = y + 99;
        for (String item : CATEGORIES) {
            int tabW = Math.max(48, font.width(item) + 22);
            boolean chosen = category.equals(item);
            boolean hover = mouseX >= tabX && mouseX <= tabX + tabW
                    && mouseY >= tabY && mouseY <= tabY + 30;
            fill(g, tabX, tabY, tabW, 30, chosen ? 0xFF482630 : hover ? CARD_HOVER : PANEL);
            if (chosen) fill(g, tabX, tabY + 28, tabW, 2, RED);
            drawText(g, item, tabX + 11, tabY + 10, chosen ? WHITE : MUTED);
            tabX += tabW + 8;
        }

        int headingY = y + 148;
        drawText(g, "MODULE LIBRARY", x + 26, headingY, WHITE);
        drawText(g, "Select a module to configure it", x + 26, headingY + 17, MUTED);
        drawText(g, "FEATURES", listLeft(), headingY + 39, MUTED);
        drawText(g, "CONFIGURATION", detailsLeft(), headingY + 39, MUTED);

        List<Module> visible = visibleModules();
        int maxRows = Math.max(1, (h - 225) / 58);
        for (int i = 0; i < Math.min(visible.size(), maxRows); i++) {
            drawModuleCard(g, visible.get(i), i, mouseX, mouseY);
        }
        if (visible.isEmpty()) drawText(g, "No favourites yet", listLeft() + 14, contentTop() + 18, MUTED);
        drawSettings(g);

        fill(g, x, y + h - 34, w, 34, PANEL);
        fill(g, x, y + h - 35, w, 1, BORDER);
        drawText(g, "DONUT CLIENT  •  MINECRAFT 1.21.11", x + 22, y + h - 21, MUTED);
        drawText(g, "4 WORKING MODULES", x + w - 150, y + h - 21, MUTED);
        super.render(g, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) return super.mouseClicked(event, doubleClick);
        double mx = event.x(), my = event.y();
        int x = left(), y = top() + Math.round((1f - open) * 13f);
        int tabX = x + 26, tabY = y + 99;

        for (String item : CATEGORIES) {
            int tabW = Math.max(48, font.width(item) + 22);
            if (mx >= tabX && mx <= tabX + tabW && my >= tabY && my <= tabY + 30) {
                category = item;
                selected = null;
                return true;
            }
            tabX += tabW + 8;
        }

        List<Module> visible = visibleModules();
        int maxRows = Math.max(1, (panelHeight() - 225) / 58);
        for (int i = 0; i < Math.min(visible.size(), maxRows); i++) {
            Module module = visible.get(i);
            int rowX = listLeft(), rowY = contentTop() + i * 58, rowW = listWidth();
            if (mx >= rowX && mx <= rowX + rowW && my >= rowY && my <= rowY + 50) {
                if (mx >= rowX + rowW - 92 && mx < rowX + rowW - 62) module.toggleFavorite();
                else if (mx >= rowX + rowW - 54) module.toggle();
                else selected = module;
                return true;
            }
        }

        if (selected != null) {
            int sx = detailsLeft(), sy = contentTop(), sw = detailsWidth();
            int toggleY = sy + 100;
            if (mx >= sx + 16 && mx <= sx + sw - 16 && my >= toggleY && my <= toggleY + 43) {
                selected.toggle();
                return true;
            }
            String[] settings = options(selected);
            for (int i = 0; i < settings.length; i++) {
                int yy = sy + 183 + i * 43;
                if (mx >= sx + 16 && mx <= sx + sw - 16 && my >= yy && my <= yy + 37) {
                    String key = settingKey(settings[i]);
                    boolean current = ModuleConfig.getBoolean(selected.getName(), key, true);
                    ModuleConfig.setBoolean(selected.getName(), key, !current);
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }
}