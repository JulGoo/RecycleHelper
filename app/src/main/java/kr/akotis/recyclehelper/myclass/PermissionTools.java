package kr.akotis.recyclehelper.myclass;

import android.Manifest;

public class PermissionTools {
    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final String[] REQUIRED_PERMISSIONS = new String[]{
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
    };



}
