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

import java.io.IOException;

import android.annotation.SuppressLint;
import android.nfc.tech.NfcA;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import de.syss.MifareClassicTool.Common;
import de.syss.MifareClassicTool.R;

// TODO: implement & doc.
public class NfcACommandsToolActivity extends BasicActivity {

    EditText mCommandText;
    Button mCommandButton;
    ToggleButton mConnectToggleButton;
    TextView mLogText;
    Button mTimeoutButton;
    /**
     * If not null, this is a connected RFID-reader for ISO 14443A tags.
     */
    NfcA mNfcA;

    private static final String LOG_TAG =
            NfcACommandsToolActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nfca_commands_tool);

        mCommandText = (EditText) findViewById(
                R.id.editTextNfcACommandsToolCommand);
        mCommandButton = (Button) findViewById(R.id.buttonNfcACommandsToolSend);
        mTimeoutButton = (Button) findViewById(
                R.id.buttonNfcACommandsToolTimeout);
        mConnectToggleButton = (ToggleButton) findViewById(
                R.id.toggleButtonNfcACommandsToolConnect);
        mLogText = (TextView) findViewById(R.id.textViewNfcACommandsToolLog);
    }

    /**
     * Disconnect (close) {@link #mNfcA} from tag.
     */
    @Override
    public void onStop() {
        super.onStop();
        if (mNfcA != null && mNfcA.isConnected()) {
            try {
                mNfcA.close();
                mNfcA = null;
            } catch (IOException e) {
                Log.d(LOG_TAG, "Error while closing NfcA tag");
            }
        }
    }

    // TODO: implement & doc.
    @SuppressLint("NewApi")
    public void onSendCommand(View view) {
        String command = mCommandText.getText().toString();
        if (command.equals("")) {
            // Error. There is no command.
            // TODO: implement error message.
            return;
        }
        // TODO: distinguish API versions.
        if (command.length() <= mNfcA.getMaxTransceiveLength()) {
            try {
                byte[] tagAnswer = mNfcA.transceive(
                        Common.hexStringToByteArray(command));
                // TODO: Do not use fixed strings. This is just for testing.
                mLogText.setText("Command: " + command + "\n" + "Answer: "
                        + Common.byte2HexString(tagAnswer) + "\n" + mLogText.getText());

            } catch (IOException e) {
                // Error.
                // TODO: implement.
            }
        }
    }

    /**
     * Connect or disconnect {@link #mNfcA}.
     * @param view The View object that triggered the method
     * (in this case the connect/disconnect button).
     */
    public void onToggleConnect(View view) {
        if (mConnectToggleButton.isChecked()) {
            // Connect.
            if (Common.getTag() == null) {
                // Error. There is no tag.
                Toast.makeText(this, R.string.info_no_nfca_tag_found,
                        Toast.LENGTH_LONG).show();
                mConnectToggleButton.setChecked(false);
                return;
            }
            mNfcA = NfcA.get(Common.getTag());
            try {
                mNfcA.connect();
                mTimeoutButton.setEnabled(true);
                mCommandButton.setEnabled(true);
            } catch (Exception e) {
                // Error. Maybe there is no tag.
                Toast.makeText(this, R.string.info_no_nfca_tag_found,
                        Toast.LENGTH_LONG).show();
                mNfcA = null;
                mConnectToggleButton.setChecked(false);
            }
        } else {
            // Disconnect.
            try {
                mNfcA.close();
            } catch (IOException e) {
                Log.d(LOG_TAG, "Error while closing NfcA tag");
            }
            mNfcA = null;
            mTimeoutButton.setEnabled(false);
            mCommandButton.setEnabled(false);
        }
    }

    // TODO implement & doc.
    public void onChangeTimeout(View view) {

    }

}
