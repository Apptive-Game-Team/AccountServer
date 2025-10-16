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

    private final static String part1Path = "classpath:/nickname/kr/part1_adjectives.csv";
    private final static String part2Path = "classpath:/nickname/kr/part2_descriptors.csv";
    private final static String part3Path = "classpath:/nickname/kr/part3_characters.csv";

    private final CSVListReader csvListReader;

    public String generate() {
        try {
            String part1 = pickRandom(csvListReader.readCSVFromClasspath(part1Path));
            String part2 = pickRandom(csvListReader.readCSVFromClasspath(part2Path));
            String part3 = pickRandom(csvListReader.readCSVFromClasspath(part3Path));
            return String.format(nameFormat, part1, part2, part3);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String pickRandom(List<String> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        int index = random.nextInt(list.size());
        return list.get(index);
    }
}
