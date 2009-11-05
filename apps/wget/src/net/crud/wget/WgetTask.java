package net.crud.wget;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.os.AsyncTask;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

public class WgetTask extends AsyncTask<String, String, Boolean> {
	TextView mLogTarget;
    ScrollView mScroller;
    Button mRunButton;
    Button mKillButton;
    int mPid;
    Method mCreateSubprocess;
    Method mWaitFor;
    
	WgetTask(TextView tv, ScrollView sc, Button run, Button kill) {
		tv.setText("");
		mLogTarget = tv;
		mScroller = sc;
		mPid = -1;
	}

	protected Boolean doInBackground(String... command) {
		try {
			String one_line;

			String working_dir = "/sdcard/wget";
			// Use a working directory on the sdcard
			File dir = new File(working_dir);
			dir.mkdir();

			// Inspired by http://remotedroid.net/blog/2009/04/13/running-native-code-in-android/
			Class<?> execClass = Class.forName("android.os.Exec");
			Method createSubprocess = execClass.getMethod("createSubprocess",
			  String.class, String.class, String.class, int[].class);
			mCreateSubprocess = createSubprocess;
            mWaitFor = execClass.getMethod("waitFor", int.class);

			// Executes the command.
			// NOTE: createSubprocess() is asynchronous.
			int[] pids = new int[1];
			FileDescriptor fd = (FileDescriptor)createSubprocess.invoke(
			   null, "/system/bin/sh", "-c", "cd " + working_dir + ";" + command[0], pids);
	        mPid = pids[0];
	        
			// Reads stdout.
			// NOTE: You can write to stdin of the command using new FileOutputStream(fd).
			FileInputStream in = new FileInputStream(fd);
			BufferedReader reader = new BufferedReader(new InputStreamReader(in));

			//Old-style, has obnoxious buffering behaviour, don't know why.
            //Process proc = Runtime.getRuntime().exec(command[0], null, dir);
			//DataInputStream in = new DataInputStream(proc.getInputStream());
			//BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
			
			while ((one_line = reader.readLine()) != null) {
				publishProgress(one_line + "\n");
			}
		} catch (IOException e1) {
			// Hacky: When the input fd is closed, instead of returning null from the next readLine()
			// call, it seems that IOException is thrown.  So we ignore it.
			
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

	// Runs on UI thread
	protected void killWget() {
	    try {
		    mCreateSubprocess.invoke(
				   null, "/system/bin/sh", "-c", "kill -9 " + mPid, null);
		    publishProgress("Killed by user");
		// If we catch an exception trying to run kill, don't worry about it much.
		// Wget will die on its own, eventually (probably)
	    } catch (IllegalArgumentException e) {
		} catch (IllegalAccessException e) {
		} catch (InvocationTargetException e) {
		}
	}
	protected void onProgressUpdate(String... progress) {
		this.mLogTarget.append(progress[0]);
		final ScrollView sc = this.mScroller;
		// Put this on the UI thread queue so the text view re-renders before we try to scroll.
		// Otherwise we fire too soon and there is no additional space to scroll to!
        sc.post(new Runnable() {
            public void run() {
            	sc.smoothScrollBy(0, 1000); // Arbitrary number greater than line height
            }
        }); 
    }

	protected void onPostExecute(Integer result) {
	}

}
