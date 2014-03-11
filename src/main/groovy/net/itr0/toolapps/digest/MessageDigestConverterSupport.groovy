package net.itr0.toolapps.digest

import net.itr0.toolapps.domain.convert.ByteArrayConverter

import java.security.MessageDigest

/**
 * A support class for computing a digest of a byte array.
 *
 * @author ryotan
 * @since 1.0
 */
abstract class MessageDigestConverterSupport implements ByteArrayConverter {

    /**
     * Returns a {@link MessageDigest} to compute a digest.
     *
     * @return {@link MessageDigest} for {@link #convert(byte [ ])}
     */
    abstract protected MessageDigest getDigest();

    /**
     * Returns the digest value of {@code value}.
     *
     * @param value raw byte array
     * @return resulting hash value
     */
    @Override
    byte[] convert(byte[] value) {
        MessageDigest md = getDigest()
        try {
            return md.digest(value)
        } finally {
            md?.reset()
        }
    }
}
