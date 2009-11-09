package net.crud.wget;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import net.crud.wget.R;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

public class Wget extends Activity {
	private static final String OPTIONS = "options";
	private static final String URLS = "urls";
	WgetTask mWgetTask;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		// The options and urls fields are saved across invocations of the
		// application.
		// They are only stored if the user hits 'Run wget'.
		SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
		String options = prefs.getString(OPTIONS, "-E -H  -N -k -p");
		String urls = prefs.getString(URLS, "google.com");

		EditText url_field = (EditText) findViewById(R.id.UrlField);
		url_field.setText(urls);
		EditText options_field = (EditText) findViewById(R.id.OptionsField);
		options_field.setText(options);

		final Button engage = (Button) findViewById(R.id.engage);
		engage.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				runWget();
			}
		});

		final Button kill = (Button) findViewById(R.id.kill);
		kill.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if (mWgetTask != null) {
					mWgetTask.killWget();
				}
			}
		});

		// When the user hits 'enter', consider that the same as pressing the
		// 'engage' button.
		url_field
				.setOnEditorActionListener(new TextView.OnEditorActionListener() {
					@Override
					public boolean onEditorAction(TextView v, int actionId,
							KeyEvent event) {
						runWget();
						return true;
					}
				});

		Button help = (Button) findViewById(R.id.HelpButton);

		help.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				startActivity(new Intent(Wget.this, WgetHelp.class));

			}
		});

		Button about = (Button) findViewById(R.id.AboutButton);
		about.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				startActivity(new Intent(Wget.this, WgetAbout.class));
			}
		});
	}

	public void runWget() {
		String base = this.getApplicationContext().getFilesDir().getParent();
		String wget = base + "/wget";

		// Get the flags and URLs from the input fields, and save them for future use.
		String options = ((EditText) findViewById(R.id.OptionsField)).getText()
				.toString();
		String url = ((EditText) findViewById(R.id.UrlField)).getText()
				.toString();
		SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putString(OPTIONS, options);
		editor.putString(URLS, url);
		editor.commit();

		String args = options + " " + url;

		TextView tv = (TextView) findViewById(R.id.output);
		ScrollView sv = (ScrollView) findViewById(R.id.scrollview);

		try {
			this.copyBinary("wget");
		} catch (IOException e) {
			tv.setText(e.toString());
		}

		InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(tv.getWindowToken(), 0);

		Button run = (Button) findViewById(R.id.engage);
		Button kill = (Button) findViewById(R.id.kill);

		// This is an AsyncTask that executes the wget binary and copies stdout
		// into 'tv'.
		mWgetTask = new WgetTask(tv, sv, run, kill);
		mWgetTask.execute(wget + " " + args);
	}

	private void copyBinary(String filename) throws IOException {
		String base = this.getApplicationContext().getFilesDir().getParent();
		String outFileName = base + "/" + filename;
		File outFile = new File(outFileName);
		if (!new File(outFileName).exists()) {
			Log.d("wget", "Extracting " + filename + " to " + outFileName);
			InputStream is = this.getAssets().open(filename);
			byte buf[] = new byte[1024];
			int len;
			OutputStream out = new FileOutputStream(outFile);
			while ((len = is.read(buf)) > 0) {
				out.write(buf, 0, len);
			}
			out.close();
			is.close();
			String[] cmd = { "/system/bin/chmod", "0755", outFileName };

			Runtime.getRuntime().exec(cmd);
		}
	}

}