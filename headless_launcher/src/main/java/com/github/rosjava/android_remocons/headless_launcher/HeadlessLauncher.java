package com.github.rosjava.android_remocons.headless_launcher;

import android.app.Activity;
import android.os.Bundle;

public class HeadlessLauncher extends Activity
{
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
    }
}

