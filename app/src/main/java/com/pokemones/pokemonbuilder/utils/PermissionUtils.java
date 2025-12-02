package com.pokemones.pokemonbuilder.utils;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class PermissionUtils {
    public static final int REQ_PERMISSIONS_ALL = 5001;

    // Permisos que queremos solicitar al inicio
    private static final String[] REQUIRED = new String[] {
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.RECORD_AUDIO
    };

    public static boolean hasAllPermissions(Activity act) {
        for (String p : REQUIRED) {
            if (ContextCompat.checkSelfPermission(act, p) != PackageManager.PERMISSION_GRANTED) return false;
        }
        return true;
    }

    public static void requestCameraAndAudioPermissions(Activity act) {
        if (!hasAllPermissions(act)) {
            List<String> needed = new ArrayList<>();
            for (String p : REQUIRED) {
                if (ContextCompat.checkSelfPermission(act, p) != PackageManager.PERMISSION_GRANTED) {
                    needed.add(p);
                }
            }
            if (!needed.isEmpty()) {
                ActivityCompat.requestPermissions(act, needed.toArray(new String[0]), REQ_PERMISSIONS_ALL);
            }
        }
    }

    public static void handlePermissionsResult(Activity act, int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode != REQ_PERMISSIONS_ALL) return;
        boolean ok = true;
        if (grantResults == null || grantResults.length == 0) ok = false;
        else {
            for (int r : grantResults) if (r != PackageManager.PERMISSION_GRANTED) ok = false;
        }
        if (!ok) {
            Toast.makeText(act, "Permisos de cámara y audio son necesarios para la app", Toast.LENGTH_LONG).show();
        }
    }
}
