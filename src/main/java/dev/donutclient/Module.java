package dev.donutclient;

public class Module {

    private final String name;
    private final String category;
    private final String description;

    private boolean enabled;
    private boolean favorite;

    public Module(String name, String category, String description) {
        this.name = name;
        this.category = category;
        this.description = description;
        this.enabled = ModuleConfig.isEnabled(name);
        this.favorite = ModuleConfig.isFavorite(name);
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public String getDescription() {
        return description;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void toggle() {
        enabled = !enabled;
        ModuleConfig.setEnabled(name, enabled);
    }

    public boolean isFavorite() {
        return favorite;
    }

    public void toggleFavorite() {
        favorite = !favorite;
        ModuleConfig.setFavorite(name, favorite);
    }
}
