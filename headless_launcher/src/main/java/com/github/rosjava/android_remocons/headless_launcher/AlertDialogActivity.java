package com.github.rosjava.android_remocons.headless_launcher;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class AlertDialogActivity extends Activity implements View.OnClickListener {
    private String installPackage = "";
    private String mainContext = "";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alert_dialog);
        findViewById(R.id.yes).setOnClickListener(this);
        findViewById(R.id.no).setOnClickListener(this);

        installPackage = getIntent().getExtras().getString("InstallPackageName");
        mainContext = getIntent().getExtras().getString("MainContext");

        TextView text_view;
        text_view = (TextView)this.findViewById(R.id.main_context);
        text_view.setText(mainContext);
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()){
            case R.id.yes:
                Uri uri = Uri.parse("market://details?id=" + installPackage);
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                AlertDialogActivity.this.startActivity(intent);
                finish();
                break;
            case R.id.no:
                finish();
                break;
            default:
                break;
        }
    }
}
