/*
 * Copyright 2013 Gerhard Klostermeier
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
import android.view.View;
import android.widget.EditText;
import de.syss.MifareClassicTool.Common;
import de.syss.MifareClassicTool.R;

/**
 *
 * @author Gerhard Klostermeier
 */
public class AccessConditionTool extends BasicActivity {

    private EditText mAC;

    /**
     * Initialize the some member variables.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_access_condition_tool);

        mAC = (EditText) findViewById(R.id.editTextAccessConditionToolAC);
    }

    // TODO: Implement and doc.
    public void onDecode(View view) {

    }

    // TODO: Implement and doc.
    public void onEncode(View view) {

    }

    /**
     * Copy the Mifare Classic Access Conditions to the Android clipboard.
     * @param view The View object that triggered the method
     * (in this case the copy button).
     */
    public void onCopyToClipboard(View view) {
        Common.copyToClipboard(mAC.getText().toString(), this);
    }

    /**
     * Paste the content of the Android clipboard (if plain text) to the
     * access conditions edit text.
     * @param view The View object that triggered the method
     * (in this case the paste button).
     */
    public void onPasteFromClipboard(View view) {
        String text = Common.getFromClipboard(this);
        if (text != null) {
            mAC.setText(text);
        }
    }

}
