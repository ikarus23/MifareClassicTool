package crccalc;

/**
 * Created by anthony on 15.05.2017.
 */
public class Crc8 {
    public static AlgoParams Crc8 = new AlgoParams("CRC-8", 8, 0x7, 0x0, false, false, 0x0, 0xF4);
    public static AlgoParams Crc8Cdma2000 = new AlgoParams("CRC-8/CDMA2000", 8, 0x9B, 0xFF, false, false, 0x0, 0xDA);
    public static AlgoParams Crc8Darc = new AlgoParams("CRC-8/DARC", 8, 0x39, 0x0, true, true, 0x0, 0x15);
    public static AlgoParams Crc8DvbS2 = new AlgoParams("CRC-8/DVB-S2", 8, 0xD5, 0x0, false, false, 0x0, 0xBC);
    public static AlgoParams Crc8Ebu = new AlgoParams("CRC-8/EBU", 8, 0x1D, 0xFF, true, true, 0x0, 0x97);
    public static AlgoParams Crc8ICode = new AlgoParams("CRC-8/I-CODE", 8, 0x1D, 0xFD, false, false, 0x0, 0x7E);
    public static AlgoParams Crc8Itu = new AlgoParams("CRC-8/ITU", 8, 0x7, 0x0, false, false, 0x55, 0xA1);
    public static AlgoParams Crc8Maxim = new AlgoParams("CRC-8/MAXIM", 8, 0x31, 0x0, true, true, 0x0, 0xA1);
    public static AlgoParams Crc8Rohc = new AlgoParams("CRC-8/ROHC", 8, 0x7, 0xFF, true, true, 0x0, 0xD0);
    public static AlgoParams Crc8Wcdma = new AlgoParams("CRC-8/WCDMA", 8, 0x9B, 0x0, true, true, 0x0, 0x25);

    public static final AlgoParams[] Params = new AlgoParams[]{
            Crc8, Crc8Cdma2000, Crc8Darc, Crc8DvbS2, Crc8Ebu, Crc8ICode, Crc8Itu, Crc8Maxim, Crc8Rohc, Crc8Wcdma
    };
}