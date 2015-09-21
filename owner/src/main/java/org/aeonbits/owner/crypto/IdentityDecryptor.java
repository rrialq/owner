package org.aeonbits.owner.crypto;

/**
 * IdentityDecryptor is a (non) encryptor: it accepts a value and returns the same value for decripting and encripting.
 * It is used as default value for {@link EncryptedValue} and {@link DecryptorManagerClass}.
 * <p>
 */
public final class IdentityDecryptor
extends AbstractDecryptor {
    @Override
    public String decrypt( String value ) {
        return value;
    }
}
