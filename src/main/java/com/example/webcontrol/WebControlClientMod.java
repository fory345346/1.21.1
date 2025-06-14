package com.example.webcontrol;

import com.example.webcontrol.keybinds.KeybindManager;
import com.example.webcontrol.spoof.CoordSpoofManager;
import com.sun.net.httpserver.HttpServer;
import net.fabricmc.api.ClientModInitializer;
import java.io.IOException;
import java.net.InetSocketAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.network.ServerInfo.ServerType;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.CookieStorage;
import net.minecraft.util.Identifier;
import java.util.HashMap;

public class WebControlClientMod implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("webcontrol");
    private static HttpServer webServer;

    @Override
    public void onInitializeClient() {
        LOGGER.info("WebControl Mod initializing...");

        // Initialize configuration system first
        WebControlConfig config = WebControlConfig.getInstance();
        LOGGER.info("Configuration system initialized");

        // Initialize core systems
        KeybindManager.initialize();

        // Initialize web interface
        startWebServer();

        // Initialize feature systems
        PanicQuitManager.initialize();
        VisualRangeManager.initialize();

        // Set up coordinate spoofing with simple offset logic
        // OFFSET mode: server->client subtract, client->server add
        // Example: offset=100 means server coords 150,150 → client sees 50,50
        CoordSpoofManager.setMode(CoordSpoofManager.SpoofMode.OFFSET);
        CoordSpoofManager.setOffset(100, 0, 100); // X offset=100, Z offset=100

        LOGGER.info("OFFSET mode enabled: server->client subtract, client->server add (offset X=100, Z=100)");

        // Apply saved settings from config
        applySavedSettings(config);
        VisualRangeManager.loadFromConfig(config);

        LOGGER.info("WebControl Mod initialization complete with enhanced features");
    }

    private static void startWebServer() {
        try {
            webServer = HttpServer.create(new InetSocketAddress("0.0.0.0", 8080), 0);
            webServer.createContext("/", new WebControlHandler());
            webServer.setExecutor(null);
            webServer.start();
            LOGGER.info("Web interface started at http://localhost:8080");
        } catch (IOException e) {
            LOGGER.error("Failed to start web interface: " + e.getMessage());
        }
    }
    
    private static void applySavedSettings(WebControlConfig config) {
        try {
            // Apply coordinate spoofing settings
            com.example.webcontrol.spoof.CoordSpoofManager.SpoofMode mode = 
                com.example.webcontrol.spoof.CoordSpoofManager.SpoofMode.valueOf(config.getSpoofMode());
            com.example.webcontrol.spoof.CoordSpoofManager.setMode(mode);
            com.example.webcontrol.spoof.CoordSpoofManager.setOffset(config.getOffsetX(), config.getOffsetY(), config.getOffsetZ());
            com.example.webcontrol.spoof.CoordSpoofManager.setAnimateCoords(config.isAnimateCoords());
            com.example.webcontrol.spoof.CoordSpoofManager.setObscureRotations(config.isObscureRotations());
            com.example.webcontrol.spoof.CoordSpoofManager.setBiomeSpoof(config.getSpoofedBiome());
            com.example.webcontrol.spoof.CoordSpoofManager.setRapidChangeMode(config.isRapidChangeMode());
            com.example.webcontrol.spoof.CoordSpoofManager.setRapidHudMode(config.isRapidHudMode());
            com.example.webcontrol.spoof.CoordSpoofManager.setTextReplaceMode(config.isTextReplaceMode());
            com.example.webcontrol.spoof.CoordSpoofManager.setReplacementText(config.getReplacementText());
            
            LOGGER.info("Applied saved coordinate spoofing settings");
        } catch (Exception e) {
            LOGGER.error("Failed to apply saved settings: " + e.getMessage());
        }
    }

    public static void joinServer(String address) {
        MinecraftClient client = MinecraftClient.getInstance();
        ServerInfo serverInfo = new ServerInfo("Server", address, ServerType.OTHER);
        ServerAddress serverAddress = ServerAddress.parse(address);
        
        client.execute(() -> {
            Screen screen = new TitleScreen();
            ConnectScreen.connect(screen, client, serverAddress, serverInfo, false, 
                new CookieStorage(new HashMap<>()));
        });
    }
}