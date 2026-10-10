package me.collective.module.client;

import me.collective.category.Category;
import me.collective.mcp.McpServer;
import me.collective.module.Module;
import me.collective.setting.NumberSetting;

public class McpModule extends Module {
    public static McpModule INSTANCE;
    public final NumberSetting port = add(new NumberSetting("Port", 21114, 1024, 65535, 1));
    private McpServer server;

    public McpModule() {
        super("McpServer", "Model Context Protocol сервер для управления клиентом AI-агентами и нейросетями.", Category.CLIENT);
        INSTANCE = this;
    }

    @Override
    public void onEnable() {
        try {
            server = new McpServer(port.getIntValue());
            server.start();
            info("MCP Server запущен на http://127.0.0.1:%d", port.getIntValue());
        } catch (Exception e) {
            error("Ошибка запуска MCP Server: " + e.getMessage());
            setToggled(false);
        }
    }

    @Override
    public void onDisable() {
        if (server != null) {
            server.stop();
            server = null;
            info("MCP Server остановлен.");
        }
    }

    public McpServer getServer() {
        return server;
    }
}
