# AGENTS.md — Полная архитектурная и инженерная документация Collective Client (1.21.11 Fabric)

> **Назначение документа**: Исчерпывающее техническое руководство для AI-агентов, LLM и разработчиков, работающих над кодовой базой **Collective Client**. Содержит полные правила проектирования, шаблоны реализации, внутренние соглашения, структуру пакетов, античит-байпасы и инструкции по сборке.

---

## 1. Обзор проекта и технологический стек

| Характеристика | Спецификация |
|---|---|
| **Название проекта** | Collective Client (Collective Core) |
| **Целевая платформа** | Minecraft `1.21.11` (Fabric Mod) |
| **Язык & JDK** | Java `21` (LTS, OpenJDK / Eclipse Temurin) |
| **Система сборки** | Gradle `9.8.1` + Fabric Loom `1.17.21` |
| **Набор маппингов** | Yarn (`1.21.11+build.6-v2`) |
| **Главный пакет** | `me.collective` |
| **Главный класс инициализации** | `me.collective.CollectiveClient` (`ClientModInitializer`) |
| **Внешние библиотеки** | Baritone API, ViaFabric / ViaVersion, Netty, Discord IPC |

### Команды сборки и проверки (CLI)
Рабочая директория: `/home/anvres/Collective 1.21.11`

```bash
# 1. Быстрая проверка компиляции исходников (занимает < 1-2 сек)
./gradlew compileJava

# 2. Полная сборка с валидацией Access Wideners и ремаппингом JAR
./gradlew build -x test

# 3. Расположение готового мода
build/libs/collective-1.21.11-1.0.0.jar
```

---

## 2. Глобальные правила и ограничения для агентов

1. **Запрет сторонних Event-аннотаций**:
   - ❌ **НЕ использовать**: `com.google.common.eventbus.Subscribe`, `meteordevelopment.orbit.EventHandler`, `net.minecraftforge.eventbus.api.SubscribeEvent`.
   -  **Использовать ТОЛЬКО**: `me.collective.event.EventHandler` на методах-слушателях шины `me.collective.event.EventBus.INSTANCE`.

2. **Запрет прямого изменения углов поворота в читах**:
   - Изменение `mc.player.setYaw()` или `mc.player.setPitch()` напрямую флагуется GrimAC и большинством античитов при движении.
   - Все боевые и вспомогательные ротации обязаны проходить через `me.collective.util.RotationUtils`.

3. **Соблюдение целостности Mixin-конфигов**:
   - Каждый созданный класс миксина обязан быть объявлен в файле `src/main/resources/collective.mixins.json` в секции `"client"`.
   - Любое расширение видимости полей/методов Vanilla регистрируется в `src/main/resources/collective.accesswidener`.

4. **Лицензионная политика**:
   - Репозиторий защищен лицензией **All Rights Reserved**. Весь новый код создается с сохранением авторского стиля и структуры пакетов.

---

## 3. Детальная структура проекта (`me.collective`)

```
src/main/java/me/collective/
├── CollectiveClient.java               # Инициализация мода, сервисов и команд
│
├── baritone/                           # Baritone интеграции и автономные процессы
│   └── OverworldElytraProcess.java     # Автономный волновой полет на элитрах (Pitch 40)
│
├── category/                           # Перечисление категорий (Category.java)
│   └── Category.java                   # COMBAT, MOVEMENT, PLAYER, RENDER, EXPLOIT, HUD, MISC, CLIENT
│
├── command/                            # Командный движок клиента
│   ├── Command.java                    # Базовый абстрактный класс команды
│   ├── CommandManager.java             # Регистрация и парсинг команд (префикс '.')
│   └── commands/                       # Реализации (.bind, .toggle, .elytrafly, .сорт, .перенос и т.д.)
│
├── event/                              # Собственная шина событий
│   ├── EventBus.java                   # Диспетчер подписок/отписок и рассылки
│   ├── EventHandler.java               # Аннотация для методов-слушателей
│   ├── CancellableEvent.java           # Базовый класс отменяемых событий
│   └── events/                         # Классы событий:
│       ├── TickEvent.java              # TickEvent.Pre, TickEvent.Post
│       ├── PacketEvent.java            # PacketEvent.Send, PacketEvent.Receive
│       ├── PlayerMoveEvent.java        # Перехват движения игрока
│       ├── MouseClickEvent.java        # Клики мыши (включая СКМ)
│       ├── KeyInputEvent.java          # Ввод с клавиатуры
│       ├── Render3DEvent.java          # Отрисовка в мире (ESP, Tracers)
│       ├── RenderHeldItemEvent.java    # Отрисовка предметов в руках
│       └── StartBreakingBlockEvent.java# Начало копания блоков
│
├── gui/                                # Графический интерфейс
│   ├── clickgui/                       # ClickGUI окна категорий, модулей, настроек
│   └── hud/                            # HUD Editor для перетаскивания элементов
│
├── manager/                            # Менеджеры состояния клиента
│   └── FriendManager.java              # Список друзей (addFriend, isFriend, removeFriend)
│
├── mixin/                              # Mixin-классы (Vanilla & Fabric хуки)
│   ├── MixinClientConnection.java      # Сетевой перехват входящих/исходящих пакетов
│   ├── MixinClientPlayerEntity.java    # Ротации, инпут, Freecam хуки
│   ├── MixinKeyboard.java              # Обработка биндов и глобального ввода
│   ├── MixinMouse.java                 # Обработка мыши
│   ├── MixinChatInputSuggestor.java    # Автодополнение команд и аргументов
│   ├── MixinElytraFeatureRenderer.java # Рендер плащей и элитр
│   └── MixinLivingEntityRenderer.java  # Шейдеры и Chams на сущностей
│
├── module/                             # Модули клиента
│   ├── Module.java                     # Базовый абстрактный класс модуля
│   ├── ModuleManager.java              # Реестр всех модулей клиента
│   ├── client/                         # Системные: UI, HUD, Fonts, Discord, AntiCheat
│   ├── combat/                         # ПВП: GrimAnchorAura, KillAura, Offhand, Surround
│   ├── movement/                       # Движение: GrimScaffold, ElytraBounce, Pitch40, HoleSnap, ControlFly
│   ├── player/                         # Автоматизация: StashMover, StashSorter, AutoTool, MiddleClickExtra
│   ├── exploit/                        # Эксплойты: NewChunks, ShulkerDupe, WorldEventCoords
│   ├── render/                         # Визуал: TargetESP, HoleESP, NameTags, Fullbright, StorageESP
│   ├── hud/                            # Элементы интерфейса: Watermark, ModuleList, Armor, Position
│   └── misc/                           # Разное: ServerSpoof, AutoReconnect, Notifier, Spam, AutoFish
│
├── setting/                            # Система настроек модулей
│   ├── Setting.java                    # Базовый дженерик класс настройки
│   ├── BooleanSetting.java             # Переключатель true/false
│   ├── NumberSetting.java              # Слайдер чисел (double/int/float)
│   ├── ModeSetting.java                # Селектор строк/режимов
│   ├── ColorSetting.java               # Выбор цвета RGBA
│   ├── KeybindSetting.java             # Привязка клавиши (GLFW)
│   └── StringSetting.java              # Текстовое поле ввода
│
└── util/                               # Утилиты
    ├── RotationUtils.java              # Античит-ротации GrimAC / Vanilla
    ├── InvUtils.java                   # Свап предметов, поиск в инвентаре
    ├── PacketUtil.java                 # Тихая отправка пакетов (sendSilently)
    ├── BaritoneBridge.java             # Взаимодействие с Baritone API
    ├── DiscordIpc.java                 # Прямое взаимодействие с Discord через сокеты
    ├── printer/                        # Схемо-принтер (Litematica интеграция)
    ├── sort/                           # Логика автоматической сортировки сундуков/стешей
    └── tracker/                        # Индексация и трекинг содержимого сундуков
```

---

## 4. Жизненный цикл и разработка модуля (`Module.java`)

Базовый класс `me.collective.module.Module` предоставляет:
- Поля `mc` и `client` (ссылка на `MinecraftClient.getInstance()`).
- Методы жизненного цикла: `onEnable()`, `onDisable()`, `onActivate()`, `onDeactivate()`, `onUpdate()`, `onRender2D()`.
- Регистрацию настроек через метод `add(Setting<?> setting)`.
- Вывод сообщений в чат: `info(msg)`, `warning(msg)`, `error(msg)`.

### Шаблон создания нового модуля

```java
package me.collective.module.misc;

import me.collective.category.Category;
import me.collective.event.EventHandler;
import me.collective.event.events.PacketEvent;
import me.collective.event.events.TickEvent;
import me.collective.module.Module;
import me.collective.setting.BooleanSetting;
import me.collective.setting.NumberSetting;
import me.collective.setting.ModeSetting;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

public class MyCustomModule extends Module {
    // 1. Объявление настроек (добавляются в UI через add)
    public final BooleanSetting autoToggle = add(new BooleanSetting("AutoToggle", true));
    public final NumberSetting range = add(new NumberSetting("Range", 4.5, 1.0, 6.0, 0.1));
    public final ModeSetting mode = add(new ModeSetting("Mode", "Normal", "Normal", "Strict", "Silent"));

    public MyCustomModule() {
        super("MyCustomModule", "Описание того, что делает модуль.", Category.MISC);
    }

    @Override
    public void onEnable() {
        // Логика инициализации при включении (EventBus подписывает модуль сам)
    }

    @Override
    public void onDisable() {
        // Сброс таймеров/состояний при выключении (EventBus отписывает модуль сам)
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;
        
        if (mode.process("Strict")) {
            // Режим Strict
        }
    }

    @EventHandler
    private void onPacketSend(PacketEvent.Send event) {
        if (event.packet instanceof PlayerMoveC2SPacket) {
            // Отмена пакета при необходимости:
            // event.cancel();
        }
    }
}
```

### Регистрация в `ModuleManager`
Любой новый модуль **обязательно** регистрируется в методе `init()` в `me.collective.module.ModuleManager`:
```java
register(new MyCustomModule());
```

---

## 5. Работа с шиной событий (`EventBus`)

Все события клиента отправляются через `EventBus.INSTANCE.post(event)` и ловятся аннотацией `@EventHandler`.

### Доступные ключевые события:
- `TickEvent.Pre` / `TickEvent.Post`: вызываются каждый игровой такт клиента.
- `PacketEvent.Send`: отправка пакета клиентом серверу (`event.packet`, `event.cancel()`).
- `PacketEvent.Receive`: получение пакета от сервера (`event.packet`, `event.cancel()`).
- `PlayerMoveEvent`: вызывается перед обработкой физики движения игрока (`event.movement`, `event.type`).
- `MouseClickEvent`: нажатие кнопок мыши (`event.button` — 0 = ЛКМ, 1 = ПКМ, 2 = СКМ).
- `KeyInputEvent`: нажатие клавиш клавиатуры.
- `Render3DEvent`: рендер в мире (используется для ESP, отрисовки боксов, трассеров).
- `StartBreakingBlockEvent`: перехват начала удара по блоку.

---

## 6. Движение, ротации и байпасы античитов (GrimAC / Strict)

### 1. Ротации (`RotationUtils`)
Клиент использует специальную систему перехвата углов `RotationUtils`:
- Модификаторы `@ModifyExpressionValue` в `MixinClientPlayerEntity` подменяют реальные отправляемые значения `yaw` и `pitch` пакетов `PlayerMoveC2SPacket` на серверные.
- Чтобы навести прицел на цель без дергания камеры игрока:
  ```java
  RotationUtils rot = RotationUtils.getInstance();
  rot.setTargetRotation(targetYaw, targetPitch);
  ```
- Для мгновенного синхронного поворота:
  ```java
  rot.setServerYaw(targetYaw);
  rot.setServerPitch(targetPitch);
  ```

### 2. Свап предметов (`InvUtils`)
Для работы модулей без киков за рассинхронизацию слотов:
- Найти предмет: `FindItemResult result = InvUtils.findInHotbar(Items.ENDER_PEARL);`
- Быстрый свап с возвратом:
  ```java
  InvUtils.swap(result.slot(), true);
  // Действие...
  InvUtils.swapBack();
  ```

---

## 7. Baritone & Процесс полета на элитрах (`OverworldElytraProcess`)

Полет в Верхнем мире и Энде реализован в `me.collective.baritone.OverworldElytraProcess`:
1. **Команда управления**: `.elytrafly <x> <z>` (алиасы: `.efly`, `.belytra`).
2. **Кинематика Pitch-40**:
   - Пикирование: угол наклона `+40°` для быстрого разгона до скорости > 2.2–2.5 блоков/тик.
   - Подъем: плавный набор высоты при угле `-35°` по баллистической дуге.
3. **Экономия ресурсов**:
   - Использование фейерверка только при падении скорости ниже порога (1.4 б/т) и достаточной высоте.
   - Автоматический контроль прочности элитр: при достижении критической прочности процесс безопасно сажает игрока.
4. **Умная посадка**: при подлете к цели ближе 40 блоков модуль переходит в режим мягкой спиральной посадки и снимает элитры.

---

## 8. Discord Rich Presence (`DiscordIpc`)

- Реализовано прямое нативное IPC подключение к Discord без тяжелых внешних библиотек через UNIX Domain Sockets (`/run/user/<uid>/discord-ipc-0`) и именованные каналы Windows (`\\.\pipe\discord-ipc-*`).
- Управление состоянием: `me.collective.module.client.Discord`.
- Поддерживает:
  - Отображение текущего сервера / одиночной игры.
  - Отображение ника игрока (`UID`).
  - Передачу внешних HTTPS URL для отображения логотипа `Collective Core`.
  - Чтобы изменить заголовок «Играет в ...», в `DiscordIpc.CLIENT_ID` прописывается `Application ID` созданного приложения из Discord Developer Portal.

---

## 9. Чек-лист для AI-агентов перед сдачей кода

Перед завершением любой задачи обязательно проверьте:
- [ ] Все новые Java-классы компилируются без предупреждений: `./gradlew compileJava` возвращает `BUILD SUCCESSFUL`.
- [ ] Все новые миксины внесены в `src/main/resources/collective.mixins.json`.
- [ ] Все новые модули зарегистрированы в `ModuleManager.java`.
- [ ] Не используются запрещенные аннотации сторонних event-систем (только `@me.collective.event.EventHandler`).
- [ ] Нет мусорных временных файлов в репозитории (`.git/` чист от временных скриптов).
