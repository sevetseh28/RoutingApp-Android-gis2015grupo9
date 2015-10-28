package com.example.hernan.examplegis;

import android.app.ProgressDialog;
import android.app.SearchManager;
import android.app.SearchableInfo;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
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
import com.esri.core.map.FeatureSet;
import com.esri.core.map.Graphic;
import com.esri.core.symbol.SimpleMarkerSymbol;
import com.esri.core.tasks.ags.query.Query;
import com.esri.core.tasks.na.NAFeaturesAsFeature;
import com.esri.core.tasks.na.Route;
import com.esri.core.tasks.na.RouteParameters;
import com.esri.core.tasks.na.RouteResult;
import com.esri.core.tasks.na.RouteTask;
import com.esri.core.tasks.na.StopGraphic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;

import org.apache.commons.lang3.ArrayUtils;

public class MainActivity extends AppCompatActivity {
    private final Semaphore available = new Semaphore(1);

    private EditText mSearchEditText;
    private static final String TAG = "MainActivity";

    private ArrayAdapter<String> listAdapter;
    public List<Point> listaActualdeStops = new ArrayList<Point>();
    public ArrayList<Long> objectIDsPuntosGuardados = new ArrayList<Long>();
    public long objectIdRutaGuardada;
    public Graphic[] puntosGraficosDeRuta;

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
                if (MainActivity.this.listAdapter.getCount() < 2 ) {
                    Toast.makeText(MainActivity.this,
                            "Deben haber al menos dos puntos",
                            Toast.LENGTH_LONG).show();
                    return;
                }

                AsyncTask<Void, Void, Void> guardar_puntos = new GuardarPuntosAsyncTask().execute();
            }
        });

        // Cuando hace click en generar ruta debo consultar el servicio de ruteo y guardar
        // la polilinea en el FeatureService provisto
        buttonGenerarRuta.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                AsyncTask<Void, Void, Void> obtener_puntos = new ObtenerPuntos().execute();
            }
        });
    }

    private class GuardarPuntosAsyncTask extends AsyncTask<Void, Void, Void> {
        ProgressDialog pdLoading = new ProgressDialog(MainActivity.this);

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            //this method will be running on UI thread
            pdLoading.setMessage("Guardando puntos...");
            pdLoading.show();
        }

        @Override
        protected Void doInBackground(Void... params) {
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
                attributes.put("description", "GIS-Gr-9: " + MainActivity.this.listAdapter.getItem(i));
                attributes.put("event_type", 5);
                Graphic g = new Graphic(punto, new SimpleMarkerSymbol(Color.RED, 10, SimpleMarkerSymbol.STYLE.CIRCLE), attributes);

                adds.add(g);

                i++;
                iteratorPoints.next();
            }
            Graphic[] addsArray = adds.toArray(new Graphic[adds.size()]);
            // put Graphic in an array (applyEdits takes Graphic arrays as parameters)
            try {
                available.acquire();
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
                                }
                                available.release();
                            }

                            @Override
                            public void onError(Throwable e) {
                                // handle the error
                            }
                        }
                );
                available.acquire();
                available.release();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        /** The system calls this to perform work in the UI thread and delivers
         * the result from doInBackground() */
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);

            // disable button to save points
            findViewById(R.id.buttonGuardarPuntos).setEnabled(false);
            findViewById(R.id.buttonGenerarRuta).setEnabled(true);
            pdLoading.dismiss();
        }
    }

    private class ObtenerPuntos extends AsyncTask<Void, Void, Void> {
        ProgressDialog pdLoading = new ProgressDialog(MainActivity.this);

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            //this method will be running on UI thread
            pdLoading.setMessage("Obteniendo puntos guardados...");
            pdLoading.show();
        }

        @Override
        protected Void doInBackground(Void... params) {
            // Voy a usar esta capa para obtener los puntos previamente guardados
            ArcGISFeatureLayer eventlayer = new ArcGISFeatureLayer(
                    "http://sampleserver5.arcgisonline.com/ArcGIS/rest/services/LocalGovernment/Events/FeatureServer/0",
                    ArcGISFeatureLayer.MODE.ONDEMAND);

            // Get the points previously saved
            Query q = new Query();
            ArrayList<Long> objids = MainActivity.this.objectIDsPuntosGuardados;
            q.setObjectIds(ArrayUtils.toPrimitive(objids.toArray(new Long[objids.size()])));
            try {
                available.acquire();

                eventlayer.queryFeatures(q,
                        new CallbackListener<FeatureSet>() {

                            @Override
                            public void onCallback(FeatureSet result) {
                                // do something with the feature edit result object
                                MainActivity.this.puntosGraficosDeRuta = result.getGraphics();
                                available.release();
                            }

                            @Override
                            public void onError(Throwable e) {
                                // handle the error
                            }
                        }
                );
                available.acquire();
                available.release();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        /** The system calls this to perform work in the UI thread and delivers
         * the result from doInBackground() */
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            // LUEGO DE OBTENER LOS PUNTOS GENERO LA RUTA
            pdLoading.dismiss();
            AsyncTask<Void, Void, Void> obtener_ruta = new GenerarYGuardarRutaAsyncTask().execute();
        }
    }

    private class GenerarYGuardarRutaAsyncTask extends AsyncTask<Void, Void, Void> {
        ProgressDialog pdLoading = new ProgressDialog(MainActivity.this);

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            //this method will be running on UI thread
            pdLoading.setMessage("Generando y guardando ruta...");
            pdLoading.show();
        }

        @Override
        protected Void doInBackground(Void... params) {
            String routeTaskURL = "http://tasks.arcgisonline.com/ArcGIS/rest/services/NetworkAnalysis/ESRI_Route_NA/NAServer/Route";
            RouteTask routeTask = null;
            try {
                routeTask = RouteTask.createOnlineRouteTask(routeTaskURL, null);
            } catch (Exception e) {
                e.printStackTrace();
            }


            // create routing features class
            NAFeaturesAsFeature naFeatures = new NAFeaturesAsFeature();
            // Create the stop points from point geometry
            Graphic[] puntosGraph = new Graphic[MainActivity.this.objectIDsPuntosGuardados.size()];
            for (int i = 0; i < MainActivity.this.puntosGraficosDeRuta.length; i++) {
                puntosGraph[i] = new StopGraphic(MainActivity.this.puntosGraficosDeRuta[i]);
            }
            // set features on routing feature class
            naFeatures.setFeatures(puntosGraph);
            // set stops on routing feature class

            RouteParameters routeParams = null;
            try {
                routeParams = routeTask.retrieveDefaultRouteTaskParameters();
            } catch (Exception e) {
                e.printStackTrace();
            }
            routeParams.setStops(naFeatures);
            RouteResult resultRuta = null;
            try {
                resultRuta = routeTask.solve(routeParams);
            } catch (Exception e) {
                e.printStackTrace();
            }
            // Voy a guardar la ruta generada (polilinea) en el servicio propuesto
            ArcGISFeatureLayer trailsLayer = new ArcGISFeatureLayer(
                    "http://sampleserver5.arcgisonline.com/ArcGIS/rest/services/LocalGovernment/Recreation/FeatureServer/1",
                    ArcGISFeatureLayer.MODE.ONDEMAND);
            Map<String, Object> attributes = new HashMap<String, Object>();
            attributes.put("notes", "GIS-Gr-9: " + resultRuta.getRoutes().get(0).getRouteName());
            attributes.put("trailtype", 4); // Motorized trailtype
            Graphic rutaRaw = resultRuta.getRoutes().get(0).getRouteGraphic();
            Graphic rutaFinal = new Graphic(rutaRaw.getGeometry(), rutaRaw.getSymbol(), attributes);
            // Graphic g = new Graphic(punto, new SimpleMarkerSymbol(Color.RED, 10, SimpleMarkerSymbol.STYLE.CIRCLE), attributes);

            // adds.add(g);
            Graphic[] addsArray = new Graphic[]{rutaFinal};
            try {
                available.acquire();
                trailsLayer.applyEdits(
                        addsArray,
                        new Graphic[]{}, // no graphics to update
                        new Graphic[]{}, // no graphics to delete
                        new CallbackListener<FeatureEditResult[][]>() {

                            @Override
                            public void onCallback(FeatureEditResult[][] result) {
                                MainActivity.this.objectIdRutaGuardada = result[0][0].getObjectId();
                                available.release();
                            }

                            @Override
                            public void onError(Throwable e) {
                                // handle the error
                            }
                        }
                );
                available.acquire();
                available.release();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }


            return null;
        }

        @Override
        /** The system calls this to perform work in the UI thread and delivers
         * the result from doInBackground() */
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            // LUEGO DE GENERAR LA RUTA INICIO EL MAPA

            pdLoading.dismiss();
            //findViewById(R.id.buttonGenerarRuta).setEnabled(false);
            Intent intent = new Intent(MainActivity.this, MapaActivity.class);
            intent.putExtra("OBJECT_ID_RUTA", MainActivity.this.objectIdRutaGuardada);
            MainActivity.this.startActivity(intent);
        }
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
