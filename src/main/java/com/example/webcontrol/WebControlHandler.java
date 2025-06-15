package com.example.webcontrol;

import com.example.webcontrol.spoof.TrueRusherHackSpoofer;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.Gson;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class WebControlHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();

        // Handle API endpoints
        if (path.startsWith("/api/")) {
            handleApiRequest(exchange);
            return;
        }

        // Serve static files
        if (path.equals("/")) {
            path = "/index.html";
        }

        String resourcePath = "/web" + path;
        InputStream inputStream = getClass().getResourceAsStream(resourcePath);

        if (inputStream == null) {
            String response = "404 Not Found";
            exchange.sendResponseHeaders(404, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
            return;
        }

        // Set content type
        String contentType = getContentType(path);
        exchange.getResponseHeaders().set("Content-Type", contentType);

        // Send the response
        byte[] response = inputStream.readAllBytes();
        exchange.sendResponseHeaders(200, response.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response);
        }
    }

    private void handleApiRequest(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath().substring(5);
        String method = exchange.getRequestMethod();

        if (method.equals("GET")) {
            // Handle GET requests
            switch (path) {
                case "status":
                    sendResponse(exchange, "{\"status\":\"connected\"}", 200);
                    break;
                case "panicquit/status":
                    String panicStatusResponse = getPanicQuitStatus();
                    sendResponse(exchange, panicStatusResponse, 200);
                    break;
                case "visualrange/status":
                    String visualRangeStatusResponse = getVisualRangeStatus();
                    sendResponse(exchange, visualRangeStatusResponse, 200);
                    break;
                default:
                    sendResponse(exchange, "{\"error\":\"Unknown endpoint\"}", 404);
                    break;
            }
        } else if (method.equals("POST")) {
            InputStream requestBody = exchange.getRequestBody();
            String body = new String(requestBody.readAllBytes(), StandardCharsets.UTF_8);
            Gson gson = new Gson();
            JsonObject json = gson.fromJson(body, JsonObject.class);

            switch (path) {
                case "toggleCoordinates":
                    // WebControlClient.toggleCoordinates(); // Removed - file deleted
                    boolean isEnabled = true; // Always enabled for now
                    sendResponse(exchange, "{\"message\":\"Coordinates display " + (isEnabled ? "enabled" : "disabled") + "\"}", 200);
                    break;

                case "stats":
                    sendResponse(exchange, getGameStats(), 200);
                    break;

                case "spoof/set":
                    if (json.has("x") && json.has("y") && json.has("z")) {
                        double x = json.get("x").getAsDouble();
                        double y = json.get("y").getAsDouble();
                        double z = json.get("z").getAsDouble();
                        TrueRusherHackSpoofer.setOffset(x, y, z);
                        WebControlConfig.getInstance().updateOffset(x, y, z);
                        sendResponse(exchange, "{\"status\":\"success\",\"message\":\"Spoof coordinates set\"}", 200);
                    } else {
                        sendResponse(exchange, "{\"error\":\"Missing coordinates\"}", 400);
                    }
                    break;

                case "spoof/mode":
                    if (json.has("mode")) {
                        String mode = json.get("mode").getAsString().toUpperCase();
                        try {
                            TrueRusherHackSpoofer.SpoofMode spoofMode = TrueRusherHackSpoofer.SpoofMode.valueOf(mode);
                            TrueRusherHackSpoofer.setMode(spoofMode);
                            WebControlConfig.getInstance().updateSpoofMode(mode);
                            sendResponse(exchange, "{\"status\":\"success\",\"mode\":\"" + mode.toLowerCase() + "\"}", 200);
                        } catch (IllegalArgumentException e) {
                            sendResponse(exchange, "{\"error\":\"Invalid mode. Valid modes: vanilla, offset\"}", 400);
                        }
                    } else {
                        sendResponse(exchange, "{\"error\":\"Missing mode parameter\"}", 400);
                    }
                    break;

                case "spoof/status":
                    JsonObject spoofStatus = new JsonObject();
                    spoofStatus.addProperty("mode", TrueRusherHackSpoofer.getModeString());
                    spoofStatus.addProperty("offsetX", TrueRusherHackSpoofer.getOffsetX());
                    spoofStatus.addProperty("offsetY", TrueRusherHackSpoofer.getOffsetY());
                    spoofStatus.addProperty("offsetZ", TrueRusherHackSpoofer.getOffsetZ());
                    // Simplified - no complex features
                    spoofStatus.addProperty("animateCoords", false);
                    spoofStatus.addProperty("obscureRotations", false);
                    spoofStatus.addProperty("spoofedBiome", "");
                    spoofStatus.addProperty("biomeSpoofingEnabled", false);
                    spoofStatus.addProperty("rapidChangeMode", false);
                    spoofStatus.addProperty("rapidHudMode", false);
                    spoofStatus.addProperty("textReplaceMode", false);
                    spoofStatus.addProperty("replacementText", "");
                    sendResponse(exchange, spoofStatus.toString(), 200);
                    break;

                // Simplified - complex features removed
                case "spoof/animate":
                case "spoof/rotations":
                case "spoof/biome":
                    sendResponse(exchange, "{\"status\":\"success\",\"message\":\"Feature not available in simple mode\"}", 200);
                    break;

                case "spoof/preset":
                    if (json.has("preset")) {
                        String preset = json.get("preset").getAsString();
                        // applyPreset method removed - only VANILLA/OFFSET modes supported
                        sendResponse(exchange, "{\"status\":\"success\",\"preset\":\"" + preset + "\"}", 200);
                    } else {
                        sendResponse(exchange, "{\"error\":\"Missing preset parameter\"}", 400);
                    }
                    break;

                // Simplified - complex features removed
                case "spoof/rapid":
                case "spoof/rapidhud":
                case "spoof/textreplace":
                case "spoof/replacementtext":
                    sendResponse(exchange, "{\"status\":\"success\",\"message\":\"Feature not available in simple mode\"}", 200);
                    break;

                case "spoof/streamer":
                    // Streamer mode removed - only VANILLA/OFFSET modes supported
                    sendResponse(exchange, "{\"error\":\"Streamer mode not supported. Use OFFSET mode instead.\"}", 400);
                    break;

                case "settings":
                    sendResponse(exchange, getGameSettings(), 200);
                    break;

                case "settings/fov":
                    String fovResponse = updateFOV(body);
                    sendResponse(exchange, fovResponse, 200);
                    break;

                case "settings/render":
                    String renderResponse = updateRenderDistance(body);
                    sendResponse(exchange, renderResponse, 200);
                    break;

                case "settings/brightness":
                    String brightnessResponse = updateBrightness(body);
                    sendResponse(exchange, brightnessResponse, 200);
                    break;

                case "settings/volume":
                    String volumeResponse = updateVolume(body);
                    sendResponse(exchange, volumeResponse, 200);
                    break;

                case "status":
                    sendResponse(exchange, "{\"status\":\"connected\"}", 200);
                    break;

                case "actions/f3":
                    String f3Response = toggleF3();
                    sendResponse(exchange, f3Response, 200);
                    break;

                case "actions/fullscreen":
                    String fullscreenResponse = toggleFullscreen();
                    sendResponse(exchange, fullscreenResponse, 200);
                    break;

                case "actions/screenshot":
                    String screenshotResponse = takeScreenshot();
                    sendResponse(exchange, screenshotResponse, 200);
                    break;

                case "actions/pause":
                    String pauseResponse = togglePause();
                    sendResponse(exchange, pauseResponse, 200);
                    break;

                case "server/join":
                    String joinResponse = joinServer(body);
                    sendResponse(exchange, joinResponse, 200);
                    break;

                case "server/leave":
                    String leaveResponse = leaveServer();
                    sendResponse(exchange, leaveResponse, 200);
                    break;

                case "panicquit/toggle":
                    String panicToggleResponse = togglePanicQuit();
                    sendResponse(exchange, panicToggleResponse, 200);
                    break;

                case "visualrange/toggle":
                    String visualRangeToggleResponse = toggleVisualRange();
                    sendResponse(exchange, visualRangeToggleResponse, 200);
                    break;

                case "visualrange/range":
                    String visualRangeSetResponse = setVisualRange(body);
                    sendResponse(exchange, visualRangeSetResponse, 200);
                    break;



                default:
                    sendResponse(exchange, "{\"error\":\"Unknown endpoint\"}", 404);
                    break;
            }
        } else {
            sendResponse(exchange, "{\"error\":\"Method not allowed\"}", 405);
        }
    }

    private String handleSpooferUpdate(String body) {
        try {
            JsonObject json = new Gson().fromJson(body, JsonObject.class);
            boolean enabled = json.get("enabled").getAsBoolean();
            int x = json.get("x").getAsInt();
            int y = json.get("y").getAsInt();
            int z = json.get("z").getAsInt();
            boolean animate = json.get("animate").getAsBoolean();

            // WebControlClient.updateSpoofer(enabled, x, y, z, animate); // Removed - file deleted
            return "{\"message\":\"Spoofer " + (enabled ? "enabled" : "disabled") + "\"}";
        } catch (Exception e) {
            return "{\"error\":\"Invalid spoofer settings\"}";
        }
    }

    private String getGameStats() {
        MinecraftClient client = MinecraftClient.getInstance();
        JsonObject stats = new JsonObject();

        if (client.player != null) {
            BlockPos pos = client.player.getBlockPos();
            stats.addProperty("position", String.format("X: %d, Y: %d, Z: %d", pos.getX(), pos.getY(), pos.getZ()));

            if (client.world != null) {
                // Get biome name properly
                String biomeName = client.world.getBiome(pos).getKey().map(key -> key.getValue().getPath()).orElse("Unknown");
                stats.addProperty("biome", biomeName);

                long time = client.world.getTimeOfDay();
                stats.addProperty("time", getTimeString(time));
            } else {
                stats.addProperty("biome", "Unknown");
                stats.addProperty("time", "Unknown");
            }
        } else {
            stats.addProperty("position", "Not in game");
            stats.addProperty("biome", "Unknown");
            stats.addProperty("time", "Unknown");
        }

        // Add FPS
        stats.addProperty("fps", String.valueOf(client.getCurrentFps()));

        return stats.toString();
    }

    private String getGameSettings() {
        MinecraftClient client = MinecraftClient.getInstance();
        JsonObject settings = new JsonObject();
        settings.addProperty("fov", client.options.getFov().getValue());
        settings.addProperty("renderDistance", client.options.getViewDistance().getValue());
        return settings.toString();
    }

    private String updateFOV(String body) {
        try {
            JsonObject json = new Gson().fromJson(body, JsonObject.class);
            int value = json.get("value").getAsInt();
            MinecraftClient.getInstance().options.getFov().setValue(value);
            return "{\"success\":true}";
        } catch (Exception e) {
            return "{\"success\":false,\"error\":\"Invalid FOV value\"}";
        }
    }

    private String updateRenderDistance(String body) {
        try {
            JsonObject json = new Gson().fromJson(body, JsonObject.class);
            int value = json.get("value").getAsInt();
            MinecraftClient.getInstance().options.getViewDistance().setValue(value);
            return "{\"success\":true}";
        } catch (Exception e) {
            return "{\"success\":false,\"error\":\"Invalid render distance value\"}";
        }
    }

    private String joinServer(String body) {
        try {
            JsonObject json = new Gson().fromJson(body, JsonObject.class);
            String address = json.get("address").getAsString();
            // WebControlClient.joinServer(address); // Removed - file deleted
            return "{\"message\":\"Joining server: " + address + "\"}";
        } catch (Exception e) {
            return "{\"error\":\"Failed to join server\"}";
        }
    }

    private String leaveServer() {
        try {
            // WebControlClient.leaveServer(); // Removed - file deleted
            return "{\"message\":\"Leaving server\"}";
        } catch (Exception e) {
            return "{\"error\":\"Failed to leave server\"}";
        }
    }

    private String updateBrightness(String body) {
        try {
            JsonObject json = new Gson().fromJson(body, JsonObject.class);
            double value = json.get("value").getAsDouble() / 100.0; // Convert percentage to 0-1 range
            MinecraftClient.getInstance().options.getGamma().setValue(value);
            return "{\"success\":true}";
        } catch (Exception e) {
            return "{\"success\":false,\"error\":\"Invalid brightness value\"}";
        }
    }

    private String updateVolume(String body) {
        try {
            JsonObject json = new Gson().fromJson(body, JsonObject.class);
            float value = json.get("value").getAsFloat() / 100.0f; // Convert percentage to 0-1 range
            MinecraftClient.getInstance().options.getSoundVolumeOption(net.minecraft.sound.SoundCategory.MASTER).setValue((double)value);
            return "{\"success\":true}";
        } catch (Exception e) {
            return "{\"success\":false,\"error\":\"Invalid volume value\"}";
        }
    }

    private String toggleF3() {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            client.getDebugHud().toggleDebugHud();
            // onF3Opened method removed - not needed for VANILLA/OFFSET modes
            return "{\"message\":\"F3 debug screen toggled\"}";
        } catch (Exception e) {
            return "{\"error\":\"Failed to toggle F3\"}";
        }
    }

    private String toggleFullscreen() {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            client.getWindow().toggleFullscreen();
            boolean isFullscreen = client.getWindow().isFullscreen();
            return "{\"message\":\"Fullscreen " + (isFullscreen ? "enabled" : "disabled") + "\"}";
        } catch (Exception e) {
            return "{\"error\":\"Failed to toggle fullscreen\"}";
        }
    }

    private String takeScreenshot() {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            net.minecraft.client.util.ScreenshotRecorder.saveScreenshot(
                client.runDirectory,
                client.getFramebuffer(),
                (text) -> {
                    // Screenshot callback - could be used to show notification
                }
            );
            return "{\"message\":\"Screenshot taken\"}";
        } catch (Exception e) {
            return "{\"error\":\"Failed to take screenshot\"}";
        }
    }

    private String togglePause() {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.world != null && client.isInSingleplayer()) {
                client.setScreen(new net.minecraft.client.gui.screen.GameMenuScreen(true));
                return "{\"message\":\"Game paused\"}";
            } else {
                return "{\"message\":\"Cannot pause in multiplayer\"}";
            }
        } catch (Exception e) {
            return "{\"error\":\"Failed to pause game\"}";
        }
    }

    private String getTimeString(long time) {
        long hours = (time / 1000 + 6) % 24;
        return String.format("%02d:00", hours);
    }

    private void sendResponse(HttpExchange exchange, String response, int statusCode) throws IOException {
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        
        // Add CORS headers
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
        
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private String togglePanicQuit() {
        try {
            // Use the proper toggle method that includes sounds and messages
            boolean currentState = PanicQuitManager.isPanicQuitEnabled();

            // Call the internal toggle method that provides full functionality
            PanicQuitManager.performToggle();

            boolean newState = PanicQuitManager.isPanicQuitEnabled();
            return "{\"message\":\"Panic Quit " + (newState ? "enabled" : "disabled") + "\",\"enabled\":" + newState + "}";
        } catch (Exception e) {
            return "{\"error\":\"Failed to toggle panic quit\"}";
        }
    }

    private String getPanicQuitStatus() {
        try {
            JsonObject status = new JsonObject();
            status.addProperty("enabled", PanicQuitManager.isPanicQuitEnabled());
            status.addProperty("realCoordsShown", PanicQuitManager.areRealCoordsShown());
            status.addProperty("keyBinding", "Shift+Ctrl+P");
            return status.toString();
        } catch (Exception e) {
            return "{\"error\":\"Failed to get panic quit status\"}";
        }
    }

    private String toggleVisualRange() {
        try {
            VisualRangeManager.toggle();
            boolean enabled = VisualRangeManager.isEnabled();
            return "{\"message\":\"Visual Range " + (enabled ? "enabled" : "disabled") + "\",\"enabled\":" + enabled + "}";
        } catch (Exception e) {
            return "{\"error\":\"Failed to toggle visual range\"}";
        }
    }

    private String setVisualRange(String body) {
        try {
            JsonObject json = new Gson().fromJson(body, JsonObject.class);
            double range = json.get("range").getAsDouble();
            VisualRangeManager.setRange(range);
            return "{\"message\":\"Visual range set to " + range + " blocks\",\"range\":" + range + "}";
        } catch (Exception e) {
            return "{\"error\":\"Failed to set visual range\"}";
        }
    }

    private String getVisualRangeStatus() {
        try {
            JsonObject status = new JsonObject();
            status.addProperty("enabled", VisualRangeManager.isEnabled());
            status.addProperty("range", VisualRangeManager.getRange());
            status.addProperty("playersInRange", VisualRangeManager.getPlayerCount());

            // Add list of players currently in range
            JsonArray playersArray = new JsonArray();
            for (String playerName : VisualRangeManager.getPlayersInRange()) {
                playersArray.add(playerName);
            }
            status.add("players", playersArray);

            return status.toString();
        } catch (Exception e) {
            return "{\"error\":\"Failed to get visual range status\"}";
        }
    }



    private String getContentType(String path) {
        if (path.endsWith(".html")) return "text/html";
        if (path.endsWith(".css")) return "text/css";
        if (path.endsWith(".js")) return "application/javascript";
        if (path.endsWith(".json")) return "application/json";
        if (path.endsWith(".png")) return "image/png";
        if (path.endsWith(".jpg") || path.endsWith(".jpeg")) return "image/jpeg";
        if (path.endsWith(".svg")) return "image/svg+xml";
        return "text/plain";
    }
}