package com.wordonline.account.util;

import java.util.List;
import java.util.Random;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NicknameGenerator {

    private static final Random random = new Random();

    private final static String nameFormat = "%s %s %s";
    private final static String DEFAULT_LOCALE = "kr";

    private final static String part1PathFormat = "classpath:/nickname/%s/part1_adjectives.csv";
    private final static String part2PathFormat = "classpath:/nickname/%s/part2_descriptors.csv";
    private final static String part3PathFormat = "classpath:/nickname/%s/part3_characters.csv";

    private final CSVListReader csvListReader;

    public String generate() {
        return generate(DEFAULT_LOCALE);
    }

    public String generate(String locale) {
        try {
            String normalizedLocale = normalizeLocale(locale);
            String part1Path = String.format(part1PathFormat, normalizedLocale);
            String part2Path = String.format(part2PathFormat, normalizedLocale);
            String part3Path = String.format(part3PathFormat, normalizedLocale);
            
            String part1 = pickRandom(csvListReader.readCSVFromClasspath(part1Path));
            String part2 = pickRandom(csvListReader.readCSVFromClasspath(part2Path));
            String part3 = pickRandom(csvListReader.readCSVFromClasspath(part3Path));
            return String.format(nameFormat, part1, part2, part3);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String normalizeLocale(String locale) {
        if (locale == null || locale.isBlank()) {
            return DEFAULT_LOCALE;
        }
        
        String lowerLocale = locale.toLowerCase();
        
        // Support both "en" and "en_US" formats
        if (lowerLocale.startsWith("en")) {
            return "en";
        }
        if (lowerLocale.startsWith("ko") || lowerLocale.startsWith("kr")) {
            return "kr";
        }
        
        // Default to Korean if unsupported locale
        return DEFAULT_LOCALE;
    }

    private String pickRandom(List<String> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        int index = random.nextInt(list.size());
        return list.get(index);
    }
}
