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

import java.util.Arrays;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.nfc.Tag;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import de.syss.MifareClassicTool.Common;
import de.syss.MifareClassicTool.R;

/**
 * Display tag info like technology, size, sector count, etc.
 * This is the only thing a user can do with a device that does not support
 * Mifare Classic.
 * @author Gerhard Klostermeier
 */
public class TagInfoActivity extends BasicActivity {

    LinearLayout mLayout;
    
    /**
     * Calls {@link #updateTagInfos(Tag)}.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tag_info);
        
        mLayout = (LinearLayout) findViewById(R.id.LinearLayoutTagInfo);
        updateTagInfos(Common.getTag());
    }

    /**
     * Calls {@link Common#treatAsNewTag(Intent, android.content.Context)} and
     * then calls {@link #updateTagInfos(Tag)}
     */
    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        updateTagInfos(Common.getTag());
    }
    
    /**
     * Show a dialog with further information.
     * @param view The View object that triggered the method
     * (in this case the read more button).
     */
    public void onReadMore(View view) {
        new AlertDialog.Builder(this)
        .setTitle(R.string.dialog_no_mfc_title)
        .setMessage(R.string.dialog_no_mfc)
        .setPositiveButton(R.string.button_ok,
                new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                // Do nothing.
            }
         })
         .show();
    }

    // TODO: doc.
    private void updateTagInfos(Tag tag) {
        
        if (tag != null) {
            mLayout.removeAllViews();
            // Display generic info.
            // TODO: Implement.
            
            // Check for Mifare Classic support.
            if (Arrays.asList(tag.getTechList()).contains(
                    "android.nfc.tech.MifareClassic")) {
                // Display Mifare Classic info.
                // TODO: Implement.
            } else {
                // No Mifare Classic Support.
                LinearLayout layout = (LinearLayout) findViewById(
                        R.id.LinearLayoutTagInfoSupport);
                layout.setVisibility(View.VISIBLE);
            }
        } else {
            // There is no Tag.
            TextView text = new TextView(this);
            text.setTextAppearance(this, android.R.style.TextAppearance_Large);
            text.setText(getString(R.string.text_no_tag));
            mLayout.removeAllViews();
            mLayout.addView(text);
            Toast.makeText(this, R.string.info_no_tag_found,
                    Toast.LENGTH_SHORT).show();
        }
    }
}
