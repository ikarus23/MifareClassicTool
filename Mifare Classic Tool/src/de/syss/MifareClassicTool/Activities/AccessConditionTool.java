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

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import de.syss.MifareClassicTool.Common;
import de.syss.MifareClassicTool.R;

// TODO: Icon.
/**
 * Decode Mifare Classic Access Conditions from their hex format
 * to a more human readable format and vice versa.
 * @author Gerhard Klostermeier
 */
public class AccessConditionTool extends BasicActivity {

    private EditText mAC;
    /**
     * The last clicked "choose Access Conditions for data block"-button.
     * @see #onChooseACforDataBock(View)
     */
    private Button mSelectedButton;
    /**
     * A dialog which allow the user to choose between all possible
     * Access Conditions for a data block.
     */
    private AlertDialog mDataBlockDialog;
    /**
     * A dialog which allow the user to choose between all possible
     * Access Conditions for a Sector Trailer.
     */
    private AlertDialog mSectorTrailerDialog;

    /**
     * Build the two dialogs for choosing the Access Conditions
     * ({@link #mDataBlockDialog} and {@link #mSectorTrailerDialog}) and
     * initialize the some member variables.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_access_condition_tool);

        // Init. member vars.
        mAC = (EditText) findViewById(R.id.editTextAccessConditionToolAC);

        // Build the dialog with Access Conditions for data blocks.
        String[] items = new String[8];
        for (int i = 0; i < 8; i++) {
            items[i] = getString(getResourceByACNumber(i, true));
        }
        ListAdapter adapter = new ArrayAdapter<String>(
                this, R.layout.list_item_small_text, items);
        ListView lv = new ListView(this);
        lv.setAdapter(adapter);
        lv.setOnItemClickListener(
                new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, final View view,
                    int position, long id) {
                // Change button text to selected Access Conditions.
                mSelectedButton.setText(getString(
                        getResourceByACNumber(position, true)));
                mDataBlockDialog.dismiss();
            }
        });
        mDataBlockDialog =  new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_choose_ac_title)
                .setView(lv)
                .create();

        // Build the dialog with Access Conditions for the Sector Trailer.
        items = new String[8];
        for (int i = 0; i < 8; i++) {
            items[i] = getString(getResourceByACNumber(i, false));
        }
        adapter = new ArrayAdapter<String>(
                this, R.layout.list_item_small_text, items);
        lv = new ListView(this);
        lv.setAdapter(adapter);
        final Button sectorTrailerButton = (Button) findViewById(
                R.id.buttonAccessConditionToolBlock3);
        lv.setOnItemClickListener(
                new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, final View view,
                    int position, long id) {
                // Change button text to selected Access Conditions.
                sectorTrailerButton.setText(getString(
                        getResourceByACNumber(position, false)));
                mSectorTrailerDialog.dismiss();
            }
        });
        mSectorTrailerDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_choose_ac_title)
                .setView(lv)
                .create();
    }

    // TODO: Implement and doc.
    public void onDecode(View view) {

    }

    // TODO: Implement and doc.
    public void onEncode(View view) {

    }

    /**
     * Backup the button which triggered this method to {@link #mSelectedButton}
     * to change its text later and show the Access Condition chooser dialog
     * for data blocks ({@link #mDataBlockDialog}).
     * @param view The View object that triggered the method
     * (in this case one of the data block buttons).
     * @see #mDataBlockDialog
     * @see #mSelectedButton
     */
    public void onChooseACforDataBock(View view) {
        mSelectedButton = (Button) view;
        mDataBlockDialog.show();
    }

    /**
     * Show the Access Condition chooser dialog for Sector Trailers
     * ({@link #mSectorTrailerDialog}).
     * @param view The View object that triggered the method
     * (in this case the Sector Trailer button).
     * @see #mSectorTrailerDialog
     */
    public void onChooseACforSectorTrailer(View view) {
        mSectorTrailerDialog.show();
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

    /**
     * Return the resource ID of an Access Condition string based on its
     * position in the table (see: res/values/access_conditions.xml and
     * NXP's MF1S50yyX, Chapter 8.7.1, Table 7 and 8).
     * @param acNumber Row number of the Access Condition in the table.
     * @param isDataBlock True for data blocks, False for Sector Trailers
     * (True = Table 8, False = Table 7).
     * @return The resource ID of an Access Condition string.
     */
    private int getResourceByACNumber(int acNumber, boolean isDataBlock) {
        String prefix = "ac_data_block_";
        if (!isDataBlock) {
            prefix = "ac_sector_trailer_";
        }
        return getResources().getIdentifier(
                prefix + acNumber, "string", getPackageName());
    }

}
