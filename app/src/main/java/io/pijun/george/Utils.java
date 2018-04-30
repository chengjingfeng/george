package io.pijun.george;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Looper;
import android.support.annotation.AnyThread;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.annotation.UiThread;
import android.support.v7.app.AlertDialog;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@SuppressWarnings("WeakerAccess")
public final class Utils {

    public static Map<String, Object> map(Object... args) {
        if (args.length % 2 != 0) {
            throw new IllegalArgumentException("You need to provide an even number of arguments. (Keys and values)");
        }
        HashMap<String, Object> map = new HashMap<>();
        for (int i=0; i<args.length; i++) {
            map.put((String)args[i], args[i+1]);
            i += 1;
        }

        return map;
    }

    @SuppressLint("WrongThread")
    @AnyThread
    public static void showAlert(final Context ctx, @StringRes final int titleId, @StringRes final int msgId) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            App.runOnUiThread(() -> _showAlert(ctx, titleId, msgId));
            return;
        }

        _showAlert(ctx, titleId, msgId);
    }

    @UiThread
    private static void _showAlert(Context ctx, @StringRes int titleId, @StringRes int msgId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(ctx, R.style.AlertDialogTheme);
        if (titleId != 0) {
            builder.setTitle(titleId);
        }
        if (msgId != 0) {
            builder.setMessage(msgId);
        }
        builder.setPositiveButton(R.string.ok, null);
        builder.show();
    }

    @SuppressLint("WrongThread")
    @AnyThread
    public static void showStringAlert(final Context ctx, final CharSequence title, final CharSequence msg) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            App.runOnUiThread(() -> _showStringAlert(ctx, title, msg));
            return;
        }

        _showStringAlert(ctx, title, msg);
    }

    @UiThread
    private static void _showStringAlert(final Context ctx, CharSequence title, CharSequence msg) {
        AlertDialog.Builder builder = new AlertDialog.Builder(ctx, R.style.AlertDialogTheme);
        if (title != null) {
            builder.setTitle(title);
        }
        builder.setMessage(msg);
        builder.setPositiveButton(R.string.ok, null);
        builder.show();
    }

    public static int pixToDps(Context ctx, int pixs) {
        float scale = ctx.getResources().getDisplayMetrics().density;
        return (int)((pixs - 0.5f)/scale);
    }

    public static int dpsToPix(Context ctx, int dps) {
        float scale = ctx.getResources().getDisplayMetrics().density;
        return (int) (dps * scale + 0.5f);
    }

    private static final Pattern sUsernamePattern = Pattern.compile("^[a-z0-9]{5,}$");
    @AnyThread
    public static boolean isValidUsername(String username) {
        if (username == null) {
            return false;
        }
        return sUsernamePattern.matcher(username.toLowerCase()).matches();
    }

    @StringRes
    public static int getInvalidUsernameReason(@Nullable String username) {
        if (username == null) {
            return R.string.username_missing;
        }
        String lc = username.toLowerCase(Locale.US);
        // Check if it's valid, before making any other assumptions.
        if (sUsernamePattern.matcher(lc).matches()) {
            return 0;
        }

        if (lc.length() < 5) {
            return R.string.too_short;
        }

        return R.string.invalid_characters_msg;
    }

    public static boolean isValidEmail(String email) {
        String[] parts = email.split("@");
        if (parts.length != 2) {
            return false;
        }

        String local = parts[0];
        if (local.length() == 0 || local.length() > 64) {
            return false;
        }

        // check if we have the format of a domain
        String domain = parts[1];
        if (domain.length() == 0 || domain.length() > 255) {
            return false;
        }
        String[] domainParts = domain.split("\\.");
        if (domainParts.length < 2) {
            return false;
        }

        if (domainParts[domainParts.length-1].length() < 2) {
            return false;
        }
        return true;
    }
}
