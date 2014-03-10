package net.itr0.toolapps.domain.encode;

/**
 * Represents a byte array to string value encoder.
 *
 * @author ryotan
 * @since 1.0
 */
public interface ByteArrayEncoder {

    String encode(byte[] decoded);
}
