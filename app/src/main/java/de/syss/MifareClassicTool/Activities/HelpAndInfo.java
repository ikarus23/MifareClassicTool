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
import android.webkit.WebView;

import de.syss.MifareClassicTool.R;

/**
 * In-App (offline) help and info.
 * @author Gerhard Klostermeier
 */
public class HelpAndInfo extends BasicActivity {

    /**
     * Initialize the layout and the web view (browser) on local
     * help website.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);

        // Init. help from local website.
        WebView wv = findViewById(R.id.webViewHelpText);
        wv.loadUrl("file:///android_asset/help/help.html");
    }
}
