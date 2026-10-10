package me.collective.module.client;

import me.collective.category.Category;
import me.collective.event.EventHandler;
import me.collective.event.events.PacketEvent;
import me.collective.event.events.SendMessageEvent;
import me.collective.module.Module;
import me.collective.setting.BooleanSetting;
import me.collective.setting.EnumSetting;
import me.collective.util.ChatUtils;
import net.minecraft.network.packet.c2s.play.ChatCommandSignedC2SPacket;
import net.minecraft.network.packet.c2s.play.CommandExecutionC2SPacket;

import java.util.HashMap;
import java.util.Map;

/**
 * ChatFonts: переводит буквы сообщений в чате в различные шрифты/стили Unicode.
 * Поддерживает латиницу, кириллицу (транслитерация в выбранный стиль) и цифры.
 */
public class ChatFonts extends Module {
    public final EnumSetting<FontStyle> fontStyle = add(new EnumSetting<>("Шрифт", FontStyle.BLOCK));
    public final BooleanSetting applyFromSyntax = add(new BooleanSetting("Только между $f", false));
    public final BooleanSetting translitRussian = add(new BooleanSetting("Кириллица", true));

    private static final String SYNTAX_MARKER = "$f";

    private static final Map<Character, String> BLOCK_MAP = new HashMap<>();
    private static final Map<Character, String> BLACKLETTER_MAP = new HashMap<>();
    private static final Map<Character, String> BOLD_MAP = new HashMap<>();
    private static final Map<Character, String> SCRIPT_MAP = new HashMap<>();
    private static final Map<Character, String> DOUBLESTRUCK_MAP = new HashMap<>();
    private static final Map<Character, String> CIRCLED_MAP = new HashMap<>();
    private static final Map<Character, String> SQUARED_MAP = new HashMap<>();
    private static final Map<Character, String> REGIONAL_MAP = new HashMap<>();
    private static final Map<Character, String> SMALL_CAPS_MAP = new HashMap<>();

    private static final Map<Character, String> CYRILLIC_TO_LATIN = new HashMap<>();

    static {
        char[] letters = "abcdefghijklmnopqrstuvwxyz".toCharArray();

        String[] blockLetters = {
            "🅰", "🅱", "🅲", "🅳", "🅴", "🅵", "🅶", "🅷", "🅸", "🅹",
            "🅺", "🅻", "🼼", "🅽", "🅾", "🅿", "🆀", "🆁", "🆂", "🆃",
            "🆄", "🆅", "🆆", "🆇", "🆈", "🆉"
        };
        // fix "🅼" for M
        blockLetters[12] = "🅼";
        for (int i = 0; i < letters.length; i++) BLOCK_MAP.put(letters[i], blockLetters[i]);

        String[] blackletterLetters = {
            "𝔄", "𝔅", "ℭ", "𝔇", "𝔈", "𝔉", "𝔊", "ℌ", "ℑ", "𝔍",
            "𝔎", "𝔏", "𝔐", "𝔑", "𝔒", "𝔓", "𝔔", "ℜ", "𝔖", "𝔗",
            "𝔘", "𝔙", "𝔚", "𝔛", "𝔜", "ℨ"
        };
        for (int i = 0; i < letters.length; i++) BLACKLETTER_MAP.put(letters[i], blackletterLetters[i]);

        String[] boldLetters = {
            "𝕬", "𝕭", "𝕮", "𝕯", "𝕰", "𝕱", "𝕲", "𝕳", "𝕴", "𝕵",
            "𝕶", "𝕷", "𝕸", "𝕹", "𝕺", "𝕻", "𝑸", "𝕽", "𝕾", "𝕿",
            "𝖀", "𝖁", "𝖂", "𝖃", "𝖄", "𝖅"
        };
        for (int i = 0; i < letters.length; i++) BOLD_MAP.put(letters[i], boldLetters[i]);

        String[] scriptLetters = {
            "𝒜", "𝐵", "𝒞", "𝒟", "𝐸", "𝐹", "𝒢", "𝐻", "𝐼", "𝒥",
            "𝒦", "𝐿", "𝑀", "𝒩", "𝒪", "𝒫", "𝒬", "𝑅", "𝒮", "𝒯",
            "𝒰", "𝒱", "𝒲", "𝒳", "𝒴", "𝒵"
        };
        for (int i = 0; i < letters.length; i++) SCRIPT_MAP.put(letters[i], scriptLetters[i]);

        String[] doubleStruckLetters = {
            "𝔸", "𝔹", "ℂ", "𝔻", "𝔼", "𝔽", "𝔾", "ℍ", "𝕀", "𝕁",
            "𝕂", "𝕃", "𝕄", "ℕ", "𝕆", "ℙ", "ℚ", "ℝ", "𝕊", "𝕋",
            "𝕌", "𝕍", "𝕎", "𝕏", "𝕐", "ℤ"
        };
        for (int i = 0; i < letters.length; i++) DOUBLESTRUCK_MAP.put(letters[i], doubleStruckLetters[i]);

        String[] circledLetters = {
            "Ⓐ", "Ⓑ", "Ⓒ", "Ⓓ", "Ⓔ", "Ⓕ", "Ⓖ", "Ⓗ", "Ⓘ", "Ⓙ",
            "Ⓚ", "Ⓛ", "Ⓜ", "Ⓝ", "Ⓞ", "Ⓟ", "Ⓠ", "Ⓡ", "Ⓢ", "Ⓣ",
            "Ⓤ", "Ⓥ", "Ⓦ", "Ⓧ", "Ⓨ", "Ⓩ"
        };
        for (int i = 0; i < letters.length; i++) CIRCLED_MAP.put(letters[i], circledLetters[i]);

        String[] squaredLetters = {
            "🄰", "🄱", "🄲", "🄳", "🄴", "🄵", "🄶", "🄷", "🄸", "🄹",
            "🄺", "🄻", "🄼", "🄽", "🄾", "🄿", "🅀", "🅁", "🅂", "🅃",
            "🅄", "🅅", "🅆", "🅇", "🅈", "🅉"
        };
        for (int i = 0; i < letters.length; i++) SQUARED_MAP.put(letters[i], squaredLetters[i]);

        String[] regionalLetters = {
            "🇦", "🇧", "🇨", "🇩", "🇪", "🇫", "🇬", "🇭", "🇮", "🇯",
            "🇰", "🇱", "🇲", "🇳", "🇴", "🇵", "🇶", "🇷", "🇸", "🇹",
            "🇺", "🇻", "🇼", "🇽", "🇾", "🇿"
        };
        for (int i = 0; i < letters.length; i++) REGIONAL_MAP.put(letters[i], regionalLetters[i]);

        String[] smallCapsLetters = {
            "ᴀ", "ʙ", "ᴄ", "ᴅ", "ᴇ", "ғ", "ɢ", "ʜ", "ɪ", "ᴊ",
            "ᴋ", "ʟ", "ᴍ", "ɴ", "ᴏ", "ᴘ", "ǫ", "ʀ", "s", "ᴛ",
            "ᴜ", "ᴠ", "ᴡ", "x", "ʏ", "ᴢ"
        };
        for (int i = 0; i < letters.length; i++) SMALL_CAPS_MAP.put(letters[i], smallCapsLetters[i]);

        // Digits for circled, doublestruck, bold
        char[] digits = "0123456789".toCharArray();
        String[] circledDigits = {"⓪", "①", "②", "③", "④", "⑤", "⑥", "⑦", "⑧", "⑨"};
        String[] doubleDigits = {"𝟘", "𝟙", "𝟚", "𝟛", "𝟜", "𝟝", "𝟞", "𝟟", "𝟠", "𝟡"};
        String[] boldDigits = {"𝟎", "𝟏", "𝟐", "𝟑", "𝟒", "𝟓", "𝟔", "𝟕", "𝟖", "𝟗"};
        for (int i = 0; i < digits.length; i++) {
            CIRCLED_MAP.put(digits[i], circledDigits[i]);
            DOUBLESTRUCK_MAP.put(digits[i], doubleDigits[i]);
            BOLD_MAP.put(digits[i], boldDigits[i]);
        }

        // Cyrillic transliteration
        CYRILLIC_TO_LATIN.put('а', "a");
        CYRILLIC_TO_LATIN.put('б', "b");
        CYRILLIC_TO_LATIN.put('в', "v");
        CYRILLIC_TO_LATIN.put('г', "g");
        CYRILLIC_TO_LATIN.put('д', "d");
        CYRILLIC_TO_LATIN.put('е', "e");
        CYRILLIC_TO_LATIN.put('ё', "e");
        CYRILLIC_TO_LATIN.put('ж', "zh");
        CYRILLIC_TO_LATIN.put('з', "z");
        CYRILLIC_TO_LATIN.put('и', "i");
        CYRILLIC_TO_LATIN.put('й', "y");
        CYRILLIC_TO_LATIN.put('к', "k");
        CYRILLIC_TO_LATIN.put('л', "l");
        CYRILLIC_TO_LATIN.put('м', "m");
        CYRILLIC_TO_LATIN.put('н', "n");
        CYRILLIC_TO_LATIN.put('о', "o");
        CYRILLIC_TO_LATIN.put('п', "p");
        CYRILLIC_TO_LATIN.put('р', "r");
        CYRILLIC_TO_LATIN.put('с', "s");
        CYRILLIC_TO_LATIN.put('т', "t");
        CYRILLIC_TO_LATIN.put('у', "u");
        CYRILLIC_TO_LATIN.put('ф', "f");
        CYRILLIC_TO_LATIN.put('х', "h");
        CYRILLIC_TO_LATIN.put('ц', "c");
        CYRILLIC_TO_LATIN.put('ч', "ch");
        CYRILLIC_TO_LATIN.put('ш', "sh");
        CYRILLIC_TO_LATIN.put('щ', "shch");
        CYRILLIC_TO_LATIN.put('ъ', "");
        CYRILLIC_TO_LATIN.put('ы', "y");
        CYRILLIC_TO_LATIN.put('ь', "");
        CYRILLIC_TO_LATIN.put('э', "e");
        CYRILLIC_TO_LATIN.put('ю', "yu");
        CYRILLIC_TO_LATIN.put('я', "ya");
    }

    public static ChatFonts INSTANCE;

    public ChatFonts() {
        super("ChatFonts", "Переводит буквы сообщений в чате в разные шрифты (блоки, готика, жирный и другие).", Category.CLIENT);
        INSTANCE = this;
    }

    @EventHandler
    public void onMessageSend(SendMessageEvent event) {
        if (!isToggled() || event.message == null) return;
        event.message = convertText(event.message, fontStyle.getValue(), applyFromSyntax.getValue());
    }

    @EventHandler
    public void onReceivePacket(PacketEvent.Send event) {
        if (!isToggled()) return;

        // Обработка команд с марreading $f при отправке через сеть
        if (event.packet instanceof ChatCommandSignedC2SPacket chatPacket) {
            handleCommandFormatting(chatPacket.command(), event);
        } else if (event.packet instanceof CommandExecutionC2SPacket execPacket) {
            handleCommandFormatting(execPacket.command(), event);
        }
    }

    private void handleCommandFormatting(String command, PacketEvent.Send event) {
        if (!command.contains(SYNTAX_MARKER)) return;

        // Поиск команды сообщения (например: msg target, tell target, w target, r)
        String[] prefixes = {"msg ", "tell ", "w ", "m ", "pm "};
        int foundPrefix = -1;
        for (String p : prefixes) {
            int idx = command.indexOf(p);
            if (idx != -1 && (foundPrefix == -1 || idx < foundPrefix)) {
                foundPrefix = idx;
                break;
            }
        }

        if (foundPrefix == -1) return;

        int spaceIndex = command.indexOf(' ', foundPrefix + 4);
        if (spaceIndex == -1) return;

        String commandName = command.substring(0, spaceIndex);
        String text = command.substring(spaceIndex + 1).strip();
        if (text.isEmpty()) return;

        event.cancel();
        ChatUtils.sendPlayerMsg("/" + commandName + " " + convertText(text, fontStyle.getValue(), applyFromSyntax.getValue()));
    }

    public String convertText(String message, FontStyle style, boolean syntaxOnly) {
        if (message == null || message.isEmpty()) return message;
        Map<Character, String> fontMap = getFontMap(style);
        if (fontMap == null) return message;

        // Если включен режим только между $f, но в сообщении нет маркера $f — оставляем как есть
        if (syntaxOnly) {
            if (!message.contains(SYNTAX_MARKER)) return message;

            StringBuilder result = new StringBuilder();
            int index = 0;
            boolean applyingFont = false;

            while (index < message.length()) {
                int markerIndex = message.indexOf(SYNTAX_MARKER, index);

                if (markerIndex == -1) {
                    String rest = message.substring(index);
                    result.append(applyingFont ? applyFont(rest, fontMap) : rest);
                    break;
                }

                String before = message.substring(index, markerIndex);
                result.append(applyingFont ? applyFont(before, fontMap) : before);

                applyingFont = !applyingFont;
                index = markerIndex + SYNTAX_MARKER.length();
            }

            return result.toString();
        }

        // Если не команда (не начинается со слеша) или если в сообщении есть маркер
        if (message.startsWith("/")) {
            return message;
        }

        return applyFont(message, fontMap);
    }

    private String applyFont(String text, Map<Character, String> fontMap) {
        StringBuilder converted = new StringBuilder();
        boolean translit = translitRussian.getValue();

        for (char c : text.toCharArray()) {
            char lower = Character.toLowerCase(c);
            if (fontMap.containsKey(lower)) {
                converted.append(fontMap.get(lower));
            } else if (translit && CYRILLIC_TO_LATIN.containsKey(lower)) {
                String lat = CYRILLIC_TO_LATIN.get(lower);
                for (char lc : lat.toCharArray()) {
                    converted.append(fontMap.getOrDefault(lc, String.valueOf(lc)));
                }
            } else {
                converted.append(c);
            }
        }

        return converted.toString();
    }

    private Map<Character, String> getFontMap(FontStyle style) {
        return switch (style) {
            case BLOCK -> BLOCK_MAP;
            case BLACKLETTER -> BLACKLETTER_MAP;
            case BOLD -> BOLD_MAP;
            case SCRIPT -> SCRIPT_MAP;
            case DOUBLESTRUCK -> DOUBLESTRUCK_MAP;
            case CIRCLED -> CIRCLED_MAP;
            case SQUARED -> SQUARED_MAP;
            case REGIONAL -> REGIONAL_MAP;
            case SMALL_CAPS -> SMALL_CAPS_MAP;
        };
    }

    public enum FontStyle {
        BLOCK,
        BLACKLETTER,
        BOLD,
        SCRIPT,
        DOUBLESTRUCK,
        CIRCLED,
        SQUARED,
        REGIONAL,
        SMALL_CAPS
    }
}
