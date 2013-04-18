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


package de.syss.MifareClassicTool.Activitys;

import android.os.Bundle;
import android.view.View;
import de.syss.MifareClassicTool.R;

/**
 * Decode Mifare Classic Value Blocks from their hex format
 * to an integer and vice versa (encode).
 * @author Gerhard Klostermeier
 */
public class ValueBlockToolActivity extends BasicActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_value_block_tool);
    }

    // TODO: Implement & doc.
    public void onDecode(View view) {

    }

    // TODO: Implement & doc.
    public void onEncode(View view) {

    }

    // TODO: Implement & doc.
    public void onCopyToClipboard(View view) {

    }

    // TODO: Implement & doc.
    public void onPasteFromClipboard(View view) {

    }
}
