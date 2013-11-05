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

/**
 * Decode Mifare Classic Access Conditions from their hex format
 * to a more human readable format and vice versa.
 * @author Gerhard Klostermeier
 */
public class AccessConditionTool extends BasicActivity {

    private EditText mAC;
    /**
     * Matrix of access conditions bits (C1-C3) where the first
     * dimension is the "C" parameter (C1-C3, Index 0-2) and the second
     * dimension is the block number (Index 0-3).
     * This matrix will be updated each time a user chooses an Access Condition
     * via {@link #mDataBlockDialog} or {@link #mSectorTrailerDialog}
     */
    private byte[][] mACMatrix;
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
        // Init AC matrix with factory setting/transport configuration.
        mACMatrix = new byte[][] {
                {0, 0, 0, 0},
                {0, 0, 0, 0},
                {0, 0, 0, 1} };

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
                // Set Access Condition bits for this block.
                byte[] acBits = acRowNrToACBits(position);
                int blockNr = Integer.parseInt(
                        mSelectedButton.getTag().toString());
                mACMatrix[0][blockNr] = acBits [0];
                mACMatrix[1][blockNr] = acBits [1];
                mACMatrix[2][blockNr] = acBits [2];
                // Close dialog.
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
                // Set Access Condition bits for sector trailer.
                byte[] acBits = acRowNrToACBits(position);
                mACMatrix[0][3] = acBits [0];
                mACMatrix[1][3] = acBits [1];
                mACMatrix[2][3] = acBits [2];
                // Close dialog.
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

    /**
     * Convert the {@link #mACMatrix} to 3 Access Condition bytes using
     * {@link Common#acMatrixToACBytes(byte[][])} and display them.
     * @param view The View object that triggered the method
     * (in this case the encode button).
     * @see #mACMatrix
     * @see Common#acMatrixToACBytes(byte[][])
     */
    public void onEncode(View view) {
        mAC.setText(Common.byte2HexString(Common.acMatrixToACBytes(mACMatrix)));
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

    /**
     * Convert the the row number of the Access Condition table to its
     * corresponding access bits C1, C2 and C3
     * (see: res/values/access_conditions.xml and
     * NXP's MF1S50yyX, Chapter 8.7.1, Table 7 and 8).
     * @param rowNr The row number of the Access Condition table (0-7).
     * @return The access bits C1, C2 and C3. On error null will be returned.
     */
    private byte[] acRowNrToACBits(int rowNr) {
        switch (rowNr) {
        case 0:
            return new byte[] {0, 0, 0};
        case 1:
            return new byte[] {0, 1, 0};
        case 2:
            return new byte[] {1, 0, 0};
        case 3:
            return new byte[] {1, 1, 0};
        case 4:
            return new byte[] {0, 0, 1};
        case 5:
            return new byte[] {0, 1, 1};
        case 6:
            return new byte[] {1, 0, 1};
        case 7:
            return new byte[] {1, 1, 1};
        default:
            // Error.
            return null;
        }
    }

    /**
     * Convert the access bits C1, C2 and C3 to its corresponding row number
     * in the Access Condition table (see: res/values/access_conditions.xml and
     * NXP's MF1S50yyX, Chapter 8.7.1, Table 7 and 8).
     * @param acBits The access bits C1, C2 and C3.
     * @return The row number of the Access Condition table. On error -1 will
     * be returned.
     */
    private int acBitsToACRowNr(byte[] acBits) {
        if (acBits != null && acBits.length != 3) {
            return -1;
        }
        if (acBits[0] == 0 && acBits[1] == 0 && acBits[2] == 0) {
            return 0;
        } else if (acBits[0] == 0 && acBits[1] == 1 && acBits[2] == 0) {
            return 1;
        } else if (acBits[0] == 1 && acBits[1] == 0 && acBits[2] == 0) {
            return 2;
        } else if (acBits[0] == 1 && acBits[1] == 1 && acBits[2] == 0) {
            return 3;
        } else if (acBits[0] == 0 && acBits[1] == 0 && acBits[2] == 1) {
            return 4;
        } else if (acBits[0] == 0 && acBits[1] == 1 && acBits[2] == 1) {
            return 5;
        } else if (acBits[0] == 1 && acBits[1] == 0 && acBits[2] == 1) {
            return 6;
        } else if (acBits[0] == 1 && acBits[1] == 1 && acBits[2] == 1) {
            return 7;
        }

        // Error.
        return -1;
    }

}
