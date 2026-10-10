# AGENTS.md — Developer & AI Agent Technical Guide

This document provides architectural details, conventions, and guidelines for AI coding agents and human developers maintaining and extending **Collective Client** (Minecraft 1.21.11 Fabric).

---

## 1. Project Overview

Collective Client is an open-source utility client focused on a 1:1 pixel-perfect port of OpenMio's ClickGUI, HUD system, custom font engine (CFont), and OpenGL blur shaders to Minecraft 1.21.11.

- **Target Version**: Minecraft `1.21.11`
- **Fabric Loader**: `>=0.16` (Current: `0.19.5`)
- **Fabric Loom**: `1.17.21`
- **Yarn Mappings**: `1.21.11+build.6`
- **Java Version**: `Java 21`

---

## 2. Package Architecture

All source code is located under `src/main/java/me/collective/`:

```
me.collective/
├── CollectiveClient.java       # Mod initializer, HUD render event hooks, key handling
├── category/
│   └── Category.java           # Category enum (CLIENT, RENDER, HUD, etc.)
├── command/                    # In-game chat command system (.toggle, .bind, etc.)
├── setting/
│   ├── Setting.java            # Abstract base setting class with change listeners and visibility
│   ├── BooleanSetting.java     # Checkbox / toggle
│   ├── NumberSetting.java      # Double/Int slider with min, max, inc
│   ├── EnumSetting.java        # Enum selector
│   ├── ColorSetting.java       # RGBA color with rainbow / alpha support
│   ├── StringSetting.java      # Text input
│   └── KeybindSetting.java     # GLFW keybind binding
├── module/
│   ├── Module.java             # Base module class with settings registry and keybinds
│   ├── ModuleManager.java      # Module registry, category grouping, key listener
│   ├── client/                 # Client modules: UI, HUD, Fonts, Colors, Notifications, AntiCheat, Discord, Baritone
│   ├── render/                 # Blur module (frosted glass background shader)
│   └── hud/                    # All 17 HUD modules + HudModule base class
├── render/
│   ├── RenderUtil.java         # DrawContext 2D helpers (drawRect, drawOutline, gradient)
│   ├── ColorUtil.java          # Color conversions and rainbow generator
│   ├── SoundUtil.java          # Audio playback (click, hover)
│   ├── ScissorUtil.java        # Scissor stack utility
│   └── BlurEngine.java         # Two-pass GLSL Gaussian blur engine with glScissor clipping
├── font/
│   ├── CFont.java              # NativeImageBackedTexture glyph atlas generator with Cyrillic support
│   └── FontManager.java        # Singleton delegating between CFont and Minecraft TextRenderer
├── gui/
│   ├── ClickGuiScreen.java     # Main ClickGUI screen with search, snow, blur, mouse scroll, panels
│   ├── component/              # GUI components: Panel, ModuleButton, SliderComponent, etc.
│   └── hud/
│       └── HudEditorScreen.java # Drag-and-drop HUD layout editor
└── mixin/
    ├── MixinKeyboard.java      # Global key press injector
    ├── MixinChatScreen.java    # Chat screen command execution injector
    └── MixinChatInputSuggestor.java # Command and module tab-completion
```

---

## 3. Active Modules State

In accordance with user preferences, only the clean visual/client foundation modules are active:
- **Client Tab**:
  - `UI`: ClickGUI dimensions, colors, sounds, and animations.
  - `HUD`: Global HUD visibility, colors, rainbow gradient, and HUD Editor trigger.
  - `Fonts`: Custom CFont engine toggle and configuration.
  - `Colors`: Global color theme and client palette.
  - `Notifications`: In-game popups and status messages.
  - `AntiCheat`: Server anti-cheat mode profile flags.
  - `Discord`: Discord Rich Presence.
  - `Baritone`: Pathfinding bridge integration.
- **Render Tab**:
  - `Blur`: Real-time OpenGL Gaussian frosted glass shader for ClickGUI and HUD Editor.
- **HUD Tab**:
  - 17 customizable drag-and-drop HUD elements (`Watermark`, `ModuleList`, `Armor`, `Position`, `Direction`, `Metrics`, `Inventory`, `Effects`, `Totems`, `PlayerModel`, `TextRadar`, `Welcomer`, `Lag`, `Graph`, `Chat`, `Music`, `Crypto`).

All non-working legacy cheat modules from Combat, Movement, Player, Exploit, and Misc have been removed.

---

## 4. Minecraft 1.21.11 API Breaking Changes & Solutions

When working with Minecraft 1.21.11, be aware of the following breaking changes compared to 1.20.x:

### A. Screen Input Events
- **Mouse Clicks**: `mouseClicked(Click click, boolean bl)` takes a `Click` record.
  - Access coordinates: `click.x()`, `click.y()`.
  - Access button: `click.button()`.
- **Key Presses**: `keyPressed(KeyInput input)` takes a `KeyInput` record.
  - Access key code: `input.key()`.
  - Access modifiers: `input.hasCtrl()`, `input.hasShift()`, `input.hasAlt()`.
- **Character Typing**: `charTyped(CharInput input)` takes a `CharInput` record.
  - Access string/char: `input.asString()`, `input.isValidChar()`.
- **Mouse Wheel Scrolling**: `mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount)`:
  - Panel scrolling clamps offset between `0` and `maxScroll`.
  - Slider adjustment alters values by `setting.getIncrement()`.

### B. DrawContext Rendering Pipeline
- In Minecraft 1.21.11, Mojang overhauled texture and GUI pipelines.
- Standard texture drawing must specify a `RenderPipeline`:
  ```java
  context.drawTexture(
      RenderPipelines.GUI_TEXTURED,
      identifier,
      (int) x, (int) y,
      (float) u, (float) v,
      width, height,
      textureWidth, textureHeight,
      color
  );
  ```
- Built-in scissoring is provided on `DrawContext`:
  ```java
  context.enableScissor(x1, y1, x2, y2);
  // render widgets
  context.disableScissor();
  ```

### C. Player Inventory & Equipment
- `player.getInventory().armor` array does not exist in 1.21.11.
- Use `player.getEquippedStack(EquipmentSlot)`:
  ```java
  ItemStack helm = player.getEquippedStack(EquipmentSlot.HEAD);
  ItemStack chest = player.getEquippedStack(EquipmentSlot.CHEST);
  ItemStack legs = player.getEquippedStack(EquipmentSlot.LEGS);
  ItemStack boots = player.getEquippedStack(EquipmentSlot.FEET);
  ```

### D. Audio & Sound Events
- `PositionedSoundInstance.master(...)` is replaced by:
  ```java
  PositionedSoundInstance.ui(SoundEvent, pitch, volume);
  ```

### E. Modern OpenGL (Core Profile 3.2+)
- Legacy GLSL 120 (`#version 120`, `gl_ModelViewProjectionMatrix`, `gl_MultiTexCoord0`) fails on modern drivers.
- Shaders must use `#version 150` with explicit inputs (`in vec2 aPos`), outputs (`out vec4 fragColor`), and VAO/VBO bindings.

---

## 5. Font System (CFont)

The custom font engine renders TrueType/OpenType fonts dynamically:
- Character map covers full ASCII (32–126), Latin-1 Supplement (160–255), and Russian/Cyrillic (`А-Я`, `а-я`, `Ё`, `ё`, 1040–1103).
- Generates an atlas image using `java.awt.Graphics2D` with `KEY_TEXT_ANTIALIASING`.
- Uploads the image to GPU VRAM using Minecraft's `NativeImage` and `NativeImageBackedTexture`.
- `FontManager.INSTANCE` automatically routes string drawing:
  - If `Fonts.INSTANCE.isToggled()`: renders via `CFont` (with shadow and Minecraft `§` color codes).
  - If `Fonts.INSTANCE` is disabled: falls back seamlessly to Minecraft's vanilla `TextRenderer`.

---

## 6. Frosted Glass Blur Engine

`BlurEngine` executes a two-pass Gaussian blur on the active frame:
1. **Pass 1 (Horizontal)**: Samples `sceneTexture` into an offscreen framebuffer (`pingFbo`) with horizontal kernel weights.
2. **Pass 2 (Vertical)**: Samples `pingTexture` back into the main framebuffer.
3. **Region Scissoring**: When drawing Pass 2, `glScissor` is enabled for each bounding box:
    - Category panel rects (`[x, y, x + width, y + height]`).
    - Open module settings containers.
    - Search bar and tooltip rects.
4. Panels draw their semi-transparent background (`UI.INSTANCE.bgColor`) over this blurred framebuffer area, creating true frosted glass.

---

## 7. Verification & Build Commands

Always run build from the project root:
```bash
cd "/home/anvres/Mioclient 1.21.11"
./gradlew clean build -x test
```
Verify that `build/libs/collective-1.21.11-1.0.0.jar` compiles cleanly with `BUILD SUCCESSFUL`.
