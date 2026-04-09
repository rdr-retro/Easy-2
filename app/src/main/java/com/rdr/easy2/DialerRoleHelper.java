package com.rdr.easy2;

import android.app.role.RoleManager;
import android.content.Context;
import android.os.Build;

public final class DialerRoleHelper {
    private DialerRoleHelper() {
    }

    public static boolean isDialerRoleHeld(Context context) {
        if (context == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return false;
        }

        RoleManager roleManager = context.getSystemService(RoleManager.class);
        return roleManager != null
                && roleManager.isRoleAvailable(RoleManager.ROLE_DIALER)
                && roleManager.isRoleHeld(RoleManager.ROLE_DIALER);
    }
}
