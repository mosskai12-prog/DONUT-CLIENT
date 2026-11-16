package dev.donutclient;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class DonutScreen extends Screen {

    private final List<Module> modules = new ArrayList<>();
    private String category = "All modules";
    private Module selected;

    private static final int BACKGROUND = 0xFF191B20;
    private static final int PANEL = 0xFF22252B;
    private static final int BORDER = 0xFF41454D;
    private static final int WHITE = 0xFFF0F0F0;
    private static final int MUTED = 0xFF9DA1AA;
    private static final int ACCENT = 0xFFDCA4A4;
    private static final int GREEN = 0xFFB9E889;

    public DonutScreen() {
        super(Component.literal("DONUT CLIENT"));
        modules.add(new Module("Base Finder", "Visual", "Finds configured bases."));
        modules.add(new Module("Fullbright", "Visual", "Improves world brightness."));
        modules.add(new Module("Fly", "Movement", "Movement module."));
        modules.add(new Module("Progress Bar", "HUD", "Displays a progress bar."));
        modules.add(new Module("Name Hider", "Visual", "Hides selected names."));
        modules.add(new Module("Hotbar", "HUD", "Custom hotbar display."));
        modules.add(new Module("Amethyst Scanner", "Visual", "Scans amethyst blocks."));
        modules.add(new Module("Light Debug", "Visual", "Displays light information."));
        modules.add(new Module("Sound Detector", "Visual", "Detects nearby sounds."));
        modules.add(new Module("BlockESP", "Visual", "Highlights selected blocks."));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        renderBackground(g, mouseX, mouseY, delta);

        int x = (width - 1000) / 2;
        int y = (height - 580) / 2;
        int w = 1000;
        int h = 580;

        g.fill(x, y, x + w, y + h, BACKGROUND);
        g.fill(x, y, x + w, y + 48, 0xFF15171B);
        g.drawString(font, "DONUT", x + 20, y + 12, 0xFFBCE5F0, false);
        g.drawString(font, "MODULE CONTROL", x + 20, y + 27, MUTED, false);
        g.drawString(font, "MODULES", x + 700, y + 18, WHITE, false);
        g.drawString(font, "HUD", x + 790, y + 18, MUTED, false);
        g.drawString(font, "APPEARANCE", x + 850, y + 18, MUTED, false);

        int sideX = x;
        int sideY = y + 48;
        g.fill(sideX, sideY, sideX + 145, y + h - 25, 0xFF1C1F24);
        g.drawString(font, "WORKSPACE", sideX + 15, sideY + 15, ACCENT, false);

        String[] categories = {"All modules", "Visual", "Movement", "HUD", "Favorites"};
        for (int i = 0; i < categories.length; i++) {
            int cy = sideY + 35 + i * 34;
            if (category.equals(categories[i])) {
                g.fill(sideX + 7, cy - 5, sideX + 137, cy + 22, 0xFF34372F);
            }
            g.drawString(font, categories[i], sideX + 17, cy + 4,
                    category.equals(categories[i]) ? WHITE : MUTED, false);
        }

        int contentX = x + 165;
        int contentY = y + 70;
        int contentW = 535;
        g.drawString(font, "YOUR MODULE DESK", contentX, contentY, ACCENT, false);
        g.drawString(font, category, contentX, contentY + 18, WHITE, false);
        g.fill(contentX, contentY + 40, contentX + contentW, contentY + 70, PANEL);
        g.drawString(font, "Search modules...", contentX + 12, contentY + 51, MUTED, false);

        List<Module> visible = getVisibleModules();
        int cardY = contentY + 82;
        int cardW = 258;
        int cardH = 58;

        for (int i = 0; i < visible.size(); i++) {
            Module m = visible.get(i);
            int col = i % 2;
            int row = i / 2;
            int cx = contentX + col * 270;
            int cy = cardY + row * 68;
            boolean active = m.isEnabled();

            g.fill(cx, cy, cx + cardW, cy + cardH, PANEL);
            g.fill(cx, cy, cx + cardW, cy + 1, BORDER);
            g.fill(cx, cy + cardH - 1, cx + cardW, cy + cardH, BORDER);
            g.drawString(font, m.getName(), cx + 12, cy + 12, WHITE, false);
            g.drawString(font, active ? "ACTIVE" : "INACTIVE", cx + 12, cy + 34,
                    active ? GREEN : MUTED, false);

            int sx = cx + cardW - 34;
            int sy = cy + 22;
            g.fill(sx, sy, sx + 23, sy + 13, active ? GREEN : 0xFF55585F);
            g.fill(active ? sx + 12 : sx + 2, sy + 2,
                    active ? sx + 21 : sx + 11, sy + 11, WHITE);
        }

        int settingsX = x + 720;
        int settingsY = y + 70;
        g.fill(settingsX, settingsY, x + w, y + h - 25, 0xFF1D2025);
        g.drawString(font, "MODULE SETTINGS", settingsX + 15, settingsY + 5, MUTED, false);

        if (selected == null) {
            g.drawString(font, "Select a module", settingsX + 15, settingsY + 45, WHITE, false);
            g.drawString(font, "Click a module to configure it.", settingsX + 15,
                    settingsY + 65, MUTED, false);
        } else {
            g.drawString(font, selected.getName(), settingsX + 15, settingsY + 45, WHITE, false);
            g.drawString(font, selected.getCategory(), settingsX + 15, settingsY + 65, ACCENT, false);
            g.drawString(font, selected.getDescription(), settingsX + 15, settingsY + 95, MUTED, false);
            g.drawString(font, "Configuration", settingsX + 15, settingsY + 145, WHITE, false);
            g.drawString(font, "Status:", settingsX + 15, settingsY + 175, MUTED, false);
            g.drawString(font, selected.isEnabled() ? "Enabled" : "Disabled",
                    settingsX + 75, settingsY + 175, selected.isEnabled() ? GREEN : MUTED, false);
        }

        g.fill(x, y + h - 25, x + w, y + h, 0xFF15171B);
        long activeCount = modules.stream().filter(Module::isEnabled).count();
        g.drawString(font, "DONUT CLIENT", x + 15, y + h - 17, MUTED, false);
        g.drawString(font, activeCount + " modules active", x + w - 135, y + h - 17, MUTED, false);

        super.render(g, mouseX, mouseY, delta);
    }

    private List<Module> getVisibleModules() {
        return modules.stream()
                .filter(m -> category.equals("All modules")
                        || (category.equals("Favorites") && m.isFavorite())
                        || m.getCategory().equals(category))
                .toList();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();

        if (button == 0) {
            int x = (width - 1000) / 2;
            int y = (height - 580) / 2;

            String[] categories = {"All modules", "Visual", "Movement", "HUD", "Favorites"};
            for (int i = 0; i < categories.length; i++) {
                int cy = y + 48 + 35 + i * 34;
                if (mouseX >= x && mouseX <= x + 145
                        && mouseY >= cy - 5 && mouseY <= cy + 22) {
                    category = categories[i];
                    selected = null;
                    return true;
                }
            }

            int contentX = x + 165;
            int cardY = y + 70 + 82;
            List<Module> visible = getVisibleModules();
            for (int i = 0; i < visible.size(); i++) {
                int col = i % 2;
                int row = i / 2;
                int cx = contentX + col * 270;
                int cy = cardY + row * 68;
                if (mouseX >= cx && mouseX <= cx + 258
                        && mouseY >= cy && mouseY <= cy + 58) {
                    Module m = visible.get(i);
                    selected = m;
                    if (mouseX >= cx + 220) {
                        m.toggle();
                    }
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }
}
