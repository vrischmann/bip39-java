package fr.rischmann.bip39;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WordList {
    final List<String> words = new ArrayList<>();
    final Map<String, Integer> indices = new HashMap<>();

    public enum Language {
        ENGLISH
    }

    /***
     * Returns the embedded word list for a language
     *
     * @param language The language of the word list to return
     * @return words
     */
    static WordList get(Language language) {
        switch (language) {
            case ENGLISH:
                return readResource("bip39_english.txt");

            default:
                throw new IllegalArgumentException("unknown language " + language);
        }
    }

    private static WordList readResource(String name) {
        InputStream stream = WordList.class.getClassLoader().getResourceAsStream(name);
        InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8);

        WordList wordList = new WordList();

        try (BufferedReader br = new BufferedReader(reader)) {
            int i = 0;
            while (true) {
                String line = br.readLine();
                if (line == null) {
                    break;
                }

                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }

                wordList.words.add(line);
                wordList.indices.put(line, i);

                i++;
            }
        } catch (IOException e) {
            return wordList;
        }

        return wordList;
    }
}
