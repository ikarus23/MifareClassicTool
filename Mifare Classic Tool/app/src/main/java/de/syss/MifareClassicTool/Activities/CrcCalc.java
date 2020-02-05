package de.syss.MifareClassicTool.Activities;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

import crccalc.Crc16;
import crccalc.Crc32;
import crccalc.Crc64;
import crccalc.Crc8;
import crccalc.Main;
import de.syss.MifareClassicTool.R;

public class CrcCalc extends Activity {

    TextView textView;
    String text = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crc_calc);
        textView = findViewById(R.id.textView);
        text += Main.Calculate(Crc8.Params) + "\n";
        text += Main.Calculate(Crc16.Params) + "\n";
        text += Main.Calculate(Crc32.Params) + "\n";
        text += Main.Calculate(Crc64.Params) + "\n";
        textView.setText(text);
    }

}
