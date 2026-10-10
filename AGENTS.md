# AGENTS.md — Collective Client (1.21.11 Fabric)

> Руководство для AI-агентов и разработчиков по архитектуре, модулям, миксинам и правилам разработки клиента **Collective Client**.

---

## 1. Обзор проекта и стек технологий

| Параметр | Значение |
|---|---|
| **Название** | Collective Client (Collective Core) |
| **Версия Minecraft** | `1.21.11` (Fabric Mod) |
| **Java** | `21` (Temurin / OpenJDK 21) |
| **Система сборки** | Gradle 9.8.1 + Fabric Loom 1.17.21 |
| **Маппинги** | Yarn (`1.21.11+build.6-v2`) |
| **Пакет ядра** | `me.collective` |
| **Точка входа** | `me.collective.CollectiveClient` (`ClientModInitializer`) |
| **Интеграции** | Baritone API, ViaFabric / ViaVersion, Discord IPC |

---

## 2. Команды сборки и проверки

Все команды выполняются в корне проекта (`/home/anvres/Collective 1.21.11`):

```bash
# Быстрая проверка компиляции кода
./gradlew compileJava

# Полная сборка с проверкой ресурсов и ремаппингом JAR
./gradlew build -x test

# Готовый собранный JAR-архив
build/libs/collective-1.21.11-1.0.0.jar
```

> [!IMPORTANT]
> Никогда не используйте библиотеки аннотаций `@Subscribe` от Guava или `@EventHandler` из `meteordevelopment.orbit`! В проекте используется собственная шина событий `me.collective.event.EventBus` и аннотация `me.collective.event.EventHandler`.

---

## 3. Архитектура ядра (`me.collective`)

```
src/main/java/me/collective/
├── CollectiveClient.java       # Инициализация мода, регистрация команд, сервисов
├── category/                   # Категории модулей (Category enum)
├── module/                     # Модули клиента (Base: Module.java)
│   ├── client/                 # Системные: UI, HUD, Discord, Fonts, AntiCheat
│   ├── combat/                 # Боевые: GrimAnchorAura, KillAura, Offhand, Surround
│   ├── movement/               # Движение: GrimScaffold, ElytraBounce, Pitch40, HoleSnap
│   ├── player/                 # Игрок: StashMover, StashSorter, AutoTool, MiddleClickExtra
│   ├── exploit/                # Эксплойты: NewChunks, ShulkerDupe, WorldEventCoords
│   ├── render/                 # Визуал: TargetESP, HoleESP, NameTags, Fullbright
│   ├── hud/                    # HUD оверлеи: Watermark, ModuleList, Armor, Position
│   └── misc/                   # Разное: ServerSpoof, AutoReconnect, Notifier, Spam
├── baritone/                   # Интеграция Baritone (OverworldElytraProcess и др.)
├── command/                    # Командный движок (.bind, .toggle, .elytrafly, .сорт)
├── event/                      # Шина событий (EventBus, EventHandler, PacketEvent)
├── gui/                        # Графический интерфейс ClickGUI и HUD Editor
├── manager/                    # Менеджеры состояния (FriendManager)
├── mixin/                      # Срезы Mixin (сеть, ввод, рендер, элитры)
├── setting/                    # Система настроек (Boolean, Number, Mode, Color, Keybind)
└── util/                       # Утилиты (RotationUtils, InvUtils, PacketUtil, BaritoneBridge)
```

---

## 4. Гайд по разработке: Создание нового модуля

Каждый модуль наследует абстрактный класс `me.collective.module.Module`:

```java
package me.collective.module.misc;

import me.collective.category.Category;
import me.collective.event.EventHandler;
import me.collective.event.events.PacketEvent;
import me.collective.event.events.TickEvent;
import me.collective.module.Module;
import me.collective.setting.BooleanSetting;
import me.collective.setting.NumberSetting;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

public class ExampleModule extends Module {
    // Настройки модуля регистрируются через add(...)
    public final BooleanSetting flag = add(new BooleanSetting("Flag", true));
    public final NumberSetting delay = add(new NumberSetting("Delay", 10.0, 1.0, 100.0, 1.0));

    public ExampleModule() {
        super("ExampleModule", "Краткое описание модуля", Category.MISC);
    }

    @Override
    public void onEnable() {
        // Вызывается при включении (EventBus подписывает модуль автоматически)
    }

    @Override
    public void onDisable() {
        // Вызывается при выключении (EventBus отписывает модуль автоматически)
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;
        // Логика каждого тика
    }

    @EventHandler
    private void onPacketSend(PacketEvent.Send event) {
        if (event.packet instanceof PlayerMoveC2SPacket) {
            // event.cancel(); // отмена пакета
        }
    }
}
```

### Регистрация модуля
Все созданные модули обязательно регистрируются в методе `init()` класса `me.collective.module.ModuleManager`:
```java
register(new ExampleModule());
```

---

## 5. Система настроек (`me.collective.setting`)

- `BooleanSetting(name, defaultValue)` — логический переключатель.
- `NumberSetting(name, defaultValue, min, max, increment)` — числовой ползунок. Поддерживает `getIntValue()`, `getFloatValue()`, `getValue()`.
- `ModeSetting(name, defaultMode, modes...)` — выбор режима из списка строк (`process("Mode")`, `cycle()`).
- `ColorSetting(name, defaultColor)` — выбор RGBA цвета.
- `KeybindSetting(name, defaultKey)` — бинд на клавишу.
- `StringSetting(name, defaultValue)` — текстовое поле.

---

## 6. Ротации и движение (Grim & Strict Bypasses)

Для безопасных ротаций без флагов античита GrimAC и Vanilla используется `RotationUtils`:
- Не изменяйте напрямую `mc.player.setYaw()` или `mc.player.setPitch()`, если требуется легитная отправка пакетов.
- Используйте `RotationUtils.getInstance().setTargetRotation(yaw, pitch)` или хуки `MixinClientPlayerEntity`, перехватывающие `sendMovementPackets`.
- Для инвентарных действий на ходу используйте `InvUtils.swap(...)` с последующим восстановлением слота.

---

## 7. Baritone & Elytra Flight

- Процесс полета на элитрах в Overworld/End: `me.collective.baritone.OverworldElytraProcess`.
- Управление: команда `.elytrafly <x> <z>` или `.elytrafly stop`.
- Механика полета: Pitch-40 волнообразная кинематика (пикирование +40°, набор высоты -35°), контроль прочности элитр, взлет без прыжка, умная посадка при подлете к цели, экономия фейерверков.
- Мост вызовов Baritone: `me.collective.util.BaritoneBridge`.

---

## 8. Mixin соглашения

- Конфигурационный файл миксинов: `src/main/resources/collective.mixins.json`.
- Access Wideners: `src/main/resources/collective.accesswidener`.
- Все новые миксин-классы обязаны быть прописаны в массиве `"client"` внутри `collective.mixins.json`.
- Для перехвата возвращаемых значений используйте `com.llamalad7.mixinextras.injector.ModifyExpressionValue` или `@Inject(..., cancellable = true)`.

---

## 9. Сеть и Безопасность

- `MixinClientConnection`: корневая точка перехвата пакетов `PacketEvent.Send` и `PacketEvent.Receive`.
- При отправке пакетов без вызова клиентских слушателей используйте `PacketUtil.sendSilently(packet)`.
- Discord Rich Presence: `me.collective.util.DiscordIpc` подключается через UNIX Domain Socket (`/run/user/<uid>/discord-ipc-0`) или именованные пайпы Windows (`\\.\pipe\discord-ipc-*`).
