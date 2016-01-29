package de.syss.MifareClassicTool.helpers;

import android.content.Context;
import android.content.pm.PackageManager;
import android.support.v4.content.ContextCompat;

/**
 * Created by mjurinic on 29.01.16..
 */
public final class PermissionChecker {

    public static boolean hasWritePermission(Context context) {
        return (ContextCompat.checkSelfPermission(context, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) ==
                PackageManager.PERMISSION_GRANTED;
    }
}
