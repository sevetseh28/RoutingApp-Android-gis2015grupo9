package com.example.hernan.examplegis.test;

import com.example.hernan.examplegis.MainActivity;
import com.robotium.solo.*;

import android.os.Trace;
import android.test.ActivityInstrumentationTestCase2;


public class generar_ruta_test extends ActivityInstrumentationTestCase2<MainActivity> {
  	private Solo solo;
  	
  	public generar_ruta_test() {
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
        //Click on Empty Text View
		solo.clickOnView(solo.getView(com.example.hernan.examplegis.R.id.searchView));
        //Sleep for 6094 milliseconds
		solo.sleep(6000);
		//Enter the text: 'miami'
		solo.enterText((android.widget.EditText) solo.getView("android:id/search_src_text"), "miami");
        //Press next button
		solo.pressSoftKeyboardNextButton();
		Trace.beginSection("T_BUSQUEDA_1");
        //Wait for activity: 'com.example.hernan.examplegis.SearchableActivity'
		assertTrue("com.example.hernan.examplegis.SearchableActivity is not found!", solo.waitForActivity(com.example.hernan.examplegis.SearchableActivity.class));
        Trace.endSection();
        //Sleep for 2866 milliseconds
		solo.sleep(1500);
        //Click on Miami, Florida, United States
		solo.clickOnView(solo.getView(android.R.id.text1));
        //Sleep for 3425 milliseconds
		solo.sleep(1500);
        //Click on Añadir parada
		solo.clickOnView(solo.getView(com.example.hernan.examplegis.R.id.buttonAddLoc));
        //Click on miami
		solo.clickOnView(solo.getView("android:id/search_src_text"));
        //Enter the text: 'los angeles ca'
		solo.clearEditText((android.widget.EditText) solo.getView("android:id/search_src_text"));
		solo.enterText((android.widget.EditText) solo.getView("android:id/search_src_text"), "los angeles ca");
        //Press next button
		solo.pressSoftKeyboardNextButton();
		Trace.beginSection("T_BUSQUEDA_2");
		//Wait for activity: 'com.example.hernan.examplegis.SearchableActivity'
		assertTrue("com.example.hernan.examplegis.SearchableActivity is not found!", solo.waitForActivity(com.example.hernan.examplegis.SearchableActivity.class));
		Trace.endSection();
		//Sleep for 1856 milliseconds
		solo.sleep(1000);
        //Click on Los Angeles, California, United States
		solo.clickOnView(solo.getView(android.R.id.text1));
        //Sleep for 668 milliseconds
		solo.sleep(668);
        //Click on Añadir parada
		solo.clickOnView(solo.getView(com.example.hernan.examplegis.R.id.buttonAddLoc));
        //Sleep for 1945 milliseconds
		solo.waitForDialogToClose();
        //Click on Guardar puntos
		Trace.beginSection("T_GUARDAR_PUNTOS");
		solo.clickOnView(solo.getView(com.example.hernan.examplegis.R.id.buttonGuardarPuntos));
        //Sleep for 3739 milliseconds
		solo.waitForDialogToClose();
		Trace.endSection();
        //Click on Generar ruta
		solo.clickOnView(solo.getView(com.example.hernan.examplegis.R.id.buttonGenerarRuta));
		Trace.beginSection("T_GENERAR_RUTA");
		solo.waitForText("Guardar ruta generada");
        //Sleep for 3920 milliseconds
		Trace.endSection();
		solo.sleep(1500);
        //Enter the text: 'ruta test'
		solo.clearEditText((android.widget.EditText) solo.getView(android.widget.EditText.class, 0));
		solo.enterText((android.widget.EditText) solo.getView(android.widget.EditText.class, 0), "ruta test");
        //Sleep for 911 milliseconds
		//Click on Aceptar
		solo.clickOnView(solo.getView(android.R.id.button1));
        //Wait for activity: 'com.example.hernan.examplegis.MapaActivity'
		assertTrue("com.example.hernan.examplegis.MapaActivity is not found!", solo.waitForActivity(com.example.hernan.examplegis.MapaActivity.class));
	}
}
