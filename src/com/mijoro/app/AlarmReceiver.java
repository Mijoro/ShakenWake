package com.mijoro.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;
import android.os.Bundle;

public class AlarmReceiver extends BroadcastReceiver {

 @Override
 public void onReceive(Context context, Intent intent) {
   try {
	 Log.d("Shakenwake", "Broadcast received");
	 
     Bundle bundle = intent.getExtras();
     String message = bundle.getString("motd");
     long alarmTime 	= bundle.getLong("alarmTime");

     Intent newIntent = new Intent(context, AlarmActivity.class);
     newIntent.putExtra("motd", message);
     newIntent.putExtra("alarmTime", alarmTime);
     newIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
     context.startActivity(newIntent);
     
    } catch (Exception e) {
     Toast.makeText(context, "There was an error somewhere, but we still received an alarm", Toast.LENGTH_SHORT).show();
     e.printStackTrace();

    }
 }

}