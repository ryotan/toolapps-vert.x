package net.itr0.toolapps.digest

import net.itr0.toolapps.domain.convert.ByteArrayConverter

import java.security.MessageDigest

/**
 *
 * @author ryotan
 * @since 1.0
 */
abstract class MessageDigestConverterSupport implements ByteArrayConverter {

    abstract protected MessageDigest getDigest();

    @Override
    byte[] convert(byte[] value) {
        MessageDigest md = getDigest()
        try {
            return md.digest(value)
        } finally {
            md.reset()
        }
    }
}
