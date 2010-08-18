package com.mijoro.app;

import java.io.IOException;
import java.util.Calendar;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources.NotFoundException;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnErrorListener;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class AlarmActivity extends Activity implements OnErrorListener {
	static String LOGTAG = "Shakenwake";
	private TextView alarmTimeBlinker;
	private Calendar alarmCal;
	private String motd;
	private static int MIN_SHAKE_TIME = 3 * 1000;
	private boolean skipShake = false;
	public Context that;
	public SharedPreferences prefs;
	private MediaPlayer alarmMP;
	private SensorManager mSensorManager;
	
	//Shaking Vars
	private boolean isShaking = false;
	private Long startedShakingAt;
	private boolean needMoreShaking = true;
	private boolean onBreakFromShake = false;
	private Long timeOfLastShake;
	
	private final SensorEventListener mSensorListener = new SensorEventListener() {
		private float mAccel = 0.00f; // acceleration apart from gravity
		private float mAccelCurrent = SensorManager.GRAVITY_EARTH; // current acceleration including gravity
		private float mAccelLast = SensorManager.GRAVITY_EARTH; // last acceleration including gravity

		public void onSensorChanged(SensorEvent se) {
			float x = se.values[0];
			float y = se.values[1];
			float z = se.values[2];
			mAccelLast = mAccelCurrent;
			mAccelCurrent = (float) Math.sqrt((double) (x * x + y * y + z * z));
			float delta = mAccelCurrent - mAccelLast;
			mAccel = mAccel * 0.9f + delta; // perform low-cut filter

			System.out.println("Accel: " + mAccel);

			if (mAccel > 2) {
				shaking();
			} else {
				notShaking();
			}
		}

		public void onAccuracyChanged(Sensor sensor, int accuracy) {
		}
	};
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Log.d(LOGTAG, "Alarm has responded");
		
		// Build UI
		setContentView(R.layout.shakescreen);

		// Get alarm info passed in from alarmManager
		this.motd = getIntent().getStringExtra("motd");

		Log.d(LOGTAG, "Motd: " + this.motd);
		
		Long alarmTime = getIntent().getLongExtra("alarmTime", 0);
		this.alarmCal = Calendar.getInstance();
		this.alarmCal.setTimeInMillis(alarmTime);

		// Build nice-formatted time string for when alarm was set
		String ampm = (this.alarmCal.get(Calendar.AM_PM) == 0) ? "AM" : "PM";
		String smin = "";
		int intMin = this.alarmCal.get(Calendar.MINUTE);
		String digitFix = (intMin < 10) ? "0" : "";
		smin = digitFix + intMin;
		int hour = this.alarmCal.get(Calendar.HOUR);

		String niceAlarmTime = hour + ":" + smin + " " + ampm;

		// Display that nicely formatted time string
		this.alarmTimeBlinker = (TextView) this
				.findViewById(R.id.alarmTimeBlinker);
		this.alarmTimeBlinker.setText(niceAlarmTime);

		prefs = PreferenceManager.getDefaultSharedPreferences(this);

		startAlarm();

		// Start listening for shaking
		if (this.skipShake == false) {
			/* do this in onCreate */
			mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
			mSensorManager.registerListener(mSensorListener, mSensorManager
					.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
					SensorManager.SENSOR_DELAY_NORMAL);
		} else {
			enoughShaking();
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (skipShake == false) {
			mSensorManager.registerListener(mSensorListener, mSensorManager
					.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
					SensorManager.SENSOR_DELAY_NORMAL);
		}
	}

	@Override
	protected void onStop() {

		if (skipShake == false)
			mSensorManager.unregisterListener(mSensorListener);
		super.onStop();
	}

	public void shaking() {
		if (this.isShaking == false) {
			this.isShaking = true;
			this.startedShakingAt = System.currentTimeMillis();

			startedShaking();
		}

		if (this.onBreakFromShake) {
			this.onBreakFromShake = false;
		}

		if (System.currentTimeMillis() - this.startedShakingAt > MIN_SHAKE_TIME
				&& this.needMoreShaking) {
			enoughShaking();
		}
	}

	public void notShaking() {
		if (this.isShaking) {

			if (this.onBreakFromShake) {
				if (System.currentTimeMillis() - this.timeOfLastShake > 1000) {
					this.onBreakFromShake = false;
					this.isShaking = false;

					stoppedShaking();
				}
			} else {
				this.onBreakFromShake = true;
				this.timeOfLastShake = System.currentTimeMillis();
			}
		}
	}

	public void stoppedShaking() {
		Toast.makeText(this, "Stopped shaking :(", Toast.LENGTH_SHORT).show();
	}

	public void startedShaking() {
		makeQuieter();
		Toast.makeText(this, "Started Shakign! :D", Toast.LENGTH_SHORT).show();
	}

	public void enoughShaking() {
		if (this.skipShake == false) {
			mSensorManager.unregisterListener(mSensorListener);
		}

		stopAlarm();

		showMotd();
	}

	public void makeQuieter() {
		AudioManager mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
		int curVolume = mAudioManager
				.getStreamVolume(AudioManager.STREAM_MUSIC);
		mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC,
				(int) (curVolume / 2) % 1, AudioManager.FLAG_VIBRATE);
	}

	public void stopAlarm() {
		Log.v(LOGTAG, "Stopping alarm");

		alarmMP.stop();
		alarmMP.release();

		alarmMP = MediaPlayer.create(this, R.raw.coin);
		alarmMP.setLooping(false);
		alarmMP.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
			public void onCompletion(MediaPlayer mp) {
				alarmMP.release();
				alarmMP = null;
			}
		});
		alarmMP.start();
	}

	public boolean onError(MediaPlayer mediaPlayer, int what, int extra) {
		Log.e(LOGTAG, "onError--->   what:" + what + "    extra:" + extra);
		if (mediaPlayer != null) {
			mediaPlayer.stop();
			mediaPlayer.release();
		}

		return true;
	}

	public void startAlarm() {
		Log.v(LOGTAG, "Starting alarm");

		alarmMP = new MediaPlayer();
		try {
			alarmMP.setDataSource(this.getResources().openRawResourceFd(
					R.raw.beep).getFileDescriptor());
			alarmMP.setOnErrorListener(this);
			alarmMP.setAudioStreamType(AudioManager.STREAM_ALARM);

			// Set loud volume
			AudioManager mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
			int maxVolume = mAudioManager
					.getStreamMaxVolume(AudioManager.STREAM_ALARM);
			mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC,
					(int) ((maxVolume * .75) % 1), AudioManager.FLAG_VIBRATE);

			// Set looping and start
			alarmMP.setLooping(true);
			alarmMP.prepare();
			alarmMP.start();
			return;
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (NotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		//TODO What do we do for the error case if the media player doesn't work?
	}

	public void showMotd() {
		TextView motdText = (TextView) findViewById(R.id.motd);
		motdText.setText(this.motd);

		LinearLayout shakeLayout = (LinearLayout) findViewById(R.id.shakeLayout);
		LinearLayout motdLayout = (LinearLayout) findViewById(R.id.motdLayout);

		// Enable Layout 2 and Disable Layout 1
		shakeLayout.setVisibility(View.GONE);
		motdLayout.setVisibility(View.VISIBLE);

		Button closeMotdBtn = (Button) findViewById(R.id.closeMotdBtn);
		closeMotdBtn.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				saveMotd(motd);
				Log.d("Shakenwake", "Committing lastMotd to memory: " + motd);
				finish();
			}
		});
	}

	public void saveMotd(String motd) {
		// Increment the counter
		// Log.d(LOGTAG,
		// "prefs namespace: "+PreferenceManager.setDefaultValues(context,
		// resId, readAgain).)
		SharedPreferences.Editor editor = prefs.edit();
		editor.putString("lastMotd", motd);
		editor.putBoolean("fresh", true);
		editor.putBoolean("quitAllTheWay", true);
		editor.commit();

		Log.d(LOGTAG, "lastMotd saved as: " + motd);

		String tmpStr = prefs.getString("lastMotd", "");
		Log.d(LOGTAG, "lastMotd read as: " + tmpStr);
	}
}
