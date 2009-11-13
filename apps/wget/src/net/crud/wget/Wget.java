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
		
		// If the Activity is killed for an orientation change while the wget binary
		// is running, we pass the WgetTask object along.
		WgetTask task = (WgetTask) getLastNonConfigurationInstance();
		if (task != null) {
			task.resume(this);
			mWgetTask = task;
			showKillButton();
		} else {
			showRunButton();
		}
	}
	
    
	@Override
	public Object onRetainNonConfigurationInstance() {
		if (mWgetTask != null) {
			mWgetTask.pause();
			return mWgetTask;
		}
		return null;
	}

	public void addOutputLine(String line) {
		TextView tv = (TextView) findViewById(R.id.output);
		tv.append(line);
		final ScrollView sc = (ScrollView) findViewById(R.id.scrollview);
		
		// Don't scroll to the bottom if the user has scrolled back a ways to look at something.
		// We should only auto scroll if the view is already at the bottom of the scrolling element		
		if (sc.getScrollY() + sc.getHeight() >= tv.getHeight()) {
			// Put this on the UI thread queue so the text view re-renders before we
			// try to scroll.
			// Otherwise we fire too soon and there is no additional space to scroll
			// to!
			sc.post(new Runnable() {
				public void run() {
					sc.smoothScrollBy(0, 1000); // Arbitrary number greater than
					// line height
				}
			});
		}
	    
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
		tv.setText("");
		
		try {
			this.copyBinary("wget");
		} catch (IOException e) {
			tv.setText(e.toString());
		}

		InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(tv.getWindowToken(), 0);

		// This is an AsyncTask that executes the wget binary and copies stdout
		// into 'tv'.
		mWgetTask = new WgetTask(this);
		mWgetTask.execute(wget + " " + args);
	}

	public void showRunButton() {
		Button run = (Button) findViewById(R.id.engage);
		Button kill = (Button) findViewById(R.id.kill);

        kill.setVisibility(View.INVISIBLE);
		run.setVisibility(View.VISIBLE);
	}

	public void showKillButton() {
		Button run = (Button) findViewById(R.id.engage);
		Button kill = (Button) findViewById(R.id.kill);

        kill.setVisibility(View.VISIBLE);
		run.setVisibility(View.INVISIBLE);
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

//	public void onRestart() {
//		super.onRestart();
//		Log.d("wget", "onRestart");
//	}
//	public void onStart() {
//		super.onStart();
//		Log.d("wget", "onStart");
//	}
//	public void onResume() {
//		super.onRestart();
//		Log.d("wget", "onResume");
//	}
//	public void onPause() {
//		super.onPause();
//		Log.d("wget", "onPause");
//	}
//	public void onStop() {
//		super.onStop();
//		Log.d("wget", "onStop");
//	}
//	public void onDestroy() {
//		super.onDestroy();
//		Log.d("wget", "onDestroy");
//	}
}