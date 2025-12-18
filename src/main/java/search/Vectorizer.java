package search;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class Vectorizer {

    private static final List<String> STOP_WORDS = Arrays.asList(
            "the", "a", "an", "and", "or", "of", "in", "on", "for", "with"
    );


    public List<String> tokenize(String text) {
        if (text == null || text.isBlank()) return List.of();

        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "") //é, à -> e, a
                .toLowerCase(Locale.ROOT);

        return Arrays.stream(normalized.split("\\W+"))
                .filter(token -> !STOP_WORDS.contains(token) && !token.isBlank())
                .collect(Collectors.toList());
    }
}

