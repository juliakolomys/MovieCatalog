package utils;

import java.util.Arrays;
import java.util.List;

public class StringListConverter {

    public List<String> split(String s) {
        if (s == null || s.isBlank()) return List.of();
        return Arrays.stream(s.split(","))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .toList();   // "Action, , Drama"  -> Action -> Drama
    }

    public String join(List<String> list) {
        if (list == null || list.isEmpty()) return null;
        return String.join(",", list);
    }
}
