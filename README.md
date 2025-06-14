# 🎯 WebControl Coordinate Spoofer

**Simple and effective coordinate spoofing mod for Minecraft Fabric 1.21.1**

## 📊 What it does

This mod **hides your real coordinates** from other players and viewers by showing fake coordinates instead. It works using **simple coordinate offset logic**:

```
Server sends: X=150, Z=150
Mod shows you: X=50, Z=50 (offset -100)
You send to server: X=150, Z=150 (real coordinates)
```

**Result:** You're physically at X=150, Z=150, but see X=50, Z=50

## 🎮 Modes

- **🎮 VANILLA** - No spoofing, shows real coordinates
- **⚡ OFFSET** - Simple offset logic: subtract on incoming packets, add on outgoing packets

## 🔧 Usage

### Auto Setup
Mod automatically enables OFFSET mode with X=100, Z=100 offset

### Web Interface
1. Start Minecraft with the mod
2. Open browser and go to http://localhost:8080
3. Select mode and configure coordinate offset

## 📦 Installation

**Requirements:** Minecraft 1.21.1, Fabric Loader, Fabric API

1. Download the .jar file
2. Place in your Minecraft `mods` folder
3. Launch the game
4. Mod automatically starts in OFFSET mode

## 🎯 Technical Details

### What gets spoofed:
- ✅ F3 screen coordinates
- ✅ HUD coordinates
- ✅ Block positions
- ✅ Movement packets

### What doesn't get spoofed:
- ❌ Player position packets (prevents teleportation)
- ❌ Y coordinate (height unchanged)

### How it works:
```
Incoming packets (server → client): SUBTRACT offset
Outgoing packets (client → server): ADD offset
```

## 🎬 For Streamers

- Hide coordinates from viewers
- Easy setup via web interface
- Stable operation without glitches
- Use offset 1000+ for better hiding

## 🛠 Based on

CoordsSpooferExample (RusherHack plugin) with simple logic: "subtract on receive, add on send"

---

**WebControl Coordinate Spoofer** - Simple and reliable coordinate spoofing for Minecraft Fabric 1.21.1
