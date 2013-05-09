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

import java.util.Arrays;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
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
public class TagInfoToolActivity extends BasicActivity {

    LinearLayout mLayout;

    /**
     * Calls {@link #updateTagInfos(Tag)}.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tag_info_tool);

        mLayout = (LinearLayout) findViewById(R.id.LinearLayoutTagInfoTool);
        updateTagInfos(Common.getTag());
    }

    /**
     * Calls {@link Common#treatAsNewTag(Intent, android.content.Context)} and
     * then calls {@link #updateTagInfos(Tag)}
     */
    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(intent.getAction())) {
            updateTagInfos(Common.getTag());
        }
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
            // Check for Mifare Classic support.
            boolean isMifareClassic = Arrays.asList(tag.getTechList()).contains(
                    MifareClassic.class.getName());

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
            int pad = (int) (5 * scale + 0.5f); // 5dp to px.
            headerGenericInfo.setPadding(pad, pad, pad, pad);
            mLayout.addView(headerGenericInfo);
            TextView genericInfo = new TextView(this);
            genericInfo.setPadding(pad, pad, pad, pad);
            genericInfo.setTextAppearance(this,
                    android.R.style.TextAppearance_Medium);
            mLayout.addView(genericInfo);
            // Get generic info and set these as text.
            String uid = Common.byte2HexString(tag.getId());
            int uidLen = tag.getId().length;
            uid += " (" + uidLen + " byte";
            if (uidLen == 7) {
                uid += ", CL2";
            } else if (uidLen == 10) {
                uid += ", CL3";
            }
            uid += ")";
            NfcA nfca = NfcA.get(tag);
            // Swap ATQA to match the common order like shown here:
            // http://nfc-tools.org/index.php?title=ISO14443A
            byte[] atqaBytes = nfca.getAtqa();
            atqaBytes = new byte[] {atqaBytes[1], atqaBytes[0]};
            String atqa = Common.byte2HexString(atqaBytes);
            String sak = Common.byte2HexString(
                    new byte[] {(byte)nfca.getSak()});
            String ats = "-";
            IsoDep iso = IsoDep.get(tag);
            if (iso != null ) {
                ats = Common.byte2HexString(iso.getHistoricalBytes());
            }
            // Identify tag type.
            int tagTypeResourceID = getTagIdentifier(atqa, sak, ats);
            String tagType = "";
            if (tagTypeResourceID == R.string.tag_unknown && isMifareClassic) {
                tagType = getString(R.string.tag_unknown_mf_classic);
            } else {
                tagType = getString(tagTypeResourceID);
            }

            int hc = getResources().getColor(R.color.light_green);
            genericInfo.setText(TextUtils.concat(
                    Common.colorString(getString(R.string.text_uid) + ":", hc),
                    "\n", uid, "\n",
                    Common.colorString(getString(
                            R.string.text_rf_tech) + ":", hc),
                    // Tech is always ISO 14443a due to NFC Intet filter.
                    "\n", getString(R.string.text_rf_tech_14a), "\n",
                    Common.colorString(getString(R.string.text_atqa) + ":", hc),
                    "\n", atqa, "\n",
                    Common.colorString(getString(R.string.text_sak) + ":", hc),
                    "\n", sak, "\n",
                    Common.colorString(getString(
                            R.string.text_ats) + ":", hc),
                    "\n", ats, "\n",
                    Common.colorString(getString(
                            R.string.text_tag_type_and_manuf) + ":", hc),
                    "\n", tagType));

            // Add message that the tag type might be wrong.
            if (tagTypeResourceID != R.string.tag_unknown) {
                TextView tagTypeInfo = new TextView(this);
                tagTypeInfo.setPadding(pad, 0, pad, pad);
                tagTypeInfo.setText(
                        "(" + getString(R.string.text_tag_type_guess) + ")");
                mLayout.addView(tagTypeInfo);
            }

            LinearLayout layout = (LinearLayout) findViewById(
                    R.id.LinearLayoutTagInfoToolSupport);
            // Check for Mifare Classic support.
            if (isMifareClassic) {
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
                        "\n", size, " byte\n",
                        Common.colorString(getString(
                                R.string.text_block_size) + ":", hc),
                        // Block size is always 16 byte on Mifare Classic Tags.
                        "\n", "" + MifareClassic.BLOCK_SIZE, " byte\n",
                        Common.colorString(getString(
                                R.string.text_sector_count) + ":", hc),
                        "\n", sectorCount, "\n",
                        Common.colorString(getString(
                                R.string.text_block_count) + ":", hc),
                        "\n", blockCount));
                layout.setVisibility(View.GONE);
            } else {
                // No Mifare Classic Support.
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

    /**
     * Get (determine) the tag type resource ID from ATQA + SAK + ATS.
     * If no resource is found check for the tag type only on ATQA + SAK
     * (and then on ATQA only).
     * @param atqa The ATQA from the tag.
     * @param sak The SAK from the tag.
     * @param ats The ATS from the tag.
     * @return The resource ID.
     */
    private int getTagIdentifier(String atqa, String sak, String ats) {
        String prefix = "tag_";
        ats = ats.replace("-", "");

        // First check on ATQA + SAK + ATS.
        int ret = getResources().getIdentifier(
                prefix + atqa + sak + ats, "string", getPackageName());

        if (ret == 0) {
            // Check on ATQA + SAK.
            ret = getResources().getIdentifier(
                    prefix + atqa + sak, "string", getPackageName());
        }

        if (ret == 0) {
            // Check on ATQA.
            ret = getResources().getIdentifier(
                    prefix + atqa, "string", getPackageName());
        }

        if (ret == 0) {
            // No match found return "Unknown".
            return R.string.tag_unknown;
        }
        return ret;
    }
}
