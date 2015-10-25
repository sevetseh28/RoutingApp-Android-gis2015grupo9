package com.example.hernan.examplegis;

import android.app.SearchManager;
import android.app.SearchableInfo;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Toast;

import com.esri.android.map.ags.ArcGISFeatureLayer;
import com.esri.core.geometry.Point;
import com.esri.core.map.CallbackListener;
import com.esri.core.map.FeatureEditResult;
import com.esri.core.map.Graphic;
import com.esri.core.symbol.SimpleMarkerSymbol;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private EditText mSearchEditText;
    private static final String TAG = "MainActivity";

    private ArrayAdapter<String> listAdapter;
    public List<Point> listaActualdeStops = new ArrayList<Point>();
    public ArrayList<Long> objectIDsPuntosGuardados = new ArrayList<Long>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final Button buttonGenerarRuta = (Button) findViewById(R.id.buttonGenerarRuta);
        buttonGenerarRuta.setEnabled(false);


        // Esto es para la busqueda, no se muy bien que es lo que hace
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) findViewById(R.id.searchView);
        SearchableInfo info = searchManager.getSearchableInfo(getComponentName());
        searchView.setSearchableInfo(info);

        searchView.setIconifiedByDefault(false);

        // Creo el adaptador para los itemas que representan los stops
        final ArrayAdapter<String> listAdapter =
                new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_list_item_1);
        this.listAdapter = listAdapter;
        ListView lv = (ListView) findViewById(R.id.listaRutas);
        lv.setAdapter(listAdapter);


        // Listener al hacer click en el boton de generar ruta
        // OnClick listener for add location button
        Button buttonGuardarPuntos = (Button) findViewById(R.id.buttonGuardarPuntos);
        buttonGuardarPuntos.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                // obtain the clicked point from a MouseEvent
                if (MainActivity.this.listAdapter.getCount() < 2) {
                    Toast.makeText(MainActivity.this,
                            "Deben haber al menos dos puntos",
                            Toast.LENGTH_LONG).show();
                    return;
                }

                // Create feature layers
                ArcGISFeatureLayer eventlayer = new ArcGISFeatureLayer(
                        "http://sampleserver5.arcgisonline.com/ArcGIS/rest/services/LocalGovernment/Events/FeatureServer/0",
                        ArcGISFeatureLayer.MODE.ONDEMAND);


                Iterator<Point> iteratorPoints = MainActivity.this.listaActualdeStops.iterator();
                int i = 0;

                ArrayList<Graphic> adds = new ArrayList<Graphic>();
                while (iteratorPoints.hasNext()) {
                    Point punto = MainActivity.this.listaActualdeStops.get(i);

                    // create a map of attributes (keys must match fields in the feature layer)
                    // prepare the Graphic to add
                    Map<String, Object> attributes = new HashMap<String, Object>();
                    attributes.put("description", MainActivity.this.listAdapter.getItem(i));
                    attributes.put("event_type", 5);
                    Graphic g = new Graphic(punto, new SimpleMarkerSymbol(Color.RED, 10, SimpleMarkerSymbol.STYLE.CIRCLE), attributes);

                    adds.add(g);

                    i++;
                    iteratorPoints.next();
                }
                Graphic[] addsArray = adds.toArray(new Graphic[adds.size()]);
                // put Graphic in an array (applyEdits takes Graphic arrays as parameters)
                eventlayer.applyEdits(
                        addsArray,
                        new Graphic[]{}, // no graphics to update
                        new Graphic[]{}, // no graphics to delete
                        new CallbackListener<FeatureEditResult[][]>() {

                            @Override
                            public void onCallback(FeatureEditResult[][] result) {
                                // do something with the feature edit result object
                                // I save the IDs of the saved points
                                for (int i = 0; i < result[0].length; i++) {
                                    MainActivity.this.objectIDsPuntosGuardados.add(result[0][i].getObjectId());
                                    // disable button to save points
                                }
                            }

                            @Override
                            public void onError(Throwable e) {
                                // handle the error
                            }
                        }
                );

                findViewById(R.id.buttonGuardarPuntos).setEnabled(false);
                findViewById(R.id.buttonGenerarRuta).setEnabled(true);
                findViewById(R.id.searchView).clearFocus();
                findViewById(R.id.searchView).setVisibility(View.GONE);

            }
        });
    }


    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        // Get the intent, verify the action and get the query
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            // manually launch the real search activity

            final Intent searchIntent = new Intent(getApplicationContext(),
                    SearchableActivity.class);
            // add query to the Intent Extras
            searchIntent.putExtra(SearchManager.QUERY, query);
            startActivityForResult(searchIntent, 0);
        }
    }


    @Override
    // CUANDO ME DEVUELVE EL ITEM SELECCIONADO SE EJECUTA ESTO
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == 0) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                Toast.makeText(MainActivity.this,
                        data.getStringExtra("LUGAR_ELEGIDO"),
                        Toast.LENGTH_LONG).show();
                listAdapter.add(data.getStringExtra("LUGAR_ELEGIDO"));
                Point nuevoPunto = new Point(data.getDoubleExtra("posX", 0), data.getDoubleExtra("posY", 0));

                this.listaActualdeStops.add(nuevoPunto);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);


        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
