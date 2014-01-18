/*
 * Copyright 2014 Gerhard Klostermeier
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
import android.view.View;
import de.syss.MifareClassicTool.R;


// TODO: doc.
/**
 *
 * @author Gerhard Klostermeier
 */
public class PreferencesActivity extends BasicActivity {

    // TODO: doc.
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preferences);
    }

    // TODO: implement & doc.
    public void onShowAutoReconnectInfo(View view) {

    }

    // TODO: implement & doc.
    public void onSave(View view) {

    }

    /**
     * Exit the preferences view without saving anything.
     * @param view The View object that triggered the method
     * (in this case the cancel button).
     */
    public void onCancel(View view) {
        finish();
    }
}