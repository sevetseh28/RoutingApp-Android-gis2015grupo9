package com.example.hernan.examplegis;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.esri.core.tasks.geocode.Locator;
import com.esri.core.tasks.geocode.LocatorFindParameters;
import com.esri.core.tasks.geocode.LocatorGeocodeResult;

import java.util.List;

public class SearchableActivity extends ListActivity {

    public List<LocatorGeocodeResult> locations;
    public int selectedPosition;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_searchable);

        // Get the intent, verify the action and get the query
        Intent intent = getIntent();
//        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
        String query = intent.getStringExtra(SearchManager.QUERY);
        executeLocatorTask(query);
//        }

        Button buttonVerMapa = (Button) findViewById(R.id.buttonVerEnMapa);
        buttonVerMapa.setEnabled(false);

        // OnClick listener for add location button
        Button buttonAniadir = (Button) findViewById(R.id.buttonAddLoc);
        buttonAniadir.setEnabled(false);
        buttonAniadir.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Perform action on click
                LocatorGeocodeResult locationChosen = locations.get(selectedPosition);

                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                intent.setAction(Intent.ACTION_VIEW);

                intent.putExtra("LUGAR_ELEGIDO", locationChosen.getAddress());
                intent.putExtra("posX", locationChosen.getLocation().getX());
                intent.putExtra("posY", locationChosen.getLocation().getY());
                setResult(RESULT_OK, intent);

                finish();
                ////Intent activityChangeIntent = new Intent(PresentActivity.this, NextActivity.class);

                // currentContext.startActivity(activityChangeIntent);

                ////PresentActivity.this.startActivity(activityChangeIntent);
            }
        });

        buttonVerMapa.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Perform action on click
                LocatorGeocodeResult locationChosen = locations.get(selectedPosition);

                Intent intent = new Intent(getApplicationContext(), VerPuntoActivity.class);
                intent.setAction(Intent.ACTION_VIEW);
                intent.putExtra("posX", locationChosen.getLocation().getX());
                intent.putExtra("posY", locationChosen.getLocation().getY());
                //setResult(RESULT_OK, intent);
                startActivity(intent);
            }
        });

    }


    /**
     * Set up the search parameters and execute the Locator task.
     *
     * @param address
     */

    private void executeLocatorTask(String address) {
        // Create Locator parameters from single line address string
        LocatorFindParameters findParams = new LocatorFindParameters(address);
        // Execute async task to find the address
        new LocatorAsyncTask().execute(findParams);
    }


    private class LocatorAsyncTask extends AsyncTask<LocatorFindParameters, Void, List<LocatorGeocodeResult>> {
        private Exception mException;

        /* Set up dialog message for loading spinner */
        private ProgressDialog dialog = new ProgressDialog(SearchableActivity.this);

        @Override
        protected void onPreExecute() {
            this.dialog.setMessage("Obteniendo ubicaciones...");
            this.dialog.show();
        }

        public LocatorAsyncTask() {
        }

        @Override
        protected List<LocatorGeocodeResult> doInBackground(
                LocatorFindParameters[] params) {
            // Perform routing request on background thread
            mException = null;
            List<LocatorGeocodeResult> results = null;

            // Create locator using default online geocoding service and tell it
            // to find the given address
            // String url = "http://geocode.arcgis.com/arcgis/rest/services/World/GeocodeServer";
            Locator locator = Locator.createOnlineLocator();
            try {
                results = locator.find(params[0]);
            } catch (Exception e) {
                mException = e;
            }
            return results;
        }

        @Override
        protected void onPostExecute(List<LocatorGeocodeResult> result) {
            // Display results on UI thread
            if (mException != null) {
                //Log.w(TAG, "LocatorSyncTask failed with:");
                mException.printStackTrace();
                Toast.makeText(SearchableActivity.this, getString(R.string.addressSearchFailed), Toast.LENGTH_LONG).show();
                return;
            }

            if (result.size() == 0) {
                Toast.makeText(SearchableActivity.this,
                        getString(R.string.noResultsFound), Toast.LENGTH_LONG).show();
            } else {
                // Use first result in the list
                ArrayAdapter<String> itemsAdapter =
                        new ArrayAdapter<String>(SearchableActivity.this, android.R.layout.simple_list_item_1);


                // Guardo el resultado en la Actividad para luego pasarla
                locations = result;


                for (int i = 0; i < result.size(); i++ ){
                    LocatorGeocodeResult geocodeResult = result.get(i);
                    itemsAdapter.add(geocodeResult.getAddress());
                }
                final ListView lv = getListView();
                lv.setAdapter(itemsAdapter);

                lv.setOnItemClickListener(new AdapterView.OnItemClickListener()
                {
                    @Override
                    public void onItemClick(AdapterView<?> adapter, View v, int position,
                                            long arg3)
                    {
                        selectedPosition = position;
                        Button buttonVerMapa = (Button) findViewById(R.id.buttonVerEnMapa);
                        buttonVerMapa.setEnabled(true);
                        Button buttonAniadir = (Button) findViewById(R.id.buttonAddLoc);
                        buttonAniadir.setEnabled(true);
                        //String value = (String)adapter.getItemAtPosition(position);
                        // assuming string and if you want to get the value on click of list item
                        // do what you intend to do on click of listview row
                    }
                });


                //LocatorGeocodeResult geocodeResult = result.get(0);
                // crear alerta
                //Toast.makeText(SearchableActivity.this, geocodeResult.getAddress(), Toast.LENGTH_LONG).show();
            }

            if (dialog.isShowing()) {
                dialog.dismiss();
            }
        }

    }


}
