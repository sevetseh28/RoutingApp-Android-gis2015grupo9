package com.example.hernan.examplegis.test;

import com.example.hernan.examplegis.MainActivity;
import com.robotium.solo.*;
import android.test.ActivityInstrumentationTestCase2;


public class maximum_speed_and_buffer extends ActivityInstrumentationTestCase2<MainActivity> {
  	private Solo solo;
  	
  	public maximum_speed_and_buffer() {
		super(MainActivity.class);
  	}

  	public void setUp() throws Exception {
        super.setUp();
		solo = new Solo(getInstrumentation());
		getActivity();
  	}
  
   	@Override
   	public void tearDown() throws Exception {
        solo.finishOpenedActivities();
        super.tearDown();
  	}
  
	public void testRun() {
        //Wait for activity: 'com.example.hernan.examplegis.MainActivity'
		solo.waitForActivity(com.example.hernan.examplegis.MainActivity.class, 2000);
        //Click on ImageView
		solo.clickOnView(solo.getView(android.widget.ImageButton.class, 0));
        //Sleep for 3060 milliseconds
		solo.sleep(3060);
        //Click on Mis rutas
		solo.clickOnText(java.util.regex.Pattern.quote("Mis rutas"));
        //Wait for activity: 'com.example.hernan.examplegis.RutasActivity'
		assertTrue("com.example.hernan.examplegis.RutasActivity is not found!", solo.waitForActivity(com.example.hernan.examplegis.RutasActivity.class));
        //Sleep for 1932 milliseconds
		solo.sleep(1932);
        //Click on ruta
		solo.clickOnView(solo.getView(android.R.id.text1));
        //Sleep for 817 milliseconds
		solo.sleep(817);
        //Click on CARGAR RUTA
		solo.clickOnView(solo.getView(com.example.hernan.examplegis.R.id.buttonCargarRuta));
        //Wait for activity: 'com.example.hernan.examplegis.MapaActivity'
		assertTrue("com.example.hernan.examplegis.MapaActivity is not found!", solo.waitForActivity(com.example.hernan.examplegis.MapaActivity.class));
        //Sleep for 3068 milliseconds
		solo.sleep(3068);
        //Click on Iniciar marcha
		solo.clickOnView(solo.getView(com.example.hernan.examplegis.R.id.buttonIniciarRecorrido));
        //Set progress on SeekBar
		solo.setProgressBar((android.widget.ProgressBar) solo.getView(com.example.hernan.examplegis.R.id.seekBar), 4000);
        //Sleep for 726 milliseconds
		solo.sleep(726);
        //Set progress on SeekBar
		solo.setProgressBar((android.widget.ProgressBar) solo.getView(com.example.hernan.examplegis.R.id.seekBarBuffer), 30000);
        //Sleep for 2 minutes
		solo.sleep(120000);
        //Press menu back key
		solo.goBack();
        //Sleep for 1777 milliseconds
		solo.sleep(1777);
        //Press menu back key
		solo.goBack();
	}
}
