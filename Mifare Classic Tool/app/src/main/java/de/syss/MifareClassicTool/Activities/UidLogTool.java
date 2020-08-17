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

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import de.syss.MifareClassicTool.Common;
import de.syss.MifareClassicTool.R;

public class UidLogTool extends BasicActivity {

    TextView mUidLog;

    // TODO: doc.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_uid_log_tool);
        mUidLog = findViewById(R.id.textViewUidLogToolUids);
        updateUidLog();
    }

    // TODO: doc.
    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        updateUidLog();
    }

    // TODO: doc.
    private void updateUidLog() {
        File log = new File(this.getFilesDir(),
                Common.HOME_DIR + File.separator + Common.UID_LOG_FILE);
        String[] logEntries = Common.readFileLineByLine(log, false, this);
        if (logEntries != null) {
            // Reverse order (newest top).
            ArrayList<String> tempEntries =
                    new ArrayList<String>(Arrays.asList(logEntries));
            Collections.reverse(tempEntries);
            mUidLog.setText(TextUtils.join(
                    System.getProperty("line.separator"), tempEntries));
        } else {
            // No log yet.
        }
    }

    // TODO: make log sharable.
    // TODO: add function to clear log.

}
