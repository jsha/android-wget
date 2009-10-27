package net.crud.Wget;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import net.crud.Wget.R.id;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

@SuppressWarnings("unused")
public class Wget extends Activity {

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.main);
		
		Button engage = (Button) findViewById(R.id.engage);
		engage.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				runWget();
			}
		});

		// When the user hits 'enter', consider that the same as pressing the 'engage' button.
		EditText url_field = (EditText) findViewById(R.id.UrlField);
		url_field.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				runWget();
				return true;
			}
		});		
		Button help = (Button) findViewById(R.id.HelpButton);
		final Context context = this.getApplicationContext();
		help.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				startActivity(new Intent(
                        Wget.this, WgetHelp.class));

			}
		});
		Button about = (Button) findViewById(R.id.AboutButton);
		about.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				startActivity(new Intent(
                        Wget.this, WgetAbout.class));
			}
		});
		
	}

	public void runWget() {
		String base = this.getApplicationContext().getFilesDir().getParent();
		String wget = base + "/wget";

		String options = ((EditText) findViewById(R.id.OptionsField)).getText()
				.toString();
		String url = ((EditText) findViewById(R.id.UrlField)).getText()
				.toString();

		String args = options + " " + url;
		String[] cmd = { "/system/bin/sh", "-c", wget + " " + args };
		Log.d("wget", "Executing '" + wget + " " + args + "'");

		TextView tv = (TextView) findViewById(R.id.output);
		ScrollView sv = (ScrollView) findViewById(R.id.scrollview);

		try {
			this.copyBinary("wget");
		} catch (IOException e) {
		  	tv.setText(e.toString());
		}
		
		new WgetTask(tv, sv).execute(wget + " " + args);
	}

	private void copyBinary(String filename) throws IOException {
		String base = this.getApplicationContext().getFilesDir().getParent();
		String outFileName = base + "/" + filename;
		File outFile = new File(outFileName);
		if (!new File(outFileName).exists()) {
			try {
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

				Process proc = Runtime.getRuntime().exec(cmd);
			} catch (IOException e) {
				throw e;
			}
		}
	}

}