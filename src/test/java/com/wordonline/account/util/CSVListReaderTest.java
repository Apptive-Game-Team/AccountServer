package com.wordonline.account.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CSVListReaderTest {

    @Mock
    private ResourceLoader resourceLoader;

    @Mock
    private Resource resource;

    @InjectMocks
    private CSVListReader csvListReader;

    @Test
    void testReadCSV_Success(@TempDir Path tempDir) throws Exception {
        File csvFile = tempDir.resolve("test.csv").toFile();
        try (FileWriter writer = new FileWriter(csvFile)) {
            writer.write("Header1,Header2\n");
            writer.write(" Value1 \n");
            writer.write("Value2\n");
        }

        List<String> results = CSVListReader.readCSV(csvFile.getAbsolutePath());

        assertEquals(2, results.size());
        assertEquals("Value1", results.get(0));
        assertEquals("Value2", results.get(1));
    }

    @Test
    void testReadCSV_FileNotFound() {
        List<String> results = CSVListReader.readCSV("/non/existent/file.csv");

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void testReadCSV_HeaderOnly(@TempDir Path tempDir) throws Exception {
        File csvFile = tempDir.resolve("empty.csv").toFile();
        try (FileWriter writer = new FileWriter(csvFile)) {
            writer.write("Header1,Header2\n");
        }

        List<String> results = CSVListReader.readCSV(csvFile.getAbsolutePath());

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void testReadCSVFromClasspath_WithRealClasspathResource() throws Exception {
        ResourceLoader defaultResourceLoader = new DefaultResourceLoader();
        CSVListReader reader = new CSVListReader(defaultResourceLoader);

        List<String> list = reader.readCSVFromClasspath("classpath:nickname/en/part1_adjectives.csv");

        assertNotNull(list);
        assertFalse(list.isEmpty());
    }

    @Test
    void testReadCSVFromClasspath_WithMockedResourceLoader() throws Exception {
        String csvContent = "Header\n  item1  \nitem2\n";
        InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8));

        when(resourceLoader.getResource("classpath:sample.csv")).thenReturn(resource);
        when(resource.getInputStream()).thenReturn(inputStream);

        List<String> results = csvListReader.readCSVFromClasspath("classpath:sample.csv");

        assertEquals(2, results.size());
        assertEquals("item1", results.get(0));
        assertEquals("item2", results.get(1));
    }

    @Test
    void testReadCSVFromClasspath_ThrowsExceptionOnInputStreamError() throws Exception {
        when(resourceLoader.getResource("classpath:error.csv")).thenReturn(resource);
        when(resource.getInputStream()).thenThrow(new IOException("Stream error"));

        assertThrows(IOException.class, () -> csvListReader.readCSVFromClasspath("classpath:error.csv"));
    }
}
