package com.mijoro.app;

import java.util.Calendar;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.Dialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.View;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.TimePicker;

import com.mijoro.app.AlarmReceiver;
import com.snaptic.api.SnapticAPI;
import com.snaptic.api.SnapticNote;

public class SetAlarmActivity extends Activity {
	static final int TIME_DIALOG_ID = 0;
	public static String LOGTAG = "Shakenwake";

	private TimePickerDialog.OnTimeSetListener mTimeSetListener;
	private Calendar newAlarm;
	public String lastMotd;
	public SharedPreferences prefs;
	public int checkedMotdResponse;

	private Button setTimeBtn;
	private TextView alarmSetFor;
	public TextView lastMotdTV;
	public EditText commentsTV;
	public RadioGroup mRadioGroup;
	private Button setAlarmBtn;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(LOGTAG, "Testing logging");

		this.newAlarm = Calendar.getInstance();
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		
		setContentView(R.layout.main);
		this.alarmSetFor = (TextView) this.findViewById(R.id.alarmSetFor);
		this.setTimeBtn = (Button) this.findViewById(R.id.setTimeBtn);
		this.setAlarmBtn = (Button) findViewById(R.id.setUserAlarm);
		
		this.setTimeBtn.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				showDialog(TIME_DIALOG_ID);
			}
		});
		this.setAlarmBtn.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				setUserAlarm();
			}
		});
		this.mTimeSetListener = new TimePickerDialog.OnTimeSetListener() {
			public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
				Log.d(LOGTAG, "Time has been set, passing to handler");
				SetAlarmActivity.this.updateAlarmTime(hourOfDay, minute);
			}
		};

		// Increment the counter
		Boolean freshPref = prefs.getBoolean("fresh", false);
		lastMotd = prefs.getString("lastMotd", "");
		Log.d(LOGTAG, "lastMotd at start: " + lastMotd);
		Log.d(LOGTAG, "Fresh at start? " + freshPref);
		if (freshPref && lastMotd != "") {
			showLastMotdRecap();
		}
	}

	public void showLastMotdRecap() {
		LinearLayout alarmSetLayout 	= (LinearLayout) findViewById(R.id.alarmSetLayout);
		LinearLayout motdRecapLayout 	= (LinearLayout) findViewById(R.id.motdRecapLayout);
		lastMotdTV 						= (TextView) findViewById(R.id.lastMotd);
		commentsTV 						= (EditText) findViewById(R.id.comments);
		mRadioGroup 					= (RadioGroup) findViewById(R.id.group1);
		Button saveRecapBtn 			= (Button) findViewById(R.id.saveRecapBtn);
		
		lastMotdTV.setText(lastMotd);

		mRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				checkedMotdResponse = checkedId;
			}
		});
		saveRecapBtn.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				saveRecap();
			}
		});

		// Enable Layout 2 and Disable Layout 1
		alarmSetLayout.setVisibility(View.GONE);
		motdRecapLayout.setVisibility(View.VISIBLE);
	}

	public void hideLastMotdRecap() {
		LinearLayout alarmSetLayout = (LinearLayout) findViewById(R.id.alarmSetLayout);
		LinearLayout motdRecapLayout = (LinearLayout) findViewById(R.id.motdRecapLayout);

		// Enable Layout 2 and Disable Layout 1
		alarmSetLayout.setVisibility(View.VISIBLE);
		motdRecapLayout.setVisibility(View.GONE);
	}

	public void updateAlarmTime(int hourOfDay, int minute) {
		Log.d(LOGTAG, "Setting time on newAlarm");

		this.newAlarm.set(Calendar.HOUR_OF_DAY, hourOfDay);
		this.newAlarm.set(Calendar.MINUTE, minute);

		Log.d(LOGTAG, "Building new time string");

		String ampm = (this.newAlarm.get(Calendar.AM_PM) == 0) ? "AM" : "PM";
		String smin = "";

		int intMin = this.newAlarm.get(Calendar.MINUTE);
		if (intMin < 10) {
			smin = "0" + intMin;
		} else {
			smin = Integer.toString(intMin);
		}

		String textTime =
			this.newAlarm.get(Calendar.HOUR) + ":" + smin + " " + ampm;

		Log.d(LOGTAG, "Setting timeString into display");
		this.alarmSetFor.setText("Alarm will sound at: " + textTime);
	}

	public void setUserAlarm() {
		Log.d(LOGTAG, "Beginning to set alarm.");

		EditText motd 		= (EditText) findViewById(R.id.motd);
		AlarmManager am 	= (AlarmManager) getSystemService(ALARM_SERVICE);
		TextView alarmIsSet = (TextView) findViewById(R.id.alarmIsSet);

		Intent intent = new Intent(this, AlarmReceiver.class);
		intent.putExtra("motd", motd.getText().toString());
		intent.putExtra("alarmTime", this.newAlarm.getTimeInMillis());
		// TODO turn 192837 into a static const.
		PendingIntent sender = PendingIntent.getBroadcast(this, 192837, intent,
				PendingIntent.FLAG_UPDATE_CURRENT);

		am.set(AlarmManager.RTC_WAKEUP, newAlarm.getTimeInMillis(), sender);

		
		alarmIsSet.setText("The alarm has been set.");

		motd.setText("");
		Log.d(LOGTAG, "Alarm has been set. ");
		this.alarmSetFor.setText("Waiting for next alarm...");

		clearLastMotd();
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case TIME_DIALOG_ID:
			return new TimePickerDialog(this, mTimeSetListener, 8, 0, false);
		}
		return null;
	}

	public void recapLastMotd() {

	}

	public void saveRecap() {
		String comments = (String) commentsTV.getText().toString();

		RadioButton yesBtn = (RadioButton) findViewById(R.id.option1);
		int yesID = yesBtn.getId();

		String mUsername = "jfeldstein";
		String mPassword = "quantum";
		SnapticAPI mApi = new SnapticAPI(mUsername, mPassword);

		SnapticNote note = new SnapticNote();

		// Set the attributes you care about
		note.text = "Your message to yourself: \n" + lastMotd + "\n\n";
		if (yesID == checkedMotdResponse) {
			note.text = note.text + "Did you follow through? Yes. \n\n";
		} else {
			note.text = note.text + "Did you follow through? No. \n\n";
		}
		note.text = note.text + "What do you have to say for yourself? \n "
				+ comments + "\n\n";
		if (yesID == checkedMotdResponse) {
			note.text = note.text + "#win";
		} else {
			note.text = note.text + "#fail";
		}

		Log.d(LOGTAG, "Note = " + note.text);

		int returnCode = mApi.addNote(note);

		if (returnCode != SnapticAPI.RESULT_OK) {
			Log.d(LOGTAG, "Error:" + SnapticAPI.resultToString(returnCode));
		}

		hideLastMotdRecap();
		clearLastMotd();
	}

	public void clearLastMotd() {
		Log.d(LOGTAG, "Clearing lastMotd");

		// Increment the counter
		SharedPreferences.Editor editor = prefs.edit();
		editor.putString("lastMotd", "");
		editor.commit(); // Very important

		Log.d(LOGTAG, "Cleared lastMotd");
	}

	@Override
	protected void onResume() {
		super.onResume();
		// Quit, if coming back from shake-success screen
		Boolean freshPref = prefs.getBoolean("quitAllTheWay", false);
		if (freshPref) {
			Log.d(LOGTAG, "Main screen has received fresh prefs, quitting");
			SharedPreferences.Editor editor = prefs.edit();
			editor.putBoolean("quitAllTheWay", false);
			editor.commit();
			finish();
		}
	}
}
