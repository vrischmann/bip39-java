package fr.rischmann.bip39;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TestWordList {
    @Test
    public void english() {
        WordList wordList = WordList.get(WordList.Language.ENGLISH);
        assertNotNull(wordList);

        assertEquals(2048, wordList.words.size());
        assertEquals(2048, wordList.indices.size());
    }
}
