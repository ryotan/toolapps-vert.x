package net.itr0.toolapps.domain.convert;

/**
 * Represents a byte array to byte array value converter.
 *
 * @author ryotan
 * @since 1.0
 */
public interface ByteArrayConverter {

    /**
     * Converts a byte array value to another byte array.
     *
     * @param value raw byte array
     *
     * @return converted byte array
     */
    byte[] convert(byte[] value);
}
