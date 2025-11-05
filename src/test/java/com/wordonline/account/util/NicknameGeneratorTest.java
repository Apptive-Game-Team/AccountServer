package com.wordonline.account.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;

import static org.junit.jupiter.api.Assertions.*;

class NicknameGeneratorTest {

    private NicknameGenerator nicknameGenerator;
    private CSVListReader csvListReader;

    @BeforeEach
    void setUp() {
        ResourceLoader resourceLoader = new DefaultResourceLoader();
        csvListReader = new CSVListReader(resourceLoader);
        nicknameGenerator = new NicknameGenerator(csvListReader);
    }

    @Test
    void testGenerateKoreanNickname() {
        String nickname = nicknameGenerator.generate();
        assertNotNull(nickname, "Korean nickname should not be null");
        assertFalse(nickname.isBlank(), "Korean nickname should not be blank");
        
        // Korean nicknames should have 3 parts separated by spaces
        String[] parts = nickname.split(" ");
        assertEquals(3, parts.length, "Korean nickname should have 3 parts");
    }

    @Test
    void testGenerateKoreanNicknameWithLocale() {
        String nickname = nicknameGenerator.generate("kr");
        assertNotNull(nickname, "Korean nickname should not be null");
        assertFalse(nickname.isBlank(), "Korean nickname should not be blank");
        
        String[] parts = nickname.split(" ");
        assertEquals(3, parts.length, "Korean nickname should have 3 parts");
    }

    @Test
    void testGenerateEnglishNickname() {
        String nickname = nicknameGenerator.generate("en");
        assertNotNull(nickname, "English nickname should not be null");
        assertFalse(nickname.isBlank(), "English nickname should not be blank");
        
        // English nicknames should have 3 parts separated by spaces
        String[] parts = nickname.split(" ");
        assertEquals(3, parts.length, "English nickname should have 3 parts");
    }

    @Test
    void testGenerateEnglishNicknameWithVariants() {
        // Test with "en_US"
        String nickname1 = nicknameGenerator.generate("en_US");
        assertNotNull(nickname1, "English nickname with en_US should not be null");
        
        // Test with "EN"
        String nickname2 = nicknameGenerator.generate("EN");
        assertNotNull(nickname2, "English nickname with EN should not be null");
        
        // Test with "english"
        String nickname3 = nicknameGenerator.generate("english");
        assertNotNull(nickname3, "English nickname with english should not be null");
    }

    @Test
    void testGenerateKoreanNicknameWithVariants() {
        // Test with "ko"
        String nickname1 = nicknameGenerator.generate("ko");
        assertNotNull(nickname1, "Korean nickname with ko should not be null");
        
        // Test with "KR"
        String nickname2 = nicknameGenerator.generate("KR");
        assertNotNull(nickname2, "Korean nickname with KR should not be null");
        
        // Test with "ko_KR"
        String nickname3 = nicknameGenerator.generate("ko_KR");
        assertNotNull(nickname3, "Korean nickname with ko_KR should not be null");
    }

    @Test
    void testGenerateWithNullLocaleDefaultsToKorean() {
        String nickname = nicknameGenerator.generate(null);
        assertNotNull(nickname, "Nickname with null locale should not be null");
        
        String[] parts = nickname.split(" ");
        assertEquals(3, parts.length, "Nickname should have 3 parts");
    }

    @Test
    void testGenerateWithEmptyLocaleDefaultsToKorean() {
        String nickname = nicknameGenerator.generate("");
        assertNotNull(nickname, "Nickname with empty locale should not be null");
        
        String[] parts = nickname.split(" ");
        assertEquals(3, parts.length, "Nickname should have 3 parts");
    }

    @Test
    void testGenerateWithUnsupportedLocaleDefaultsToKorean() {
        String nickname = nicknameGenerator.generate("fr");
        assertNotNull(nickname, "Nickname with unsupported locale should not be null");
        
        String[] parts = nickname.split(" ");
        assertEquals(3, parts.length, "Nickname should have 3 parts");
    }

    @Test
    void testGenerateMultipleNicknames() {
        // Generate multiple nicknames to ensure randomness works
        String nickname1 = nicknameGenerator.generate("en");
        String nickname2 = nicknameGenerator.generate("en");
        String nickname3 = nicknameGenerator.generate("en");
        
        assertNotNull(nickname1);
        assertNotNull(nickname2);
        assertNotNull(nickname3);
        
        // While nicknames could theoretically be the same, 
        // with sufficient word combinations this is extremely unlikely
        // We just verify they are all generated successfully
    }
}
