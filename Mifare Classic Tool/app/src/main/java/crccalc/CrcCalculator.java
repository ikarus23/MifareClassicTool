package crccalc;

/**
 * Created by anthony on 11.05.2017.
 */
public class CrcCalculator {

    public AlgoParams Parameters;
    public byte HashSize = 8;
    private long _mask = 0xFFFFFFFFFFFFFFFFL;
    private long[] _table = new long[256];

    public static final byte[] TestBytes = new byte[]{49,50,51,52,53,54,55,56,57};

    CrcCalculator(AlgoParams params)
    {
        Parameters = params;

        HashSize = (byte) params.HashSize;
        if (HashSize < 64)
        {
            _mask = (1L << HashSize) - 1;
        }

        CreateTable();
    }

    public long Calc(byte[] data, int offset, int length)
    {
        long init = Parameters.RefOut ? CrcHelper.ReverseBits(Parameters.Init, HashSize) : Parameters.Init;
        long hash = ComputeCrc(init, data, offset, length);
        return (hash ^ Parameters.XorOut) & _mask;
    }

    private long ComputeCrc(long init, byte[] data, int offset, int length)
    {
        long crc = init;

        if (Parameters.RefOut)
        {
            for (int i = offset; i < offset + length; i++)
            {
                crc = (_table[(int)((crc ^ data[i]) & 0xFF)] ^ (crc >>> 8));
                crc &= _mask;
            }
        }
        else
        {
            int toRight = (HashSize - 8);
            toRight = toRight < 0 ? 0 : toRight;
            for (int i = offset; i < offset + length; i++)
            {
                crc = (_table[(int)(((crc >> toRight) ^ data[i]) & 0xFF)] ^ (crc << 8));
                crc &= _mask;
            }
        }

        return crc;
    }

    private void CreateTable()
    {
        for (int i = 0; i < _table.length; i++)
            _table[i] = CreateTableEntry(i);
    }

    private long CreateTableEntry(int index)
    {
        long r = (long)index;

        if (Parameters.RefIn)
            r = CrcHelper.ReverseBits(r, HashSize);
        else if (HashSize > 8)
            r <<= (HashSize - 8);

        long lastBit = (1L << (HashSize - 1));

        for (int i = 0; i < 8; i++)
        {
            if ((r & lastBit) != 0)
                r = ((r << 1) ^ Parameters.Poly);
            else
                r <<= 1;
        }

        if (Parameters.RefOut)
            r = CrcHelper.ReverseBits(r, HashSize);

        return r & _mask;
    }
}
