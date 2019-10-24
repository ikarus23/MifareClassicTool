package crccalc;

/**
 * Created by anthony on 15.05.2017.
 */
public class Crc32 {
    public static AlgoParams Crc32 = new AlgoParams("CRC-32", 32, 0x04C11DB7L, 0xFFFFFFFFL, true, true, 0xFFFFFFFFL, 0xCBF43926L);
    public static AlgoParams Crc32Bzip2 = new AlgoParams("CRC-32/BZIP2", 32, 0x04C11DB7L, 0xFFFFFFFFL, false, false, 0xFFFFFFFFL, 0xFC891918L);
    public static AlgoParams Crc32C = new AlgoParams("CRC-32C", 32, 0x1EDC6F41L, 0xFFFFFFFFL, true, true, 0xFFFFFFFFL, 0xE3069283L);
    public static AlgoParams Crc32D = new AlgoParams("CRC-32D", 32, 0xA833982BL, 0xFFFFFFFFL, true, true, 0xFFFFFFFFL, 0x87315576L);
    public static AlgoParams Crc32Jamcrc = new AlgoParams("CRC-32/JAMCRC", 32, 0x04C11DB7L, 0xFFFFFFFFL, true, true, 0x00000000L, 0x340BC6D9L);
    public static AlgoParams Crc32Mpeg2 = new AlgoParams("CRC-32/MPEG-2", 32, 0x04C11DB7L, 0xFFFFFFFFL, false, false, 0x00000000L, 0x0376E6E7L);
    public static AlgoParams Crc32Posix = new AlgoParams("CRC-32/POSIX", 32, 0x04C11DB7L, 0x00000000L, false, false, 0xFFFFFFFFL, 0x765E7680L);
    public static AlgoParams Crc32Q = new AlgoParams("CRC-32Q", 32, 0x814141ABL, 0x00000000L, false, false, 0x00000000L, 0x3010BF7FL);
    public static AlgoParams Crc32Xfer = new AlgoParams("CRC-32/XFER", 32, 0x000000AFL, 0x00000000L, false, false, 0x00000000L, 0xBD0BE338L);

    public static final AlgoParams[] Params = new AlgoParams[]{
            Crc32, Crc32Bzip2, Crc32C, Crc32D, Crc32Jamcrc, Crc32Mpeg2, Crc32Posix, Crc32Q, Crc32Xfer
    };
}