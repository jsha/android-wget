package net.crud.wget;


import android.os.Bundle;
import android.webkit.WebView;

import android.app.Activity;


public class WgetAbout extends Activity {
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.about);

		WebView wv = (WebView) findViewById(R.id.about_webview);
		wv.loadUrl("file:///android_asset/about.html");
	}
}
