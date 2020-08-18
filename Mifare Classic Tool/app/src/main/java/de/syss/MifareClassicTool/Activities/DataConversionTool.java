/*
 * Copyright 2020 Gerhard Klostermeier
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */


package de.syss.MifareClassicTool.Activities;

import android.os.Bundle;
import android.widget.EditText;

import de.syss.MifareClassicTool.R;

// TODO: doc.
public class DataConversionTool extends BasicActivity {

    EditText mAscii;
    EditText mHex;
    EditText mBin;
    EditText mByte;
    EditText mShort;
    EditText mInt;
    EditText mLong;
    
    // TODO: doc.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_convert_tool);
        mAscii = findViewById(R.id.editTextDataConversionToolAscii);
        mHex = findViewById(R.id.editTextDataConversionToolHex);
        mBin = findViewById(R.id.editTextDataConversionToolBin);
    }
}
