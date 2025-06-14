package com.example.webcontrol;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class WebControlConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("webcontrol-config");
    private static final String CONFIG_FILE_NAME = "webcontrol.json";
    private static WebControlConfig instance;
    private static File configFile;
    
    // Panic Quit Settings
    public boolean panicQuitEnabled = false;

    // Visual Range Settings
    public boolean visualRangeEnabled = false;
    public double visualRangeDistance = 100.0;

    // Performance Settings
    public boolean performanceMonitoringEnabled = false;
    public int webServerPort = 8080;
    public boolean autoSaveEnabled = true;
    public int autoSaveInterval = 30; // seconds

    // UI Settings
    public boolean showNotifications = true;
    public boolean soundEffectsEnabled = true;
    public double hudOpacity = 1.0;
    public String hudPosition = "bottom-left";

    // Security Settings
    public boolean requireAuthentication = false;
    public String apiKey = "";
    public boolean logApiRequests = false;

    // Auto Reconnect Settings
    public boolean autoReconnectEnabled = false;
    public int autoReconnectDelay = 5;
    public int autoReconnectMaxAttempts = 10;

    // Anti-AFK Settings
    public boolean antiAFKEnabled = false;
    public int antiAFKInterval = 30;
    public String antiAFKAction = "MOUSE_MOVEMENT";

    // Auto Chat Settings
    public boolean autoChatEnabled = false;
    public Map<String, String> autoChatResponses = new HashMap<>();

    // Event Notification Settings
    public boolean eventNotificationsEnabled = false;
    public boolean notifyOnPlayerJoin = true;
    public boolean notifyOnPlayerLeave = true;
    public boolean notifyOnDeath = true;
    public boolean notifyOnMention = true;
    public boolean notifyOnPrivateMessage = true;
    public boolean notifyOnServerRestart = true;
    public boolean notifyOnLowHealth = true;
    public boolean notifyOnLowHunger = true;
    
    // Coordinate Spoofing Settings
    public String spoofMode = "VANILLA";
    public double offsetX = 0.0;
    public double offsetY = 0.0;
    public double offsetZ = 0.0;
    public boolean animateCoords = false;
    public boolean obscureRotations = false;
    public String spoofedBiome = "";
    public boolean biomeSpoofingEnabled = false;
    public boolean rapidChangeMode = false;
    public boolean rapidHudMode = false;
    public boolean textReplaceMode = false;
    public String replacementText = "";
    
    // Display Settings
    public boolean showCoordinates = true;
    
    // Game Settings
    public int fov = 70;
    public int renderDistance = 12;
    public double brightness = 1.0;
    public double volume = 1.0;
    
    private WebControlConfig() {}
    
    public static WebControlConfig getInstance() {
        if (instance == null) {
            instance = new WebControlConfig();
            initConfigFile();
            instance.load();
        }
        return instance;
    }
    
    private static void initConfigFile() {
        MinecraftClient client = MinecraftClient.getInstance();
        File gameDir = client.runDirectory;
        File configDir = new File(gameDir, "config");
        
        if (!configDir.exists()) {
            configDir.mkdirs();
        }
        
        configFile = new File(configDir, CONFIG_FILE_NAME);
    }
    
    public void load() {
        if (!configFile.exists()) {
            LOGGER.info("Config file not found, creating default configuration");
            save();
            return;
        }
        
        try (FileReader reader = new FileReader(configFile)) {
            Gson gson = new Gson();
            JsonObject json = gson.fromJson(reader, JsonObject.class);
            
            if (json == null) {
                LOGGER.warn("Config file is empty or invalid, using defaults");
                return;
            }
            
            // Load Panic Quit Settings
            if (json.has("panicQuitEnabled")) {
                panicQuitEnabled = json.get("panicQuitEnabled").getAsBoolean();
            }

            // Load Visual Range Settings
            if (json.has("visualRangeEnabled")) {
                visualRangeEnabled = json.get("visualRangeEnabled").getAsBoolean();
            }
            if (json.has("visualRangeDistance")) {
                visualRangeDistance = json.get("visualRangeDistance").getAsDouble();
            }

            // Load Auto Reconnect Settings
            if (json.has("autoReconnectEnabled")) {
                autoReconnectEnabled = json.get("autoReconnectEnabled").getAsBoolean();
            }
            if (json.has("autoReconnectDelay")) {
                autoReconnectDelay = json.get("autoReconnectDelay").getAsInt();
            }
            if (json.has("autoReconnectMaxAttempts")) {
                autoReconnectMaxAttempts = json.get("autoReconnectMaxAttempts").getAsInt();
            }

            // Load Anti-AFK Settings
            if (json.has("antiAFKEnabled")) {
                antiAFKEnabled = json.get("antiAFKEnabled").getAsBoolean();
            }
            if (json.has("antiAFKInterval")) {
                antiAFKInterval = json.get("antiAFKInterval").getAsInt();
            }
            if (json.has("antiAFKAction")) {
                antiAFKAction = json.get("antiAFKAction").getAsString();
            }

            // Load Auto Chat Settings
            if (json.has("autoChatEnabled")) {
                autoChatEnabled = json.get("autoChatEnabled").getAsBoolean();
            }
            if (json.has("autoChatResponses")) {
                // Load auto chat responses map
                JsonObject responsesObj = json.getAsJsonObject("autoChatResponses");
                autoChatResponses.clear();
                for (String key : responsesObj.keySet()) {
                    autoChatResponses.put(key, responsesObj.get(key).getAsString());
                }
            }

            // Load Event Notification Settings
            if (json.has("eventNotificationsEnabled")) {
                eventNotificationsEnabled = json.get("eventNotificationsEnabled").getAsBoolean();
            }
            if (json.has("notifyOnPlayerJoin")) {
                notifyOnPlayerJoin = json.get("notifyOnPlayerJoin").getAsBoolean();
            }
            if (json.has("notifyOnPlayerLeave")) {
                notifyOnPlayerLeave = json.get("notifyOnPlayerLeave").getAsBoolean();
            }
            if (json.has("notifyOnDeath")) {
                notifyOnDeath = json.get("notifyOnDeath").getAsBoolean();
            }
            if (json.has("notifyOnMention")) {
                notifyOnMention = json.get("notifyOnMention").getAsBoolean();
            }
            if (json.has("notifyOnPrivateMessage")) {
                notifyOnPrivateMessage = json.get("notifyOnPrivateMessage").getAsBoolean();
            }
            if (json.has("notifyOnServerRestart")) {
                notifyOnServerRestart = json.get("notifyOnServerRestart").getAsBoolean();
            }
            if (json.has("notifyOnLowHealth")) {
                notifyOnLowHealth = json.get("notifyOnLowHealth").getAsBoolean();
            }
            if (json.has("notifyOnLowHunger")) {
                notifyOnLowHunger = json.get("notifyOnLowHunger").getAsBoolean();
            }
            
            // Load Coordinate Spoofing Settings
            if (json.has("spoofMode")) {
                spoofMode = json.get("spoofMode").getAsString();
            }
            if (json.has("offsetX")) {
                offsetX = json.get("offsetX").getAsDouble();
            }
            if (json.has("offsetY")) {
                offsetY = json.get("offsetY").getAsDouble();
            }
            if (json.has("offsetZ")) {
                offsetZ = json.get("offsetZ").getAsDouble();
            }
            if (json.has("animateCoords")) {
                animateCoords = json.get("animateCoords").getAsBoolean();
            }
            if (json.has("obscureRotations")) {
                obscureRotations = json.get("obscureRotations").getAsBoolean();
            }
            if (json.has("spoofedBiome")) {
                spoofedBiome = json.get("spoofedBiome").getAsString();
            }
            if (json.has("biomeSpoofingEnabled")) {
                biomeSpoofingEnabled = json.get("biomeSpoofingEnabled").getAsBoolean();
            }
            if (json.has("rapidChangeMode")) {
                rapidChangeMode = json.get("rapidChangeMode").getAsBoolean();
            }
            if (json.has("rapidHudMode")) {
                rapidHudMode = json.get("rapidHudMode").getAsBoolean();
            }
            if (json.has("textReplaceMode")) {
                textReplaceMode = json.get("textReplaceMode").getAsBoolean();
            }
            if (json.has("replacementText")) {
                replacementText = json.get("replacementText").getAsString();
            }
            
            // Load Display Settings
            if (json.has("showCoordinates")) {
                showCoordinates = json.get("showCoordinates").getAsBoolean();
            }
            
            // Load Game Settings
            if (json.has("fov")) {
                fov = json.get("fov").getAsInt();
            }
            if (json.has("renderDistance")) {
                renderDistance = json.get("renderDistance").getAsInt();
            }
            if (json.has("brightness")) {
                brightness = json.get("brightness").getAsDouble();
            }
            if (json.has("volume")) {
                volume = json.get("volume").getAsDouble();
            }
            
            LOGGER.info("Configuration loaded successfully");
            
        } catch (IOException e) {
            LOGGER.error("Failed to load configuration: " + e.getMessage());
        } catch (Exception e) {
            LOGGER.error("Error parsing configuration file: " + e.getMessage());
        }
    }
    
    public void save() {
        try {
            JsonObject json = new JsonObject();
            
            // Save Panic Quit Settings
            json.addProperty("panicQuitEnabled", panicQuitEnabled);

            // Save Visual Range Settings
            json.addProperty("visualRangeEnabled", visualRangeEnabled);
            json.addProperty("visualRangeDistance", visualRangeDistance);

            // Save Auto Reconnect Settings
            json.addProperty("autoReconnectEnabled", autoReconnectEnabled);
            json.addProperty("autoReconnectDelay", autoReconnectDelay);
            json.addProperty("autoReconnectMaxAttempts", autoReconnectMaxAttempts);

            // Save Anti-AFK Settings
            json.addProperty("antiAFKEnabled", antiAFKEnabled);
            json.addProperty("antiAFKInterval", antiAFKInterval);
            json.addProperty("antiAFKAction", antiAFKAction);

            // Save Auto Chat Settings
            json.addProperty("autoChatEnabled", autoChatEnabled);
            JsonObject responsesObj = new JsonObject();
            for (Map.Entry<String, String> entry : autoChatResponses.entrySet()) {
                responsesObj.addProperty(entry.getKey(), entry.getValue());
            }
            json.add("autoChatResponses", responsesObj);

            // Save Event Notification Settings
            json.addProperty("eventNotificationsEnabled", eventNotificationsEnabled);
            json.addProperty("notifyOnPlayerJoin", notifyOnPlayerJoin);
            json.addProperty("notifyOnPlayerLeave", notifyOnPlayerLeave);
            json.addProperty("notifyOnDeath", notifyOnDeath);
            json.addProperty("notifyOnMention", notifyOnMention);
            json.addProperty("notifyOnPrivateMessage", notifyOnPrivateMessage);
            json.addProperty("notifyOnServerRestart", notifyOnServerRestart);
            json.addProperty("notifyOnLowHealth", notifyOnLowHealth);
            json.addProperty("notifyOnLowHunger", notifyOnLowHunger);
            
            // Save Coordinate Spoofing Settings
            json.addProperty("spoofMode", spoofMode);
            json.addProperty("offsetX", offsetX);
            json.addProperty("offsetY", offsetY);
            json.addProperty("offsetZ", offsetZ);
            json.addProperty("animateCoords", animateCoords);
            json.addProperty("obscureRotations", obscureRotations);
            json.addProperty("spoofedBiome", spoofedBiome);
            json.addProperty("biomeSpoofingEnabled", biomeSpoofingEnabled);
            json.addProperty("rapidChangeMode", rapidChangeMode);
            json.addProperty("rapidHudMode", rapidHudMode);
            json.addProperty("textReplaceMode", textReplaceMode);
            json.addProperty("replacementText", replacementText);
            
            // Save Display Settings
            json.addProperty("showCoordinates", showCoordinates);
            
            // Save Game Settings
            json.addProperty("fov", fov);
            json.addProperty("renderDistance", renderDistance);
            json.addProperty("brightness", brightness);
            json.addProperty("volume", volume);
            
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            
            try (FileWriter writer = new FileWriter(configFile)) {
                gson.toJson(json, writer);
                LOGGER.info("Configuration saved successfully");
            }
            
        } catch (IOException e) {
            LOGGER.error("Failed to save configuration: " + e.getMessage());
        }
    }
    
    // Utility methods for updating specific settings
    public void updatePanicQuit(boolean enabled) {
        this.panicQuitEnabled = enabled;
        save();
    }

    public void updateVisualRange(boolean enabled, double distance) {
        this.visualRangeEnabled = enabled;
        this.visualRangeDistance = distance;
        save();
    }

    public void updateAutoReconnect(boolean enabled, int delay, int maxAttempts) {
        this.autoReconnectEnabled = enabled;
        this.autoReconnectDelay = delay;
        this.autoReconnectMaxAttempts = maxAttempts;
        save();
    }

    public void updateAntiAFK(boolean enabled, int interval, Object action) {
        this.antiAFKEnabled = enabled;
        this.antiAFKInterval = interval;
        this.antiAFKAction = action.toString();
        save();
    }

    public void updateAutoChat(boolean enabled, Map<String, String> responses) {
        this.autoChatEnabled = enabled;
        this.autoChatResponses = new HashMap<>(responses);
        save();
    }

    public void updateEventNotifications(boolean enabled, boolean playerJoin, boolean playerLeave,
                                       boolean death, boolean mention, boolean privateMessage,
                                       boolean serverRestart, boolean lowHealth, boolean lowHunger) {
        this.eventNotificationsEnabled = enabled;
        this.notifyOnPlayerJoin = playerJoin;
        this.notifyOnPlayerLeave = playerLeave;
        this.notifyOnDeath = death;
        this.notifyOnMention = mention;
        this.notifyOnPrivateMessage = privateMessage;
        this.notifyOnServerRestart = serverRestart;
        this.notifyOnLowHealth = lowHealth;
        this.notifyOnLowHunger = lowHunger;
        save();
    }
    
    public void updateSpoofMode(String mode) {
        this.spoofMode = mode;
        save();
    }
    
    public void updateOffset(double x, double y, double z) {
        this.offsetX = x;
        this.offsetY = y;
        this.offsetZ = z;
        save();
    }
    
    public void updateAnimateCoords(boolean animate) {
        this.animateCoords = animate;
        save();
    }
    
    public void updateObscureRotations(boolean obscure) {
        this.obscureRotations = obscure;
        save();
    }
    
    public void updateBiomeSpoof(String biome, boolean enabled) {
        this.spoofedBiome = biome;
        this.biomeSpoofingEnabled = enabled;
        save();
    }
    
    public void updateRapidChangeMode(boolean enabled) {
        this.rapidChangeMode = enabled;
        save();
    }
    
    public void updateRapidHudMode(boolean enabled) {
        this.rapidHudMode = enabled;
        save();
    }
    
    public void updateTextReplaceMode(boolean enabled, String text) {
        this.textReplaceMode = enabled;
        this.replacementText = text;
        save();
    }
    
    public void updateShowCoordinates(boolean show) {
        this.showCoordinates = show;
        save();
    }
    
    public void updateGameSettings(int fov, int renderDistance, double brightness, double volume) {
        this.fov = fov;
        this.renderDistance = renderDistance;
        this.brightness = brightness;
        this.volume = volume;
        save();
    }
    
    // Getters for all settings
    public boolean isPanicQuitEnabled() { return panicQuitEnabled; }
    public boolean isVisualRangeEnabled() { return visualRangeEnabled; }
    public double getVisualRangeDistance() { return visualRangeDistance; }

    // Auto Reconnect getters
    public boolean isAutoReconnectEnabled() { return autoReconnectEnabled; }
    public int getAutoReconnectDelay() { return autoReconnectDelay; }
    public int getAutoReconnectMaxAttempts() { return autoReconnectMaxAttempts; }

    // Anti-AFK getters
    public boolean isAntiAFKEnabled() { return antiAFKEnabled; }
    public int getAntiAFKInterval() { return antiAFKInterval; }
    public Object getAntiAFKAction() {
        return antiAFKAction; // Возвращаем строку, преобразование будет в AntiAFKManager
    }

    // Auto Chat getters
    public boolean isAutoChatEnabled() { return autoChatEnabled; }
    public Map<String, String> getAutoChatResponses() { return new HashMap<>(autoChatResponses); }

    // Event Notifications getters
    public boolean isEventNotificationsEnabled() { return eventNotificationsEnabled; }
    public boolean isNotifyOnPlayerJoin() { return notifyOnPlayerJoin; }
    public boolean isNotifyOnPlayerLeave() { return notifyOnPlayerLeave; }
    public boolean isNotifyOnDeath() { return notifyOnDeath; }
    public boolean isNotifyOnMention() { return notifyOnMention; }
    public boolean isNotifyOnPrivateMessage() { return notifyOnPrivateMessage; }
    public boolean isNotifyOnServerRestart() { return notifyOnServerRestart; }
    public boolean isNotifyOnLowHealth() { return notifyOnLowHealth; }
    public boolean isNotifyOnLowHunger() { return notifyOnLowHunger; }
    public String getSpoofMode() { return spoofMode; }
    public double getOffsetX() { return offsetX; }
    public double getOffsetY() { return offsetY; }
    public double getOffsetZ() { return offsetZ; }
    public boolean isAnimateCoords() { return animateCoords; }
    public boolean isObscureRotations() { return obscureRotations; }
    public String getSpoofedBiome() { return spoofedBiome; }
    public boolean isBiomeSpoofingEnabled() { return biomeSpoofingEnabled; }
    public boolean isRapidChangeMode() { return rapidChangeMode; }
    public boolean isRapidHudMode() { return rapidHudMode; }
    public boolean isTextReplaceMode() { return textReplaceMode; }
    public String getReplacementText() { return replacementText; }
    public boolean isShowCoordinates() { return showCoordinates; }
    public int getFov() { return fov; }
    public int getRenderDistance() { return renderDistance; }
    public double getBrightness() { return brightness; }
    public double getVolume() { return volume; }
}