package net.itr0.toolapps.domain.decode;

/**
 * Represents a string to byte array value decoder.
 *
 * @author ryotan
 * @since 1.0
 */
public interface StringDecoder {

    byte[] decode(String encoded);
}
