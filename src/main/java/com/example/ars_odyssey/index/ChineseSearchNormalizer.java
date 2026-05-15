package com.example.ars_odyssey.index;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Lightweight Chinese pinyin normalization for Ars Odyssey search suggestions.
 *
 * Character→pinyin mapping is loaded from the bundled resource file
 *   /assets/ars_odyssey/pinyin_table.txt
 * which covers vanilla Minecraft entity/block/item names and Ars Nouveau glyph names.
 * A small set of hardcoded entries (defined below) supplements or overrides the file.
 *
 * File format: one entry per line — `char=pinyin` (ASCII pinyin, no tone numbers).
 * Lines starting with '#' or blank lines are ignored.
 *
 * Search keys generated per term:
 *   fullPinyin  — concatenated pinyin of all Chinese characters (e.g. "dongxuezhizhu")
 *   initials    — first letter of each syllable        (e.g. "dxzz")
 * Non-Chinese ASCII chars pass through as-is.
 *
 * Inspired by Just Enough Characters (github.com/Towdium/JustEnoughCharacters) but
 * intentionally self-contained so Ars Odyssey has no required dependency on JEC.
 */
public final class ChineseSearchNormalizer {
    private static final Logger LOGGER = LogManager.getLogger("Ars Odyssey Pinyin");
    private static final String PINYIN_TABLE_RESOURCE = "/assets/ars_odyssey/pinyin_table.txt";

    private static final Map<Character, String> PINYIN_BY_CHAR = new HashMap<>();

    static {
        // Step 1: load the bundled resource file (covers ~250+ characters)
        loadPinyinTable();

        // Step 2: hardcoded entries (override / supplement the file for common MC terms)
        // These are checked AFTER the file, so they take priority.
        put("僵尸", "jiang", "shi");
        put("尸", "shi");
        put("石头", "shi", "tou");
        put("石", "shi");
        put("头", "tou");
        put("伤害", "shang", "hai");
        put("伤", "shang");
        put("害", "hai");
        put("治疗", "zhi", "liao");
        put("治", "zhi");
        put("疗", "liao");
        put("召唤", "zhao", "huan");
        put("召", "zhao");
        put("唤", "huan");
        put("生物", "sheng", "wu");
        put("方块", "fang", "kuai");
        put("物品", "wu", "pin");
        put("魔符", "mo", "fu");
        put("全部", "quan", "bu");
        put("骷髅", "ku", "lou");
        put("苦力怕", "ku", "li", "pa");
        put("蜘蛛", "zhi", "zhu");
        put("牛", "niu");
        put("羊", "yang");
        put("猪", "zhu");
        put("鸡", "ji");
        put("马", "ma");
        put("狼", "lang");
        put("村民", "cun", "min");
        put("苹果", "ping", "guo");
        put("小麦", "xiao", "mai");
        put("橡木", "xiang", "mu");
        put("圆石", "yuan", "shi");
        put("泥土", "ni", "tu");
        put("草方块", "cao", "fang", "kuai");
        put("火", "huo");
        put("水", "shui");
        put("岩浆", "yan", "jiang");
        put("冰", "bing");
    }

    private ChineseSearchNormalizer() {
    }

    // ── Resource loader ────────────────────────────────────────────────────────

    /**
     * Loads character→pinyin mappings from the bundled text resource.
     * Each valid line is: single-char=pinyin  (e.g. 洞=dong)
     * Comments (#) and blank lines are skipped.
     * Errors are logged as warnings; partial loading is accepted.
     */
    private static void loadPinyinTable() {
        int loaded = 0;
        try (InputStream is = ChineseSearchNormalizer.class.getResourceAsStream(PINYIN_TABLE_RESOURCE)) {
            if (is == null) {
                LOGGER.warn("[Ars Odyssey] Pinyin table resource not found: {}", PINYIN_TABLE_RESOURCE);
                return;
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) {
                        continue;
                    }
                    int sep = line.indexOf('=');
                    if (sep != 1) {
                        // Expect exactly one char before '='
                        continue;
                    }
                    char ch = line.charAt(0);
                    String pinyin = line.substring(2).trim().toLowerCase(Locale.ROOT);
                    if (!pinyin.isEmpty()) {
                        PINYIN_BY_CHAR.put(ch, pinyin);
                        loaded++;
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.warn("[Ars Odyssey] Failed to load pinyin table: {}", e.getMessage());
        }
        LOGGER.info("[Ars Odyssey] Pinyin table loaded: {} character entries from {}", loaded, PINYIN_TABLE_RESOURCE);
    }

    // ── Public API ─────────────────────────────────────────────────────────────

    public static PinyinKeys keysFor(String value) {
        if (value == null || value.isBlank()) {
            return new PinyinKeys("", "");
        }

        StringBuilder full = new StringBuilder();
        StringBuilder initials = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            String pinyin = PINYIN_BY_CHAR.get(c);
            if (pinyin != null && !pinyin.isBlank()) {
                full.append(pinyin);
                initials.append(pinyin.charAt(0));
            } else if (isAsciiSearchChar(c)) {
                char lower = Character.toLowerCase(c);
                full.append(lower);
                initials.append(lower);
            }
        }
        return new PinyinKeys(full.toString(), initials.toString());
    }

    public static String normalizeLoose(String value) {
        return value == null
                ? ""
                : value.toLowerCase(Locale.ROOT)
                .replace("_", "")
                .replace(" ", "")
                .replace("-", "")
                .replace(":", "");
    }

    // ── Internals ──────────────────────────────────────────────────────────────

    private static boolean isAsciiSearchChar(char c) {
        return (c >= 'a' && c <= 'z')
                || (c >= 'A' && c <= 'Z')
                || (c >= '0' && c <= '9');
    }

    /**
     * Maps each character in {@code text} to the corresponding pinyin syllable.
     * The number of pinyin syllables may be less than or equal to text.length()
     * (if there are more chars than syllables, trailing chars are unmapped).
     */
    private static void put(String text, String... pinyin) {
        int length = Math.min(text.length(), pinyin.length);
        for (int i = 0; i < length; i++) {
            PINYIN_BY_CHAR.put(text.charAt(i), pinyin[i]);
        }
    }

    public record PinyinKeys(String fullPinyin, String initials) {
        public PinyinKeys {
            fullPinyin = fullPinyin == null ? "" : fullPinyin;
            initials = initials == null ? "" : initials;
        }
    }
}
