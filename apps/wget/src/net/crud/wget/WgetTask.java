package net.crud.wget;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

import android.os.AsyncTask;
import android.util.Log;
import android.widget.Button;

public class WgetTask extends AsyncTask<String, String, Boolean> {
	Wget parent;
	int mPid;
	Method mCreateSubprocess;
	Method mWaitFor;
	Button mRunButton;
	Button mKillButton;
    // These variables "belong" to the UI thread and should only be accessed from there.
	// This stores all the text we've received so far
	ArrayList<String> thusFar;
	// This stores the lines that have arrived since pause() was called but before unpause() was called.
    ArrayList<String> bufferedLines;
    boolean isPaused;
    
	WgetTask(Wget p) {
		parent = p;
		mPid = -1;
		isPaused = false;
		bufferedLines = new ArrayList<String>();
		thusFar = new ArrayList<String>();
	}
	
	// Call from UI thread.
	void pause() {
		// Pause the actual wget process so we don't get runaway crawls running unnoticed in the background, draining battery.
		signalWget(19); // SIGSTOP
	    isPaused = true;
	}
	
	// Call from UI thread.
	void resume(Wget activity) {
		// Signal the wget process to resume
		signalWget(18); // SIGCONT
	    isPaused = false;
	    parent = activity;
	    for (String line : thusFar) {
	    	publishProgress(line);
	    }
	    while (bufferedLines.size() > 0) {
	    	String line = bufferedLines.remove(0);
	    	// We use publishProgress instead of directly adding to the buffer because
	    	// potentially we could have a large number of lines and don't want to freeze
	    	// the UI.
	    	publishProgress(line);
	    }
	}

	protected Boolean doInBackground(String... command) {
		// Inspired by
		// http://remotedroid.net/blog/2009/04/13/running-native-code-in-android/
		Class<?> execClass;
		try {
			execClass = Class.forName("android.os.Exec");
			mCreateSubprocess = execClass.getMethod("createSubprocess",
				String.class, String.class, String.class, int[].class);
			mWaitFor = execClass.getMethod("waitFor",	int.class);

			try {
				String one_line;
	
				String working_dir = "/sdcard/wget";
				// Use a working directory on the sdcard
				File dir = new File(working_dir);
				dir.mkdir();
	
	
				Log.d("wget", "Executing '" + command + "'");
	
				// Executes the command.
				// NOTE: createSubprocess() is asynchronous.
				// 'exec' is key here, otherwise killing this pid will only kill the
				// shell, and wget will go background.
				int[] pids = new int[1];
				FileDescriptor fd = (FileDescriptor) mCreateSubprocess.invoke(null,
						"/system/bin/sh", "-c", "cd " + working_dir + "; exec "
								+ command[0], pids);
				mPid = pids[0];
	
				// Reads stdout.
				// NOTE: You can write to stdin of the command using new
				// FileOutputStream(fd).
				FileInputStream in = new FileInputStream(fd);
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(in), 8096);
	
				// Old-style, has obnoxious buffering behaviour, don't know why.
				// Process proc = Runtime.getRuntime().exec(command[0], null, dir);
				// DataInputStream in = new DataInputStream(proc.getInputStream());
				// BufferedReader reader = new BufferedReader(new
				// InputStreamReader(proc.getInputStream()));
	
				while ((one_line = reader.readLine()) != null) {
					publishProgress(one_line + "\n");
				}
			} catch (IOException e1) {
				// Hacky: When the input fd is closed, instead of returning null
				// from the next readLine()
				// call, it seems that IOException is thrown. So we ignore it.
			}
			
	        mWaitFor.invoke(null, mPid);
	        mPid = -1;
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e.getMessage());
		} catch (SecurityException e) {
			throw new RuntimeException(e.getMessage());
		} catch (NoSuchMethodException e) {
			throw new RuntimeException(e.getMessage());
		} catch (IllegalArgumentException e) {
			throw new RuntimeException(e.getMessage());
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e.getMessage());
		} catch (InvocationTargetException e) {
			throw new RuntimeException(e.getMessage());
		}
		
		return true;
	}

	public void killWget() {
		signalWget(2);  // SIGINT is generally sufficient
	}
	
	// Runs on UI thread, so keep it short
	public void signalWget(int signal) {
		if (mPid != -1) {
			try {
				int[] pids = new int[1]; 
				mCreateSubprocess.invoke(null, "/system/bin/sh", "-c",
						"exec kill -" + signal + " " + mPid, pids);
                mWaitFor.invoke(null, pids[0]);
				publishProgress("\nProcess " + mPid + " killed by user\n");
			} catch (IllegalArgumentException e) {
			} catch (IllegalAccessException e) {
			} catch (InvocationTargetException e) {
				// If we catch an exception trying to run kill, don't worry
				// about it much.
				// Wget will die on its own, eventually (probably)
			}
		}
	}

	protected void onProgressUpdate(String... progress) {
		String line = progress[0];
		if (!this.isPaused) {
			this.thusFar.add(line);
			this.parent.addOutputLine(line);
		} else {
		    this.bufferedLines.add(line);
		}
	}

	protected void onPostExecute(Boolean result) {
		onProgressUpdate("\nfinished\n");
		parent.showRunButton();
	}

	protected void onCancelled() {
		parent.showRunButton();
	}

	protected void onPreExecute() {
		parent.showKillButton();
	}

	public boolean running() {
		return mPid != -1;
	}

}
