package com.wordonline.account.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CSVListReader {

    private final ResourceLoader resourceLoader;

    public static List<String> readCSV(String filePath) {
        List<String> list = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            boolean firstLine = true;
            while ((line = br.readLine()) != null) {
                if (firstLine) {
                    firstLine = false;
                    continue;
                }
                list.add(line.trim());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return list;
    }

    @Cacheable("csvCache")
    public List<String> readCSVFromClasspath(String path) throws Exception {
        Resource resource = resourceLoader.getResource(path); // "classpath:/part1_adjectives.csv"
        List<String> list = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(resource.getInputStream(), "UTF-8"))) {
            String line;
            boolean firstLine = true;
            while ((line = br.readLine()) != null) {
                if (firstLine) {
                    firstLine = false;
                    continue;
                }
                list.add(line.trim());
            }
        }
        return list;
    }
}
