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
import android.nfc.tech.MifareClassic;
import android.nfc.tech.NfcA;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
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
        .setIcon(android.R.drawable.ic_dialog_alert)
        .setPositiveButton(R.string.button_ok,
                new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                // Do nothing.
            }
         })
         .show();
    }

    /**
     * Update and display the tag information.
     * If there is no Mifare Classic support, a warning will be shown.
     * @param tag A Tag from an NFC Intent.
     */
    private void updateTagInfos(Tag tag) {

        if (tag != null) {
            mLayout.removeAllViews();
            // Display generic info.
            // Create views and add them to the layout.
            TextView headerGenericInfo = new TextView(this);
            headerGenericInfo.setText(Common.colorString(
                    getString(R.string.text_generic_info),
                    getResources().getColor(R.color.blue)));
            headerGenericInfo.setBackgroundColor(
                    getResources().getColor(R.color.dark_gray));
            headerGenericInfo.setTextAppearance(this,
                    android.R.style.TextAppearance_Large);
            headerGenericInfo.setGravity(Gravity.CENTER_HORIZONTAL);
            final float scale = getResources().getDisplayMetrics().density;
            int pad = (int) (5 * scale + 0.5f); // 5dp to px
            headerGenericInfo.setPadding(pad, pad, pad, pad);
            mLayout.addView(headerGenericInfo);
            TextView genericInfo = new TextView(this);
            genericInfo.setPadding(pad, pad, pad, pad);
            genericInfo.setTextAppearance(this,
                    android.R.style.TextAppearance_Medium);
            mLayout.addView(genericInfo);
            // Get generic info and set these as text.
            String uid = Common.byte2HexString(tag.getId());
            NfcA nfca = NfcA.get(tag);
            String atqa = Common.byte2HexString(nfca.getAtqa());
            String sak = "" + nfca.getSak();
            int hc = getResources().getColor(R.color.light_green);
            genericInfo.setText(TextUtils.concat(
                    Common.colorString(getString(R.string.text_uid) + ":", hc),
                    "\n  ", uid, "\n",
                    Common.colorString(getString(
                            R.string.text_rf_tech) + ":", hc),
                    // Tech is always ISO 14443a due to NFC Intet filter.
                    "\n  ", getString(R.string.text_rf_tech_14a), "\n",
                    Common.colorString(getString(R.string.text_atqa) + ":", hc),
                    "\n  ", atqa, "\n",
                    Common.colorString(getString(R.string.text_sak) + ":", hc),
                    "\n  ", sak));

            // Check for Mifare Classic support.
            if (Arrays.asList(tag.getTechList()).contains(
                    "android.nfc.tech.MifareClassic")) {
                // Display Mifare Classic info.
                // Create views and add them to the layout.
                TextView headerMifareInfo = new TextView(this);
                headerMifareInfo.setText(Common.colorString(
                        getString(R.string.text_mf_info),
                        getResources().getColor(R.color.blue)));
                headerMifareInfo.setBackgroundColor(
                        getResources().getColor(R.color.dark_gray));
                headerMifareInfo.setTextAppearance(
                        this, android.R.style.TextAppearance_Large);
                headerMifareInfo.setGravity(Gravity.CENTER_HORIZONTAL);
                headerMifareInfo.setPadding(pad, pad, pad, pad);
                mLayout.addView(headerMifareInfo);
                TextView mifareInfo = new TextView(this);
                mifareInfo.setPadding(pad, pad, pad, pad);
                mifareInfo.setTextAppearance(this,
                        android.R.style.TextAppearance_Medium);
                mLayout.addView(mifareInfo);

                // Get Mifare info and set these as text.
                MifareClassic mfc = MifareClassic.get(tag);
                String size = "" + mfc.getSize();
                String sectorCount = "" + mfc.getSectorCount();
                String blockCount = "" + mfc.getBlockCount();
                mifareInfo.setText(TextUtils.concat(
                        Common.colorString(getString(
                                R.string.text_mem_size) + ":", hc),
                        "\n  ", size, " Byte\n",
                        Common.colorString(getString(
                                R.string.text_block_size) + ":", hc),
                        // Block size is always 16 Byte on Mifare Classic Tags.
                        "\n  ", "" + MifareClassic.BLOCK_SIZE, " Byte\n",
                        Common.colorString(getString(
                                R.string.text_sector_count) + ":", hc),
                        "\n  ", sectorCount, "\n",
                        Common.colorString(getString(
                                R.string.text_block_count) + ":", hc),
                        "\n  ", blockCount));
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
