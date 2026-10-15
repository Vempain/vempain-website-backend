package fi.poltsi.vempain.website.tools;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LikePatternsUTC {

    @Test
    void wildcardAndEscapeCharactersAreEscaped() {
        assertEquals("\\%", LikePatterns.escape("%"));
        assertEquals("\\_", LikePatterns.escape("_"));
        assertEquals("\\\\", LikePatterns.escape("\\"));
        assertEquals("50\\% off\\_now", LikePatterns.escape("50% off_now"));
        assertEquals("plain text", LikePatterns.escape("plain text"));
        assertEquals("", LikePatterns.escape(null));
    }

    @Test
    void containsAndPrefixWrapTheEscapedText() {
        assertEquals("%\\%%", LikePatterns.contains("%"));
        assertEquals("%sunset%", LikePatterns.contains("sunset"));
        assertEquals("%sunset%", LikePatterns.containsIgnoreCase("SunSet"));
        assertEquals("trips/2024\\_x%", LikePatterns.prefix("trips/2024_x"));
        assertEquals("%%", LikePatterns.contains(null));
    }

    @Test
    void nullTextYieldsAnEmptyCaseInsensitivePattern() {
        assertEquals("%%", LikePatterns.containsIgnoreCase(null));
    }

    @Test
    void tokenListsAreCappedAtTheMaximum() {
        var tokens = java.util.stream.IntStream.range(0, LikePatterns.MAX_TOKENS + 5)
            .mapToObj(Integer::toString)
            .toList();

        assertEquals(LikePatterns.MAX_TOKENS, LikePatterns.limitTokens(tokens)
            .size());
        assertEquals(tokens.subList(0, LikePatterns.MAX_TOKENS), LikePatterns.limitTokens(tokens));
        assertEquals(java.util.List.of("a", "b"), LikePatterns.limitTokens(java.util.List.of("a", "b")));
        assertTrue(LikePatterns.limitTokens(null)
            .isEmpty());
    }

    @Test
    void overlongTextIsTruncated() {
        var text = "a".repeat(LikePatterns.MAX_TEXT_LENGTH + 50);
        var pattern = LikePatterns.contains(text);

        assertEquals(LikePatterns.MAX_TEXT_LENGTH + 2, pattern.length());
        assertTrue(pattern.startsWith("%a")
                   && pattern.endsWith("a%"));
    }
}
