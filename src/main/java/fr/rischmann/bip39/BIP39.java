package fr.rischmann.bip39;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

/***
 * BIP39 implements the BIP-0039 specification defined <a href="https://github.com/bitcoin/bips/blob/master/bip-0039.mediawiki">here</a>.
 * BIP-0039 defines an implementation of a mnemonic sentence used to encode entropy into easy to remember and communicate sequence of words.
 * This class only supports the english word list.
 */
@SuppressWarnings("WeakerAccess")
public class BIP39 {
    public static final int MIN_ENTROPY_SIZE = 16 * 8;
    public static final int MAX_ENTROPY_SIZE = 32 * 8;
    private static final int WORD_BITS = 11;

    /***
     * Decode the mnemonic sentence into a byte array.
     *
     * @param language the language used for the word list
     * @param mnemonic the mnemonic sentence to decode
     * @return a byte array
     */
    public static byte[] bytes(WordList.Language language, String mnemonic) {
        // Get the word list
        WordList wordList = WordList.get(language);

        // Assume the input is always using spaces to separate words.
        List<String> words = new ArrayList<>();
        StringTokenizer tokenizer = new StringTokenizer(mnemonic, " ");
        while (tokenizer.hasMoreTokens()) {
            words.add(tokenizer.nextToken());
        }

        // If the data size is not divisible by 8, add a byte.
        int dataSize = words.size() * 11;
        if (dataSize % 8 != 0) {
            dataSize /= 8;
            dataSize++;
        } else {
            dataSize /= 8;
        }

        // Allocate resulting byte array.
        byte[] data = new byte[dataSize];

        // Iterate over each word, fetch its index from the word list and store it in the byte array.

        for (int i = 0; i < words.size(); i++) {
            String word = words.get(i);

            int index = wordList.indices.get(word);
            storeIndex(data, i, index);
        }

        // Finally return the data byte array without the checksum byte.

        return Arrays.copyOf(data, data.length - 1);
    }

    /***
     * Encode the entropy byte array into a mnemonic sentence.
     * The byte array size must be divisible by 32 and be between {@value MIN_ENTROPY_SIZE} and {@value MAX_ENTROPY_SIZE}.
     *
     * @param language the language used for the word list
     * @param entropy the entropy data to encode
     * @return a mnemonic sentence
     */
    public static String mnemonic(WordList.Language language, byte[] entropy) {
        // Sanity checks
        //

        if (entropy == null) {
            throw new IllegalArgumentException("data can't be null");
        }

        int entropyBits = entropy.length * 8;

        if (entropyBits % 32 != 0) {
            throw new IllegalArgumentException("data size must be a multiple of 32");
        }
        if (entropyBits < MIN_ENTROPY_SIZE || entropyBits > MAX_ENTROPY_SIZE) {
            throw new IllegalArgumentException(String.format("data size must be between %d and %d bytes", MIN_ENTROPY_SIZE, MAX_ENTROPY_SIZE));
        }

        // Get the checksum mask which depends on the entropy length;
        final int mask = checksumMask(entropyBits);

        // Compute SHA256 checksum
        final byte checksum;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] tmp = digest.digest(entropy);
            byte b = tmp[0];

            checksum = (byte) (b & mask);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("unable to compute the SHA256 of the entropy bytes");
        }

        // Append checksum to entropy
        //

        // Create a new byte array with room for one entropy byte
        byte[] newEntropy = new byte[entropy.length + 1];
        // Copy over the entropy data
        System.arraycopy(entropy, 0, newEntropy, 0, entropy.length);
        // Write the checksum byte
        newEntropy[entropy.length] = checksum;

        // Generate the mnemonic sentence
        //

        final WordList wordList = WordList.get(language);

        // Compute the number of words required to represent the entropy.
        final int checksumLength = entropyBits / 32;
        final int nbWords = (entropyBits + checksumLength) / WORD_BITS;

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < nbWords; i++) {
            // For each required word, get its word list index.

            int idx = extractIndex(newEntropy, i);
            if (i > 0) {
                sb.append(" ");
            }
            sb.append(wordList.words.get(idx));
        }

        return sb.toString();
    }

    /***
     * Returns the mask to apply to the checksum byte, depending on the entropy length.
     * If the length is invalid it throws a {@link IllegalStateException}.
     *
     * @param length The entropy length
     * @return the checksum mask
     * @throws IllegalStateException if the length is invalid
     */
    private static int checksumMask(int length) {
        return switch (length) {
            case 128 -> 0xF0; // 4 bits
            case 160 -> 0xF8; // 5 bits
            case 192 -> 0xFC; // 6 bits
            case 224 -> 0xFE; // 7 bits
            case 256 -> 0xFF; // 8 bits
            default -> throw new IllegalStateException("invalid checksum length");
        };
    }

    /***
     * Stores the index <b>wordIdx</b> in the byte array <b>data</b> for the position <b>wordPos</b>.
     * This is used when decoding a mnemonic into a byte array.
     *
     * @param data the byte array to populate
     * @param wordPos the word position in the sentence (0 based)
     * @param wordIdx the word index
     */
    static void storeIndex(byte[] data, int wordPos, int wordIdx) {
        int pos;
        int i;

        // This function works by iterating over the WORD_BITS bits of a word index and storing
        // them in the correct bytes in the array "data".
        //
        // The first byte we write to is compute like this: wordPos * WORD_BITS
        //
        // So for example if our word is in the 4th position:
        //   pos = 4 * 11 = 44
        // And if wordIdx is 353, corresponding to the word "club"
        //   wordIdx = 0b0000000101100001
        // These are the iterations this loop would perform:
        //  pos = 44, i = 0,  mask = 1024 = 0b0000010000000000, pass = 0
        //  pos = 45, i = 1,  mask = 512  = 0b0000001000000000, pass = 0
        //  pos = 46, i = 2,  mask = 256  = 0b0000000100000000, pass = 1, b = 1 << (7 - (46%8)) = 2
        //  pos = 47, i = 3,  mask = 128  = 0b0000000010000000, pass = 0
        //  pos = 48, i = 4,  mask = 64   = 0b0000000001000000, pass = 1, b = 1 << (7 - (48%8)) = 128
        //  pos = 49, i = 5,  mask = 32   = 0b0000000000100000, pass = 1, b = 1 << (7 - (49%8)) = 64
        //  pos = 50, i = 6,  mask = 16   = 0b0000000000010000, pass = 0
        //  pos = 51, i = 7,  mask = 8    = 0b0000000000001000, pass = 0
        //  pos = 52, i = 8,  mask = 4    = 0b0000000000000100, pass = 0
        //  pos = 53, i = 9,  mask = 2    = 0b0000000000000010, pass = 0
        //  pos = 54, i = 10, mask = 1    = 0b0000000000000001, pass = 1, b = 1 << (7 - (54%8)) = 2

        for (pos = wordPos * WORD_BITS, i = 0; i < WORD_BITS; i++, pos++) {
            // define a mask to fetch the correct bit in wordIdx for the current position.
            int mask = 1 << (WORD_BITS - i - 1);

            // Check if the bit at the current position is set.
            // If it isn't we don't need to do anything.
            if ((wordIdx & mask) == 0) {
                continue;
            }

            // The mask will give us a byte with a single bit set for the current position inside the current byte.
            // This is sufficient to just OR the bit after.
            int b = bitMask(pos);

            // OR the mask to the current byte.
            data[pos / 8] |= (byte) b;
        }
    }

    /***
     * Compute the index of the word at position <b>wordPos</b> using the data in <b>entropy</b>.
     * This is used when encoding a byte array into a mnemonic.
     *
     * @param entropy the entropy data
     * @param wordPos the word position in the sentence (0 based)
     * @return the index in a word list
     */
    static int extractIndex(byte[] entropy, int wordPos) {
        int pos;
        int end;
        int value;

        // This function works by iterating over the bits in the range applicable for the word at "wordPos".
        //
        // For example, the second word (index 1) will need these bits:
        //  - start = 1 * 11
        //  - end   = start + 11
        //
        // For each position in this range, we fetch the corresponding bit value and write the value to the
        // output value integer.
        // See the comment for the mask to understand how the bit is fetched.
        //
        // To follow up the example above, the loop would iterate other two bytes:
        //  entropy[1] and entropy[2]
        //
        // These are the iterations this loop would perform:
        //  pos = 11, b = entropy[1], mask = 16  = 0b00010000
        //  pos = 12, b = entropy[1], mask = 8   = 0b00001000
        //  pos = 13, b = entropy[1], mask = 4   = 0b00000100
        //  pos = 14, b = entropy[1], mask = 2   = 0b00000010
        //  pos = 15, b = entropy[1], mask = 0   = 0b00000001
        //  pos = 16, b = entropy[2], mask = 128 = 0b10000000
        //  pos = 17, b = entropy[2], mask = 64  = 0b01000000
        //  pos = 18, b = entropy[2], mask = 32  = 0b00100000
        //  pos = 19, b = entropy[2], mask = 16  = 0b00010000
        //  pos = 20, b = entropy[2], mask = 8   = 0b00001000
        //  pos = 21, b = entropy[2], mask = 4   = 0b00000100

        for (pos = wordPos * WORD_BITS, end = pos + WORD_BITS, value = 0; pos < end; pos++) {
            // fetch the byte needed for the current position
            int b = entropy[pos / 8];

            int mask = bitMask(pos);

            // Shift the current value by one to the left since we're adding a single bit.
            value <<= 1;
            // Append a 1 if the bit for the current position is set, 0 otherwise.
            value |= isBitSet(b, mask);
        }

        return value;
    }

    /***
     * Computes a mask to get the bit for the position <b>pos</b>.
     * This function works by computing a bit shift for the current position mod 8 to stay in the byte range.
     * For example, if pos == 4 then pos % 8 == 4.
     * The shift is 7-4 == 3 and the mask is this:
     *  - 1<<3 == 8
     *         == 0b00001000
     * Now if we take pos == 10 then pos % 8 == 2.
     * The shift is 7 - 2 == 5 which the mask is this:
     *  - 1<<5 == 32
     *         == 0b00100000
     *
     * @param pos the position in the bit set
     * @return a mask to get the bit
     */
    private static int bitMask(int pos) {
        return 1 << (7 - (pos % 8));
    }

    /***
     * Returns 1 if the bit b&mask is set, 0 otherwise.
     *
     * @param b the input bits
     * @param mask the mask to apply
     * @return 1 if the bit is set, 0 otherwise.
     */
    private static int isBitSet(int b, int mask) {
        if ((b & mask) == mask) {
            return 1;
        } else {
            return 0;
        }
    }

    private static String b(int b) {
        return String.format("0b%11s", Integer.toBinaryString(b)).replace(" ", "0");
    }
}
