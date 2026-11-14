package engine.util.arpg;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.zip.CRC32C;

import engine.root.EngineSetting;
import engine.root.EngineUtility;
import engine.root.UtilityPackage.InternalException;

class ArpgIntegrityUtility extends EngineUtility {

    /*
     * Frames every ARPG file: the header, a CRC32C checksum of the payload,
     * then the payload itself. CRC32C is hardware accelerated and needs no
     * provider to start, so checking a file costs almost nothing, while any
     * flipped bit, truncation or foreign file is still rejected before a
     * single byte is decoded. The payload is read in place, never copied.
     */

    // Header
    private static final byte[] HEADER = createHeader();
    private static final int PAYLOAD_OFFSET = HEADER.length + Integer.BYTES;

    // Seal \\

    static byte[] seal(byte[] payload) {

        byte[] file = new byte[PAYLOAD_OFFSET + payload.length];

        System.arraycopy(HEADER, 0, file, 0, HEADER.length);
        writeChecksum(checksum(payload, 0, payload.length), file, HEADER.length);
        System.arraycopy(payload, 0, file, PAYLOAD_OFFSET, payload.length);

        return file;
    }

    // Open \\

    static int open(byte[] file) {

        verifyHeader(file);

        if (readChecksum(file, HEADER.length) != checksum(file, PAYLOAD_OFFSET, file.length - PAYLOAD_OFFSET))
            throw new InternalException(
                    "ARPG file failed its integrity check: it is corrupted, truncated, or was edited outside the "
                            + "engine");

        return PAYLOAD_OFFSET;
    }

    private static void verifyHeader(byte[] file) {

        if (file.length < PAYLOAD_OFFSET)
            throw new InternalException("ARPG file is too short to hold a header (" + file.length + " bytes)");

        int magicLength = HEADER.length - 1;

        if (!Arrays.equals(file, 0, magicLength, HEADER, 0, magicLength))
            throw new InternalException("File is not an ARPG file: its header does not start with '"
                    + EngineSetting.ARPG_FILE_MAGIC + "'");

        int version = file[magicLength] & EngineSetting.ARPG_BYTE_MASK;

        if (version != EngineSetting.ARPG_FORMAT_VERSION)
            throw new InternalException("ARPG file uses format version " + version
                    + ", but this engine reads version " + EngineSetting.ARPG_FORMAT_VERSION);
    }

    // Checksum \\

    private static int checksum(byte[] bytes, int offset, int length) {

        CRC32C crc = new CRC32C();
        crc.update(bytes, offset, length);
        return (int) crc.getValue();
    }

    private static void writeChecksum(int checksum, byte[] file, int offset) {
        for (int i = 0; i < Integer.BYTES; i++)
            file[offset + i] = (byte) (checksum >>> (i * EngineSetting.ARPG_BYTE_BITS));
    }

    private static int readChecksum(byte[] file, int offset) {

        int checksum = 0;

        for (int i = Integer.BYTES - 1; i >= 0; i--)
            checksum = (checksum << EngineSetting.ARPG_BYTE_BITS) | (file[offset + i] & EngineSetting.ARPG_BYTE_MASK);

        return checksum;
    }

    // Internal \\

    private static byte[] createHeader() {

        byte[] magic = EngineSetting.ARPG_FILE_MAGIC.getBytes(StandardCharsets.US_ASCII);
        byte[] header = Arrays.copyOf(magic, magic.length + 1);
        header[magic.length] = (byte) EngineSetting.ARPG_FORMAT_VERSION;
        return header;
    }
}
