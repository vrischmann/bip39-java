bip39
=====

This is a partial implementation of [BIP-0039](https://github.com/bitcoin/bips/blob/master/bip-0039.mediawiki) in Java.

It was not written to work with Bitcoin but rather to have a less error-prone way to communicate keys. This means some features of the original BIP-0039 are not supported.

Supported features
==================

* Encoding a byte array into a mnemonic
* Decoding a mnemonic into a byte array
* English wordlist

Missing features
================

* Wordlists for other languages
* Checksum validation
* Seed creation

Correctness
===========

This library has not been tested extensively, but:

* what is implemented is unit tested and has good coverage
* it passes all english test vectors

Installation
============

TODO

Getting started
===============

It's trivial to use:

```java
import fr.rischmann.bip39.BIP39;
import fr.rischmann.bip39.WordList;

public class App {
    public static void main(String[] args) {
        byte[] data = ...
        String mnemonic = BIP39.mnemonic(WordList.Language.ENGLISH, data);
	... // do something with the mnemonic


	byte[] tmp = BIP39.bytes(WordList.Language.ENGLISH, mnemonic);
	... // do somethhing with the bytes
    }
}
```
