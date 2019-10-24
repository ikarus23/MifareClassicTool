package crccalc;

import android.util.Log;

public class Main {

    public static void main(String[] args) {
        Check(Crc8.Params);

        Check(Crc16.Params);

        Check(Crc32.Params);

        Check(Crc64.Params);
    }

    private static void Check(AlgoParams[] params) {
        for (AlgoParams param : params) {
            CrcCalculator calculator = new CrcCalculator(param);
            long result = calculator.Calc(CrcCalculator.TestBytes, 0, CrcCalculator.TestBytes.length);
            Log.i(calculator.Parameters.Name, Long.toHexString(result).toUpperCase());
            if (result != calculator.Parameters.Check)
                Log.d("LOG:", calculator.Parameters.Name + " - BAD ALGO!!! " + Long.toHexString(result).toUpperCase());
        }
    }

    public static StringBuilder Calculate(AlgoParams[] params) {
        StringBuilder res = new StringBuilder();
        for (AlgoParams param : params) {
            CrcCalculator calculator = new CrcCalculator(param);
            long result = calculator.Calc(CrcCalculator.TestBytes, 0, CrcCalculator.TestBytes.length);
            if (result != calculator.Parameters.Check) {
                Log.d("LOG:", calculator.Parameters.Name + " - BAD ALGO!!! " + Long.toHexString(result).toUpperCase());
            }
            res.append(String.format("%s : %s", calculator.Parameters.Name, Long.toHexString(result).toUpperCase())).append("\n");
        }
        return res;
    }
}