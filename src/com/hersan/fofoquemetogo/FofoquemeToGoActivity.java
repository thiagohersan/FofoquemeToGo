package com.hersan.fofoquemetogo;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Environment;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.Engine;
import android.telephony.SmsMessage;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.widget.Toast;
import com.hersan.fofoquemetogo.R;

import java.io.FileOutputStream;
import java.io.File;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.Locale;
import java.util.HashMap;
import java.util.Queue;
import java.util.Random;

public class FofoquemeToGoActivity extends Activity implements TextToSpeech.OnInitListener {

	// msg+number file name
	private static final String MSG_FILE_NAME = "FOFOQUEME";
	//
	private static final String[] PREPHRASE  = {"aiaiai aiai. ", "ui ui ui. ", "não acredito. ", "olha essa. ", "ouve só. ", "escuta essa. ", "meu deus. ", "relou. "};
	private static final String[] POSTPHRASE = {" . assim você me mata.", " . relou.", " . verdade.", " . nem me fale.", " . não me diga.", " . puts.", " . não não não.", " . que coisa.", " . pode creee.", " . pois é."};
	private static final String[] NONPHRASE  = {"só isso? ", "como assim? ", "aaaaaaiii que preguiça. "};

	private TextToSpeech myTTS = null;
	private boolean isTTSReady = false;
	private SMSReceiver mySMS = null;
	private OutputStreamWriter myFileWriter = null;
	private Random myRandom = null;

	// queue for messages
	private Queue<String> msgQueue = null;

	// listen for intent sent by broadcast of SMS signal
	// if it gets a new SMS
	//  clean it up a little bit and send to text reader
	public class SMSReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			Bundle bundle = intent.getExtras();
			SmsMessage[] msgs = null;

			if (bundle != null) {
				Object[] pdus = (Object[]) bundle.get("pdus");
				msgs = new SmsMessage[pdus.length];

				if (msgs.length > 0) {
					// read only the most recent
					msgs[0] = SmsMessage.createFromPdu((byte[]) pdus[0]);
					String message = msgs[0].getMessageBody().toString();
					String phoneNum = msgs[0].getOriginatingAddress().toString();
					System.out.println("!!! Client MainAction got sms: "+message);
					System.out.println("!!! from: "+phoneNum);

					// only write if it's from a real number
					if(phoneNum.length() > 5) {
						// clean up the @/# if it's there...
						message = message.replaceAll("[@#]?", "");

						// write number and msg to SD card
						try{
							if(myFileWriter != null){
								// setting up date strings
								Calendar calendar = Calendar.getInstance();
								SimpleDateFormat sdfD = new SimpleDateFormat("yyyyMMdd");
								SimpleDateFormat sdfT = new SimpleDateFormat("HHmmss");
								String dateTime = new String(sdfD.format(calendar.getTime()));
								dateTime = dateTime.concat(":::");
								dateTime = dateTime.concat(new String(sdfT.format(calendar.getTime())));
								dateTime = dateTime.concat(":::");

								String t = dateTime.concat(new String(phoneNum+":::"+message+"\n"));
								myFileWriter.append(new String(t.getBytes("UTF-8"), "UTF-8"));
								myFileWriter.flush();
							}
						}
						catch(Exception e){}

						// if message is short
						String[] words = message.split(" ");
						if(words.length < 3){
							//  only play the short message provocation if queue is empty !
							if((msgQueue.isEmpty() == true)&&(myTTS.isSpeaking() == false)){
								FofoquemeToGoActivity.this.playMessage(NONPHRASE[myRandom.nextInt(NONPHRASE.length)].concat("diga mais. "));
							}
						}
						// message longer than 3 words
						else {
							// if nothing is already happening, play message
							if((msgQueue.isEmpty() == true)&&(myTTS.isSpeaking() == false)){
								// push message onto queue... twice
								msgQueue.offer(message);
								msgQueue.offer(message);
								// immediately play it once
								FofoquemeToGoActivity.this.playMessage();
							}
							// else, something is playing or queue is not empty
							else{
								// push message (longer than 3 words) onto queue... twice
								msgQueue.offer(message);
								msgQueue.offer(message);
							}
						}
					}
				}
			}
		}
	}

	/** for creating a menu object */
	@Override
	public boolean onCreateOptionsMenu (Menu menu){
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(com.hersan.fofoquemetogo.R.menu.menu, menu);
		return true;
	}

	/** for handling menu item clicks */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case com.hersan.fofoquemetogo.R.id.quitbutton:
			finish();
			return true;
		default: 
			return false;
		}
	}


	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		// new text to speech, if needed
		myTTS = (myTTS == null)?(new TextToSpeech(this, this)):myTTS;
		myRandom = (myRandom == null)?(new Random()):myRandom;

		// new sms listener if needed
		mySMS = (mySMS == null)?(new SMSReceiver()):mySMS;

		// new message queue
		msgQueue = (msgQueue == null)?(new LinkedList<String>()):msgQueue;

		// register smsReceiver
		registerReceiver(mySMS, new IntentFilter("android.provider.Telephony.SMS_RECEIVED"));

		// start a file to save msg and phone numbers to
		try {
			File root = new File(Environment.getExternalStorageDirectory(), "Fofoqueme");
			if (!root.exists()) {
				root.mkdirs();
			}

			// setting up date strings
			Calendar calendar = Calendar.getInstance();
			SimpleDateFormat sdfD = new SimpleDateFormat("yyyyMMdd");
			String sdfds = new String(sdfD.format(calendar.getTime()));

			myFileWriter = new OutputStreamWriter(new FileOutputStream(new File(root, MSG_FILE_NAME+sdfds+".txt"), true), Charset.forName("UTF-8"));
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onResume() {
		System.out.println("!!!: from onResume");
		super.onResume();
		//registerReceiver(mySMS, new IntentFilter("android.provider.Telephony.SMS_RECEIVED"));
	}

	@Override
	protected void onPause() {
		System.out.println("!!!: from onPause");
		//unregisterReceiver(mySMS);
		super.onPause();
	}

	@Override
	public boolean onTouchEvent(MotionEvent event){
		System.out.println("!!!: from onTouch");
		if((event.getAction() == MotionEvent.ACTION_UP) && (isTTSReady)){
			// if msg queue is empty, start saying the message
			if((msgQueue.isEmpty() == true)&&(myTTS.isSpeaking() == false)){
				// Add the test message to queue ... twice...
				msgQueue.offer("Ai, se eu te pego");
				msgQueue.offer("Ai, se eu te pego");
				// immediately play it
				FofoquemeToGoActivity.this.playMessage();
			}
			// if queue not empty or playing something... only add to queue (twice) without playing it
			else {
				// Add the test message to queue 
				msgQueue.offer("Ai, se eu te pego");
				msgQueue.offer("Ai, se eu te pego");
			}
			return true;
		}
		return false;
	}

	/** Called when the activity is ending. */
	@Override
	public void onDestroy() {
		System.out.println("!!!: from onDestroy");
		if(myTTS != null){
			myTTS.shutdown();
		}
		// unregister sms Receiver
		unregisterReceiver(mySMS);

		// close BT Socket
		try{
			if(myFileWriter != null){
				myFileWriter.close();
			}
		}
		catch(Exception e){
		}
		super.onDestroy();
	}

	// from OnInitListener interface
	public void onInit(int status){
		System.out.println("!!!!! default engine: "+myTTS.getDefaultEngine());
		System.out.println("!!!!! default language: "+myTTS.getLanguage().toString());

		// set the package and language for tts
		//   these are the values for Luciana
		myTTS.setEngineByPackageName("com.svox.classic");
		myTTS.setLanguage(new Locale("pt_BR"));

		// slow her down a little...
		myTTS.setSpeechRate(0.66f);
		myTTS.setPitch(1.0f);

		// attach listener
		myTTS.setOnUtteranceCompletedListener(new TextToSpeech.OnUtteranceCompletedListener(){
			@Override
			public void onUtteranceCompleted (String utteranceId){
				// check if there are more messages to be said
				if(msgQueue.peek() != null){
					FofoquemeToGoActivity.this.playMessage();
				}
			}
		});

		System.out.println("!!!!! set lang: "+myTTS.getLanguage().toString());
		Toast.makeText(this, "TTS Lang: "+myTTS.getLanguage().toString(), Toast.LENGTH_SHORT ).show();

		isTTSReady = true;
	}


	/////////////////////////////
	// to be called when it is time to play a message
	//    assumes queue is not empty
	private void playMessage(){
		playMessage(null);
	}

	private void playMessage(String msg){
		// if pulling from the queue, modify message
		if(msg == null){
			msg = msgQueue.poll();
			// 2 in 10, add something to front
			int rInt = myRandom.nextInt(10); 
			if( rInt < 2){
				msg = PREPHRASE[myRandom.nextInt(PREPHRASE.length)].concat(msg);
			}
			// 2 in 10 add to back
			else if(rInt < 4){
				msg = msg.concat(POSTPHRASE[myRandom.nextInt(POSTPHRASE.length)]);
			}
			// 2 in 10, repeat longest word
			else if(rInt < 6){
				String foo = msg.replaceAll("[.!?]+", " ");
				String[] words = foo.split(" ");
				int longestWordInd = 0;
				for(int i=0; i<words.length; i++){
					if(words[i].length() > words[longestWordInd].length()){
						longestWordInd = i;
					}
				}
				msg = msg.replaceAll(words[longestWordInd], words[longestWordInd]+" "+words[longestWordInd]+" "+words[longestWordInd]);
			}
		}
		// else, msg = msg
		System.out.println("!!! speak: "+msg);

		myTTS.setPitch(1.5f*myRandom.nextFloat()+0.5f);  // [0.5, 2.0]
		HashMap<String,String> foo = new HashMap<String,String>();
		foo.put(Engine.KEY_PARAM_UTTERANCE_ID, "1234");
		// speak with a pause before and afterwards.
		myTTS.speak(". . "+msg+" . . ", TextToSpeech.QUEUE_ADD, foo);
	}

}