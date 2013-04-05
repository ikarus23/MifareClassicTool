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

import android.content.Intent;
import android.nfc.Tag;
import android.os.Bundle;
import de.syss.MifareClassicTool.Common;
import de.syss.MifareClassicTool.R;

/**
 * Display tag info like technology, size, sector count, etc.
 * This is the only thing a user can do with a device that does not support
 * Mifare Classic.
 * @author Gerhard Klostermeier
 */
public class TagInfoActivity extends BasicActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tag_info);
    }

    /**
     * Call {@link Common#treatAsNewTag(Intent, android.content.Context)} and
     * then call {@link #updateTagInfos(Tag)}
     */
    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        updateTagInfos(Common.getTag());
    }

    // TODO: Implement & doc.
    private void updateTagInfos(Tag tag) {

    }
}
