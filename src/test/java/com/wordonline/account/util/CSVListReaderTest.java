package com.wordonline.account.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

class CSVListReaderTest {

    @Test
    void testReadCSV_Success(@TempDir File tempDir) throws IOException {
        File csvFile = new File(tempDir, "test.csv");
        try (FileWriter writer = new FileWriter(csvFile)) {
            writer.write("header1,header2\n");
            writer.write("item1\n");
            writer.write("item2\n");
        }

        List<String> result = CSVListReader.readCSV(csvFile.getAbsolutePath());

        assertThat(result).containsExactly("item1", "item2");
    }

    @Test
    void testReadCSV_FileNotFound() {
        List<String> result = CSVListReader.readCSV("non_existent_file.csv");

        assertThat(result).isEmpty();
    }
}
