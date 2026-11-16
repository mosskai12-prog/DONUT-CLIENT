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

    private static final int BG = 0xFF101115, HEADER = 0xFF17191F;
    private static final int SIDE = 0xFF14161B, PANEL = 0xFF191C23;
    private static final int CARD = 0xFF20242D, HOVER = 0xFF292E39;
    private static final int LINE = 0xFF303541, WHITE = 0xFFF5F6F8;
    private static final int MUTED = 0xFF929AA8, RED = 0xFFE65361;
    private static final int GREEN = 0xFF6ED39A;
    private static final String[] CATEGORIES = {"All","Combat","Movement","Render","World","Player","HUD","Favorites"};

    public DonutScreen() {
        super(Component.literal("DONUT CLIENT"));
        modules.add(new Module("Base Finder","World","Finds configured bases."));
        modules.add(new Module("Fullbright","Render","Brightens dark areas."));
        modules.add(new Module("Fly","Movement","Flight movement module."));
        modules.add(new Module("Progress Bar","HUD","Displays a progress bar."));
        modules.add(new Module("Name Hider","Render","Hides selected names."));
        modules.add(new Module("Hotbar","HUD","Custom hotbar display."));
        modules.add(new Module("Amethyst Scanner","World","Scans amethyst blocks."));
        modules.add(new Module("Light Debug","Render","Displays light information."));
        modules.add(new Module("Sound Detector","World","Detects nearby sounds."));
        modules.add(new Module("BlockESP","Render","Highlights selected ores."));
        modules.add(new Module("StorageESP","Render","Highlights nearby storage blocks."));
    }

    @Override public boolean isPauseScreen() { return false; }
    private int pw() { return Math.min(1040, width - 24); }
    private int ph() { return Math.min(610, height - 24); }
    private int px() { return (width - pw()) / 2; }
    private int py() { return (height - ph()) / 2; }
    private int sw() { return 166; }
    private int rw() { return 258; }
    private int cx() { return px() + sw() + 22; }
    private int cw() { return pw() - sw() - rw() - 58; }
    private int rx() { return px() + pw() - rw(); }
    private int cardW() { return (cw() - 14) / 2; }

    private List<Module> visible() {
        return modules.stream().filter(m -> category.equals("All")
                || (category.equals("Favorites") && m.isFavorite())
                || m.getCategory().equals(category)).toList();
    }

    private String[] options(Module m) {
        return switch (m.getName()) {
            case "Fullbright" -> new String[]{"Maximum brightness","Restore brightness on disable"};
            case "Base Finder" -> new String[]{"Show distance","Show coordinates"};
            case "Fly" -> new String[]{"Disable when grounded","Show flight indicator"};
            case "Progress Bar" -> new String[]{"Show percentage","Compact display"};
            case "Name Hider" -> new String[]{"Hide player names","Hide mob names"};
            case "Hotbar" -> new String[]{"Show item counts","Compact layout"};
            case "Amethyst Scanner" -> new String[]{"Show cluster markers","Show distance"};
            case "Light Debug" -> new String[]{"Show block light","Show sky light"};
            case "Sound Detector" -> new String[]{"Show direction","Show distance"};
            case "BlockESP" -> new String[]{
                    "Diamond Ore","Deepslate Diamond Ore","Ancient Debris",
                    "Emerald Ore","Gold Ore","Iron Ore","Copper Ore",
                    "Redstone Ore","Lapis Lazuli Ore","Coal Ore"
            };
            case "StorageESP" -> new String[]{
                    "Chests","Trapped Chests","Ender Chests","Barrels","Shulker Boxes"
            };
            default -> new String[]{"Show module indicator","Enable notifications"};
        };
    }

    private String key(String s) { return s.toLowerCase().replaceAll("[^a-z0-9]+","_"); }
    private void txt(GuiGraphics g,String s,int x,int y,int color) { g.drawString(font,s,x,y,color,false); }
    private void toggle(GuiGraphics g,int x,int y,boolean on) {
        g.fill(x,y,x+30,y+16,on ? 0xFF326B50 : 0xFF424955);
        g.fill(on ? x+16 : x+2,y+2,on ? x+28 : x+14,y+14,WHITE);
    }

    @Override
    public void render(GuiGraphics g,int mouseX,int mouseY,float delta) {
        g.fill(0,0,width,height,0xC9000000);
        int x=px(), y=py(), w=pw(), bottom=y+ph();
        int sideY=y+72, top=y+96, rightX=rx(), rightY=y+72, contentX=cx();
        g.fill(x,y,x+w,bottom,BG);
        g.fill(x,y,x+w,y+72,HEADER);
        g.fill(x,y,x+4,bottom,RED);
        g.fill(x,y+71,x+w,y+73,LINE);

        // Logo and top status
        g.fill(x+20,y+17,x+54,y+51,RED);
        txt(g,"D",x+31,y+27,WHITE);
        txt(g,"DONUT CLIENT",x+66,y+19,WHITE);
        txt(g,"MINECRAFT UTILITY",x+66,y+39,MUTED);
        long active=modules.stream().filter(Module::isEnabled).count();
        g.fill(x+w-176,y+20,x+w-18,y+51,CARD);
        g.fill(x+w-165,y+31,x+w-159,y+37,GREEN);
        txt(g,active+" MODULES ACTIVE",x+w-151,y+31,WHITE);

        // Sidebar navigation
        g.fill(x,sideY,x+sw(),bottom-34,SIDE);
        txt(g,"WORKSPACE",x+19,sideY+22,MUTED);
        for(int i=0;i<CATEGORIES.length;i++) {
            int cy=sideY+42+i*39;
            boolean chosen=category.equals(CATEGORIES[i]);
            boolean hover=mouseX>=x+10 && mouseX<=x+sw()-10 && mouseY>=cy-8 && mouseY<=cy+23;
            if(chosen) {
                g.fill(x+10,cy-8,x+sw()-10,cy+23,0xFF34232A);
                g.fill(x+10,cy-8,x+13,cy+23,RED);
            } else if(hover) g.fill(x+10,cy-8,x+sw()-10,cy+23,CARD);
            txt(g,CATEGORIES[i],x+24,cy+3,chosen?WHITE:MUTED);
        }

        // Module cards
        txt(g,"MODULES",contentX,top,RED);
        txt(g,category,contentX,top+20,WHITE);
        txt(g,"Manage your client features",contentX,top+39,MUTED);
        List<Module> list=visible();
        int cardTop=top+62, cardW=cardW(), cardH=76;
        for(int i=0;i<list.size();i++) {
            Module m=list.get(i);
            int col=i%2,row=i/2,bx=contentX+col*(cardW+14),by=cardTop+row*88;
            boolean chosen=selected==m;
            boolean hover=mouseX>=bx && mouseX<=bx+cardW && mouseY>=by && mouseY<=by+cardH;
            g.fill(bx,by,bx+cardW,by+cardH,chosen?0xFF30232A:hover?HOVER:CARD);
            g.fill(bx,by,bx+cardW,by+1,chosen?RED:LINE);
            g.fill(bx,by,bx+3,by+cardH,chosen?RED:0xFF343A45);
            txt(g,m.getName(),bx+14,by+15,WHITE);
            txt(g,m.getDescription(),bx+14,by+36,MUTED);
            txt(g,m.getCategory().toUpperCase(),bx+14,by+57,0xFF737D8B);
            txt(g,m.isFavorite()?"★":"☆",bx+cardW-78,by+15,m.isFavorite()?RED:MUTED);
            toggle(g,bx+cardW-48,by+48,m.isEnabled());
        }

        // Settings panel
        g.fill(rightX,rightY,x+w,bottom-34,PANEL);
        g.fill(rightX,rightY,rightX+1,bottom-34,LINE);
        txt(g,"CONFIGURATION",rightX+18,rightY+23,RED);
        if(selected==null) {
            g.fill(rightX+16,rightY+53,x+w-16,rightY+142,CARD);
            txt(g,"SELECT A MODULE",rightX+29,rightY+72,WHITE);
            txt(g,"Choose any module card",rightX+29,rightY+94,MUTED);
            txt(g,"to see its settings here.",rightX+29,rightY+109,MUTED);
        } else {
            txt(g,selected.getName(),rightX+18,rightY+57,WHITE);
            txt(g,selected.getDescription(),rightX+18,rightY+77,MUTED);
            int enableY=rightY+98;
            g.fill(rightX+16,enableY,x+w-16,enableY+39,CARD);
            txt(g,"Enabled",rightX+28,enableY+15,WHITE);
            toggle(g,x+w-61,enableY+11,selected.isEnabled());
            txt(g,"MODULE OPTIONS",rightX+18,rightY+160,MUTED);
            String[] opts=options(selected);
            boolean compactOptions = selected.getName().equals("BlockESP") || selected.getName().equals("StorageESP");
            for(int i=0;i<opts.length;i++) {
                int oy=rightY+(compactOptions ? 142 : 176)+i*(compactOptions ? 27 : 49);
                boolean value=ModuleConfig.getBoolean(selected.getName(),key(opts[i]),true);
                int rowHeight = compactOptions ? 23 : 39;
                g.fill(rightX+16,oy,x+w-16,oy+rowHeight,CARD);
                txt(g,opts[i],rightX+28,oy+(compactOptions ? 8 : 15),WHITE);
                toggle(g,x+w-61,oy+(compactOptions ? 4 : 11),value);
            }
            txt(g,"Settings save automatically",rightX+18,bottom-57,MUTED);
        }

        // Footer
        g.fill(x,bottom-34,x+w,bottom,HEADER);
        g.fill(x,bottom-35,x+w,bottom-34,LINE);
        txt(g,"DONUT CLIENT  /  RIGHT SHIFT TO CLOSE",x+18,bottom-21,MUTED);
        txt(g,"1.0.0",x+w-49,bottom-21,MUTED);
        super.render(g,mouseX,mouseY,delta);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event,boolean doubleClick) {
        double mx=event.x(),my=event.y();
        if(event.button()==0) {
            int x=px(),y=py(),w=pw(),sideY=y+72;
            for(int i=0;i<CATEGORIES.length;i++) {
                int cy=sideY+42+i*39;
                if(mx>=x && mx<=x+sw() && my>=cy-8 && my<=cy+23) {
                    category=CATEGORIES[i]; selected=null; return true;
                }
            }
            int contentX=cx(),top=y+96,cardTop=top+62,cardW=cardW();
            List<Module> list=visible();
            for(int i=0;i<list.size();i++) {
                Module m=list.get(i);
                int col=i%2,row=i/2,bx=contentX+col*(cardW+14),by=cardTop+row*88;
                if(mx>=bx && mx<=bx+cardW && my>=by && my<=by+76) {
                    if(mx>=bx+cardW-84 && mx<bx+cardW-52) m.toggleFavorite();
                    else if(mx>=bx+cardW-50 && my>=by+43) m.toggle();
                    else selected=m;
                    return true;
                }
            }
            if(selected!=null) {
                int rightX=rx(),rightY=y+72,enableY=rightY+98;
                if(mx>=rightX+16 && mx<=x+w-16 && my>=enableY && my<=enableY+39) {
                    selected.toggle(); return true;
                }
                String[] opts=options(selected);
                boolean compactOptions = selected.getName().equals("BlockESP") || selected.getName().equals("StorageESP");
                for(int i=0;i<opts.length;i++) {
                    int oy=rightY+(compactOptions ? 142 : 176)+i*(compactOptions ? 27 : 49);
                    int rowHeight = compactOptions ? 23 : 39;
                    if(mx>=rightX+16 && mx<=x+w-16 && my>=oy && my<=oy+rowHeight) {
                        String setting=key(opts[i]);
                        boolean value=ModuleConfig.getBoolean(selected.getName(),setting,true);
                        ModuleConfig.setBoolean(selected.getName(),setting,!value);
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(event,doubleClick);
    }
}
