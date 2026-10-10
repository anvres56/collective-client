package me.collective.addon;

import me.collective.category.Category;
import me.collective.command.Command;
import me.collective.module.Module;
import me.collective.module.ModuleManager;
import me.collective.command.CommandManager;

public abstract class CollectiveAddon {
    private String name;
    private String version = "1.0.0";
    private String author = "Unknown";
    private String description = "";
    private String website;

    public void initMetadata(String name, String version, String author, String description, String website) {
        this.name = name;
        if (version != null) this.version = version;
        if (author != null) this.author = author;
        if (description != null) this.description = description;
        this.website = website;
    }

    /**
     * Вызывается при загрузке аддона.
     */
    public abstract void onInitialize();

    /**
     * Вызывается при выгрузке/перезагрузке аддона.
     */
    public void onShutdown() {}

    /**
     * Вспомогательный метод регистрации модуля из аддона.
     */
    public void registerModule(Module module) {
        ModuleManager.INSTANCE.register(module);
    }

    /**
     * Вспомогательный метод регистрации команды из аддона.
     */
    public void registerCommand(Command command) {
        CommandManager.INSTANCE.register(command);
    }

    public String getName() {
        return name != null ? name : getClass().getSimpleName();
    }

    public String getVersion() {
        return version;
    }

    public String getAuthor() {
        return author;
    }

    public String getDescription() {
        return description;
    }

    public String getWebsite() {
        return website;
    }
}
