package me.collective.mcp;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import me.collective.command.CommandManager;
import me.collective.module.Module;
import me.collective.module.ModuleManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

public class McpServer {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private HttpServer server;
    private final int port;
    private boolean running;

    public McpServer(int port) {
        this.port = port;
    }

    public synchronized void start() throws IOException {
        if (running) return;
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        server.createContext("/", new McpHttpHandler());
        server.createContext("/mcp", new McpHttpHandler());
        server.setExecutor(Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "Collective-MCP-Worker");
            t.setDaemon(true);
            return t;
        }));
        server.start();
        running = true;
        System.out.println("[Collective-MCP] Server listening on http://127.0.0.1:" + port);
    }

    public synchronized void stop() {
        if (!running) return;
        if (server != null) {
            server.stop(1);
            server = null;
        }
        running = false;
        System.out.println("[Collective-MCP] Server stopped.");
    }

    public boolean isRunning() {
        return running;
    }

    public int getPort() {
        return port;
    }

    private class McpHttpHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");

            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                JsonObject manifest = getMcpManifest();
                byte[] bytes = GSON.toJson(manifest).getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
                return;
            }

            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                JsonObject responseJson;
                try {
                    JsonObject requestJson = GSON.fromJson(body, JsonObject.class);
                    responseJson = handleRpc(requestJson);
                } catch (Exception e) {
                    responseJson = new JsonObject();
                    responseJson.addProperty("jsonrpc", "2.0");
                    JsonObject err = new JsonObject();
                    err.addProperty("code", -32700);
                    err.addProperty("message", "Parse error: " + e.getMessage());
                    responseJson.add("error", err);
                }

                byte[] bytes = GSON.toJson(responseJson).getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            }
        }

        private JsonObject handleRpc(JsonObject req) {
            JsonObject res = new JsonObject();
            res.addProperty("jsonrpc", "2.0");
            if (req.has("id")) res.add("id", req.get("id"));

            String method = req.has("method") ? req.get("method").getAsString() : "";
            JsonObject params = req.has("params") && req.get("params").isJsonObject() ? req.getAsJsonObject("params") : new JsonObject();

            switch (method) {
                case "initialize" -> {
                    JsonObject result = new JsonObject();
                    result.addProperty("protocolVersion", "2024-11-05");
                    JsonObject serverInfo = new JsonObject();
                    serverInfo.addProperty("name", "collective-client-mcp");
                    serverInfo.addProperty("version", "1.0.0");
                    result.add("serverInfo", serverInfo);

                    JsonObject capabilities = new JsonObject();
                    capabilities.add("tools", new JsonObject());
                    capabilities.add("resources", new JsonObject());
                    result.add("capabilities", capabilities);

                    res.add("result", result);
                }
                case "tools/list" -> {
                    JsonObject result = new JsonObject();
                    result.add("tools", getToolsArray());
                    res.add("result", result);
                }
                case "tools/call" -> {
                    String toolName = params.has("name") ? params.get("name").getAsString() : "";
                    JsonObject args = params.has("arguments") && params.get("arguments").isJsonObject() ? params.getAsJsonObject("arguments") : new JsonObject();
                    JsonObject callResult = executeTool(toolName, args);
                    res.add("result", callResult);
                }
                default -> {
                    JsonObject err = new JsonObject();
                    err.addProperty("code", -32601);
                    err.addProperty("message", "Method not found: " + method);
                    res.add("error", err);
                }
            }

            return res;
        }

        private JsonObject getMcpManifest() {
            JsonObject obj = new JsonObject();
            obj.addProperty("name", "Collective Client MCP Server");
            obj.addProperty("version", "1.0.0");
            obj.addProperty("description", "Direct Model Context Protocol bridge for AI agents into Minecraft via Collective Client");
            obj.add("tools", getToolsArray());
            return obj;
        }

        private JsonArray getToolsArray() {
            JsonArray tools = new JsonArray();

            // 1. get_player_state
            JsonObject tState = new JsonObject();
            tState.addProperty("name", "get_player_state");
            tState.addProperty("description", "Returns detailed player status (coords, health, hunger, inventory, active server, dimension).");
            JsonObject tStateSchema = new JsonObject();
            tStateSchema.addProperty("type", "object");
            tState.add("inputSchema", tStateSchema);
            tools.add(tState);

            // 2. list_modules
            JsonObject tMods = new JsonObject();
            tMods.addProperty("name", "list_modules");
            tMods.addProperty("description", "Lists all available Collective Client modules and their current toggled state.");
            JsonObject tModsSchema = new JsonObject();
            tModsSchema.addProperty("type", "object");
            tMods.add("inputSchema", tModsSchema);
            tools.add(tMods);

            // 3. toggle_module
            JsonObject tToggle = new JsonObject();
            tToggle.addProperty("name", "toggle_module");
            tToggle.addProperty("description", "Toggles or sets the enabled status of a specific module.");
            JsonObject tToggleSchema = new JsonObject();
            tToggleSchema.addProperty("type", "object");
            JsonObject props = new JsonObject();
            JsonObject pName = new JsonObject();
            pName.addProperty("type", "string");
            pName.addProperty("description", "Module name (case-insensitive)");
            props.add("name", pName);
            JsonObject pState = new JsonObject();
            pState.addProperty("type", "boolean");
            pState.addProperty("description", "Optional desired state (true=enabled, false=disabled)");
            props.add("enabled", pState);
            tToggleSchema.add("properties", props);
            JsonArray req = new JsonArray();
            req.add("name");
            tToggleSchema.add("required", req);
            tToggle.add("inputSchema", tToggleSchema);
            tools.add(tToggle);

            // 4. execute_command
            JsonObject tCmd = new JsonObject();
            tCmd.addProperty("name", "execute_command");
            tCmd.addProperty("description", "Executes an internal client command (e.g. .elytrafly 1000 2000, .bind, .toggle).");
            JsonObject tCmdSchema = new JsonObject();
            tCmdSchema.addProperty("type", "object");
            JsonObject cmdProps = new JsonObject();
            JsonObject pCmd = new JsonObject();
            pCmd.addProperty("type", "string");
            pCmd.addProperty("description", "Command line string to execute (e.g. .elytrafly 5000 5000)");
            cmdProps.add("command", pCmd);
            tCmdSchema.add("properties", cmdProps);
            JsonArray cmdReq = new JsonArray();
            cmdReq.add("command");
            tCmdSchema.add("required", cmdReq);
            tCmd.add("inputSchema", tCmdSchema);
            tools.add(tCmd);

            // 5. chat_message
            JsonObject tChat = new JsonObject();
            tChat.addProperty("name", "chat_message");
            tChat.addProperty("description", "Sends a chat message or server command to the current Minecraft server.");
            JsonObject tChatSchema = new JsonObject();
            tChatSchema.addProperty("type", "object");
            JsonObject chatProps = new JsonObject();
            JsonObject pMsg = new JsonObject();
            pMsg.addProperty("type", "string");
            pMsg.addProperty("description", "Message string to send");
            chatProps.add("message", pMsg);
            tChatSchema.add("properties", chatProps);
            JsonArray chatReq = new JsonArray();
            chatReq.add("message");
            tChatSchema.add("required", chatReq);
            tChat.add("inputSchema", tChatSchema);
            tools.add(tChat);

            // 6. baritone_path
            JsonObject tBari = new JsonObject();
            tBari.addProperty("name", "baritone_path");
            tBari.addProperty("description", "Commands Baritone AI to pathfind to specific X Y Z coordinates or cancel current path.");
            JsonObject tBariSchema = new JsonObject();
            tBariSchema.addProperty("type", "object");
            JsonObject bariProps = new JsonObject();
            JsonObject pBx = new JsonObject();
            pBx.addProperty("type", "integer");
            pBx.addProperty("description", "Target X coordinate");
            bariProps.add("x", pBx);
            JsonObject pBy = new JsonObject();
            pBy.addProperty("type", "integer");
            pBy.addProperty("description", "Target Y coordinate (optional, default 64)");
            bariProps.add("y", pBy);
            JsonObject pBz = new JsonObject();
            pBz.addProperty("type", "integer");
            pBz.addProperty("description", "Target Z coordinate");
            bariProps.add("z", pBz);
            JsonObject pBstop = new JsonObject();
            pBstop.addProperty("type", "boolean");
            pBstop.addProperty("description", "Set to true to stop/cancel Baritone pathfinding");
            bariProps.add("stop", pBstop);
            tBariSchema.add("properties", bariProps);
            tBari.add("inputSchema", tBariSchema);
            tools.add(tBari);

            // 7. set_rotation
            JsonObject tRot = new JsonObject();
            tRot.addProperty("name", "set_rotation");
            tRot.addProperty("description", "Smoothly or silently sets player yaw and pitch angles.");
            JsonObject tRotSchema = new JsonObject();
            tRotSchema.addProperty("type", "object");
            JsonObject rotProps = new JsonObject();
            JsonObject pYaw = new JsonObject();
            pYaw.addProperty("type", "number");
            pYaw.addProperty("description", "Target yaw angle");
            rotProps.add("yaw", pYaw);
            JsonObject pPitch = new JsonObject();
            pPitch.addProperty("type", "number");
            pPitch.addProperty("description", "Target pitch angle (-90 up to 90 down)");
            rotProps.add("pitch", pPitch);
            tRotSchema.add("properties", rotProps);
            JsonArray rotReq = new JsonArray();
            rotReq.add("yaw");
            rotReq.add("pitch");
            tRotSchema.add("required", rotReq);
            tRot.add("inputSchema", tRotSchema);
            tools.add(tRot);

            // 6. get_world_entities
            JsonObject tEnt = new JsonObject();
            tEnt.addProperty("name", "get_world_entities");
            tEnt.addProperty("description", "Returns nearby entities (players, mobs, items) with distance and coordinates.");
            JsonObject tEntSchema = new JsonObject();
            tEntSchema.addProperty("type", "object");
            JsonObject entProps = new JsonObject();
            JsonObject pRad = new JsonObject();
            pRad.addProperty("type", "number");
            pRad.addProperty("description", "Radius in blocks (default 32)");
            entProps.add("radius", pRad);
            tEntSchema.add("properties", entProps);
            tEnt.add("inputSchema", tEntSchema);
            tools.add(tEnt);

            return tools;
        }

        private JsonObject executeTool(String name, JsonObject args) {
            JsonObject content = new JsonObject();
            content.addProperty("type", "text");
            MinecraftClient mc = MinecraftClient.getInstance();

            switch (name) {
                case "get_player_state" -> {
                    JsonObject state = new JsonObject();
                    if (mc.player == null) {
                        state.addProperty("inGame", false);
                        state.addProperty("screen", mc.currentScreen != null ? mc.currentScreen.getClass().getSimpleName() : "None");
                    } else {
                        state.addProperty("inGame", true);
                        state.addProperty("name", mc.player.getName().getString());
                        state.addProperty("health", mc.player.getHealth());
                        state.addProperty("hunger", mc.player.getHungerManager().getFoodLevel());
                        state.addProperty("x", mc.player.getX());
                        state.addProperty("y", mc.player.getY());
                        state.addProperty("z", mc.player.getZ());
                        state.addProperty("yaw", mc.player.getYaw());
                        state.addProperty("pitch", mc.player.getPitch());
                        state.addProperty("dimension", mc.world != null ? mc.world.getRegistryKey().getValue().toString() : "unknown");
                        state.addProperty("server", mc.getCurrentServerEntry() != null ? mc.getCurrentServerEntry().address : (mc.isInSingleplayer() ? "Singleplayer" : "None"));

                        JsonArray inv = new JsonArray();
                        for (int i = 0; i < mc.player.getInventory().size(); i++) {
                            ItemStack is = mc.player.getInventory().getStack(i);
                            if (!is.isEmpty()) {
                                JsonObject it = new JsonObject();
                                it.addProperty("slot", i);
                                it.addProperty("item", is.getItem().toString());
                                it.addProperty("count", is.getCount());
                                inv.add(it);
                            }
                        }
                        state.add("inventory", inv);
                    }
                    content.addProperty("text", GSON.toJson(state));
                }
                case "list_modules" -> {
                    JsonArray arr = new JsonArray();
                    for (Module m : ModuleManager.INSTANCE.getModules()) {
                        JsonObject mObj = new JsonObject();
                        mObj.addProperty("name", m.getName());
                        mObj.addProperty("category", m.getCategory().getName());
                        mObj.addProperty("enabled", m.isToggled());
                        mObj.addProperty("description", m.getDescription());
                        arr.add(mObj);
                    }
                    content.addProperty("text", GSON.toJson(arr));
                }
                case "toggle_module" -> {
                    String modName = args.has("name") ? args.get("name").getAsString() : "";
                    Module target = ModuleManager.INSTANCE.getModule(modName);
                    if (target == null) {
                        content.addProperty("text", "Module '" + modName + "' not found.");
                    } else {
                        boolean newState = args.has("enabled") ? args.get("enabled").getAsBoolean() : !target.isToggled();
                        mc.execute(() -> target.setToggled(newState));
                        content.addProperty("text", "Module '" + target.getName() + "' is now " + (newState ? "ENABLED" : "DISABLED"));
                    }
                }
                case "execute_command" -> {
                    String cmd = args.has("command") ? args.get("command").getAsString() : "";
                    mc.execute(() -> CommandManager.INSTANCE.execute(cmd));
                    content.addProperty("text", "Executed command: " + cmd);
                }
                case "chat_message" -> {
                    String msg = args.has("message") ? args.get("message").getAsString() : "";
                    mc.execute(() -> {
                        if (mc.getNetworkHandler() != null) {
                            if (msg.startsWith("/")) {
                                mc.getNetworkHandler().sendChatCommand(msg.substring(1));
                            } else {
                                mc.getNetworkHandler().sendChatMessage(msg);
                            }
                        }
                    });
                    content.addProperty("text", "Sent chat message: " + msg);
                }
                case "baritone_path" -> {
                    boolean stop = args.has("stop") && args.get("stop").getAsBoolean();
                    if (stop) {
                        mc.execute(() -> me.collective.util.BaritoneBridge.clearGoal());
                        content.addProperty("text", "Baritone pathfinding cancelled.");
                    } else {
                        int x = args.has("x") ? args.get("x").getAsInt() : 0;
                        int y = args.has("y") ? args.get("y").getAsInt() : (mc.player != null ? mc.player.getBlockY() : 64);
                        int z = args.has("z") ? args.get("z").getAsInt() : 0;
                        mc.execute(() -> me.collective.util.BaritoneBridge.setGoal(new net.minecraft.util.math.BlockPos(x, y, z)));
                        content.addProperty("text", "Baritone pathfinding to " + x + ", " + y + ", " + z);
                    }
                }
                case "set_rotation" -> {
                    float yaw = args.has("yaw") ? args.get("yaw").getAsFloat() : 0f;
                    float pitch = args.has("pitch") ? args.get("pitch").getAsFloat() : 0f;
                    mc.execute(() -> {
                        me.collective.util.RotationUtils.getInstance().setRotationSilent(yaw, pitch);
                        if (mc.player != null) {
                            mc.player.setYaw(yaw);
                            mc.player.setPitch(pitch);
                        }
                    });
                    content.addProperty("text", "Rotation set to yaw=" + yaw + ", pitch=" + pitch);
                }
                case "get_world_entities" -> {
                    double r = args.has("radius") ? args.get("radius").getAsDouble() : 32.0;
                    JsonArray entitiesArr = new JsonArray();
                    if (mc.world != null && mc.player != null) {
                        for (Entity e : mc.world.getEntities()) {
                            if (e == mc.player) continue;
                            double dist = mc.player.distanceTo(e);
                            if (dist <= r) {
                                JsonObject eObj = new JsonObject();
                                eObj.addProperty("name", e.getName().getString());
                                eObj.addProperty("type", e.getType().toString());
                                eObj.addProperty("distance", Math.round(dist * 100.0) / 100.0);
                                eObj.addProperty("x", e.getX());
                                eObj.addProperty("y", e.getY());
                                eObj.addProperty("z", e.getZ());
                                if (e instanceof PlayerEntity pe) {
                                    eObj.addProperty("health", pe.getHealth());
                                }
                                entitiesArr.add(eObj);
                            }
                        }
                    }
                    content.addProperty("text", GSON.toJson(entitiesArr));
                }
                default -> content.addProperty("text", "Unknown tool: " + name);
            }

            JsonObject res = new JsonObject();
            JsonArray cArr = new JsonArray();
            cArr.add(content);
            res.add("content", cArr);
            return res;
        }
    }
}
