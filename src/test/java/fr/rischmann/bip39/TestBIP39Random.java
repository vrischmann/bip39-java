package fr.rischmann.bip39;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class TestBIP39Random {

    @Parameters(name = "{index}: entropy size={1}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {WordList.Language.ENGLISH, 128},
                {WordList.Language.ENGLISH, 160},
                {WordList.Language.ENGLISH, 192},
                {WordList.Language.ENGLISH, 224},
                {WordList.Language.ENGLISH, 256},
        });
    }

    private WordList.Language language;
    private byte[] data;

    public TestBIP39Random(WordList.Language language, int size) {
        this.language = language;
        this.data = randomData(size / 8);
    }

    @Test
    public void test() {
        byte[] entropy = data;

        String mnemonic = BIP39.mnemonic(language, entropy);
        byte[] tmp = BIP39.bytes(language, mnemonic);

        assertArrayEquals(entropy, tmp);
    }

    private static byte[] randomData(int size) {
        byte[] data = new byte[size];

        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(data);

        return data;
    }
}
