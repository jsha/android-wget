package net.crud.wget;


import android.os.Bundle;
import android.webkit.WebView;

import android.app.Activity;


public class WgetHelp extends Activity {
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.help);

		WebView wv = (WebView) findViewById(R.id.help_webview);
		wv.loadUrl("file:///android_asset/help.html");
	}
}
