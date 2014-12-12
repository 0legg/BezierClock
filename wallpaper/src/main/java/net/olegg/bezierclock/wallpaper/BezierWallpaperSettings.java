package net.olegg.bezierclock.wallpaper;

import android.os.Bundle;
import android.preference.PreferenceActivity;

import net.olegg.bezierclock.R;

public class BezierWallpaperSettings extends PreferenceActivity {
    public static final String BACKGROUND = "background";
    public static final String FOREGROUND = "foreground";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);
    }
}
