package crccalc;

/**
 * Created by anthony on 15.05.2017.
 */
public class Crc16 {
    public static AlgoParams Crc16CcittFalse = new AlgoParams("CRC-16/CCITT-FALSE", 16, 0x1021, 0xFFFF, false, false, 0x0, 0x29B1);
    public static AlgoParams Crc16Arc = new AlgoParams("CRC-16/ARC", 16, 0x8005, 0x0, true, true, 0x0, 0xBB3D);
    public static AlgoParams Crc16AugCcitt = new AlgoParams("CRC-16/AUG-CCITT", 16, 0x1021, 0x1D0F, false, false, 0x0, 0xE5CC);
    public static AlgoParams Crc16Buypass = new AlgoParams("CRC-16/BUYPASS", 16, 0x8005, 0x0, false, false, 0x0, 0xFEE8);
    public static AlgoParams Crc16Cdma2000 = new AlgoParams("CRC-16/CDMA2000", 16, 0xC867, 0xFFFF, false, false, 0x0, 0x4C06);
    public static AlgoParams Crc16Dds110 = new AlgoParams("CRC-16/DDS-110", 16, 0x8005, 0x800D, false, false, 0x0, 0x9ECF);
    public static AlgoParams Crc16DectR = new AlgoParams("CRC-16/DECT-R", 16, 0x589, 0x0, false, false, 0x1, 0x7E);
    public static AlgoParams Crc16DectX = new AlgoParams("CRC-16/DECT-X", 16, 0x589, 0x0, false, false, 0x0, 0x7F);
    public static AlgoParams Crc16Dnp = new AlgoParams("CRC-16/DNP", 16, 0x3D65, 0x0, true, true, 0xFFFF, 0xEA82);
    public static AlgoParams Crc16En13757 = new AlgoParams("CRC-16/EN-13757", 16, 0x3D65, 0x0, false, false, 0xFFFF, 0xC2B7);
    public static AlgoParams Crc16Genibus = new AlgoParams("CRC-16/GENIBUS", 16, 0x1021, 0xFFFF, false, false, 0xFFFF, 0xD64E);
    public static AlgoParams Crc16Maxim = new AlgoParams("CRC-16/MAXIM", 16, 0x8005, 0x0, true, true, 0xFFFF, 0x44C2);
    public static AlgoParams Crc16Mcrf4Xx = new AlgoParams("CRC-16/MCRF4XX", 16, 0x1021, 0xFFFF, true, true, 0x0, 0x6F91);
    public static AlgoParams Crc16Riello = new AlgoParams("CRC-16/RIELLO", 16, 0x1021, 0xB2AA, true, true, 0x0, 0x63D0);
    public static AlgoParams Crc16T10Dif = new AlgoParams("CRC-16/T10-DIF", 16, 0x8BB7, 0x0, false, false, 0x0, 0xD0DB);
    public static AlgoParams Crc16Teledisk = new AlgoParams("CRC-16/TELEDISK", 16, 0xA097, 0x0, false, false, 0x0, 0xFB3);
    public static AlgoParams Crc16Tms37157 = new AlgoParams("CRC-16/TMS37157", 16, 0x1021, 0x89EC, true, true, 0x0, 0x26B1);
    public static AlgoParams Crc16Usb = new AlgoParams("CRC-16/USB", 16, 0x8005, 0xFFFF, true, true, 0xFFFF, 0xB4C8);
    public static AlgoParams CrcA = new AlgoParams("CRC-A", 16, 0x1021, 0xc6c6, true, true, 0x0, 0xBF05);
    public static AlgoParams Crc16Kermit = new AlgoParams("CRC-16/KERMIT", 16, 0x1021, 0x0, true, true, 0x0, 0x2189);
    public static AlgoParams Crc16Modbus = new AlgoParams("CRC-16/MODBUS", 16, 0x8005, 0xFFFF, true, true, 0x0, 0x4B37);
    public static AlgoParams Crc16X25 = new AlgoParams("CRC-16/X-25", 16, 0x1021, 0xFFFF, true, true, 0xFFFF, 0x906E);
    public static AlgoParams Crc16Xmodem = new AlgoParams("CRC-16/XMODEM", 16, 0x1021, 0x0, false, false, 0x0, 0x31C3);

    public static final AlgoParams[] Params = new AlgoParams[]
            {
                    Crc16CcittFalse,
                    Crc16Arc,
                    Crc16AugCcitt,
                    Crc16Buypass,
                    Crc16Cdma2000,
                    Crc16Dds110,
                    Crc16DectR,
                    Crc16DectX,
                    Crc16Dnp,
                    Crc16En13757,
                    Crc16Genibus,
                    Crc16Maxim,
                    Crc16Mcrf4Xx,
                    Crc16Riello,
                    Crc16T10Dif,
                    Crc16Teledisk,
                    Crc16Tms37157,
                    Crc16Usb,
                    CrcA,
                    Crc16Kermit,
                    Crc16Modbus,
                    Crc16X25,
                    Crc16Xmodem,
            };
}