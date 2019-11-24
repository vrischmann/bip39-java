package fr.rischmann.bip39;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class TestBIP39 {
    @Test
    public void storeIndex() {
        byte[] data = new byte[20];

        BIP39.storeIndex(data, 4, 353);

        // wordPos 4 is pos 44 to 54, which is byte 5 and 6.
        // for byte 5 only the last 4 bits count.
        // for byte 6 only the first 7 bits count.

        assertEquals(0b0010, (data[5] & 0xf));
        assertEquals(0b1100_001, (data[6] & 0xfe) >> 1);
    }

    @Test
    public void mnemonicEnglish() throws DecoderException, IOException {
        List<TestVector> vectors = loadTestVectors();

        for (int i = 0; i < vectors.size(); i++) {
            TestVector vector = vectors.get(i);

            byte[] entropyBytes = vector.entropyBytes();

            String mnemonic = BIP39.mnemonic(WordList.Language.ENGLISH, entropyBytes);
            assertEquals(String.format("vector %d failed", i), vector.mnemonic, mnemonic);

            byte[] bytes = BIP39.bytes(WordList.Language.ENGLISH, mnemonic);
            assertArrayEquals(String.format("vector %d failed", i), entropyBytes, bytes);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void mnemonicInvalidNullEntropy() {
        BIP39.mnemonic(WordList.Language.ENGLISH, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void mnemonicInvalidSize() {
        byte[] data = new byte[340];
        BIP39.mnemonic(WordList.Language.ENGLISH, data);
    }

    @Test(expected = IllegalArgumentException.class)
    public void mnemonicInvalidSizeDivisible() {
        byte[] data = new byte[21];
        BIP39.mnemonic(WordList.Language.ENGLISH, data);
    }

    private List<TestVector> loadTestVectors() throws IOException {
        byte[] data = IOUtils.resourceToByteArray("vectors.json", getClass().getClassLoader());

        ObjectMapper objectMapper = new ObjectMapper();

        return objectMapper.readValue(data, new TypeReference<List<TestVector>>() {
        });
    }

    @SuppressWarnings("WeakerAccess")
    static class TestVector {
        public String entropy;
        public String mnemonic;

        byte[] entropyBytes() throws DecoderException {
            return Hex.decodeHex(entropy);
        }
    }
}
