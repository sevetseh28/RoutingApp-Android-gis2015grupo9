package com.example.hernan.examplegis;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

import com.esri.android.map.GraphicsLayer;
import com.esri.android.map.MapView;
import com.esri.android.map.ags.ArcGISFeatureLayer;
import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.SpatialReference;
import com.esri.core.map.CallbackListener;
import com.esri.core.map.FeatureEditResult;
import com.esri.core.map.FeatureSet;
import com.esri.core.map.Graphic;
import com.esri.core.symbol.SimpleLineSymbol;
import com.esri.core.tasks.ags.query.Query;
import com.esri.core.tasks.na.NAFeaturesAsFeature;
import com.esri.core.tasks.na.RouteParameters;
import com.esri.core.tasks.na.RouteResult;
import com.esri.core.tasks.na.RouteTask;
import com.esri.core.tasks.na.StopGraphic;

import org.apache.commons.lang3.ArrayUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;

public class MapaActivity extends AppCompatActivity {
    private final Semaphore available = new Semaphore(1);
    public Graphic ruta;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mapa);
        MapView mMapView = (MapView) findViewById(R.id.map);

        AsyncTask<Void, Void, Void> MostrarRutaAsyncTask = new MostrarRutaAsyncTask().execute();
    }

    private class MostrarRutaAsyncTask extends AsyncTask<Void, Void, Void> {
        ProgressDialog pdLoading = new ProgressDialog(MapaActivity.this);

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            //this method will be running on UI thread
            pdLoading.setMessage("Mostrando ruta en mapa...");
            pdLoading.show();
        }

        @Override
        protected Void doInBackground(Void... params) {
            // Voy a usar esta capa para obtener la ruta
            ArcGISFeatureLayer trailsLayer = new ArcGISFeatureLayer(
                    "http://sampleserver5.arcgisonline.com/ArcGIS/rest/services/LocalGovernment/Recreation/FeatureServer/1",
                    ArcGISFeatureLayer.MODE.ONDEMAND);

            // Get the points previously saved
            Query q = new Query();
            long[] objid = new long[]{ getIntent().getLongExtra("OBJECT_ID_RUTA", 0) };
            q.setObjectIds(objid);
            try {
                available.acquire();
                trailsLayer.queryFeatures(q,
                        new CallbackListener<FeatureSet>() {
                            @Override
                            public void onCallback(FeatureSet result) {
                                // MUESTRO LA RUTA EN EL MAPA
                                // Access the whole route geometry and add it as a graphic
                                Geometry routeGeom = result.getGraphics()[0].getGeometry();
                                Geometry g = GeometryEngine.project(routeGeom, SpatialReference.create(4326), SpatialReference.create(102100));
                                Graphic symbolGraphic = new Graphic(g, new SimpleLineSymbol(Color.BLUE, 3));
                                MapaActivity.this.ruta = symbolGraphic;
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

            // Inflate Mapview from XML
            MapView mMapView = (MapView) findViewById(R.id.map);

            // Create GraphicsLayer
            GraphicsLayer gLayer = new GraphicsLayer();

            gLayer.addGraphic(MapaActivity.this.ruta);
            // Add basemap layer first
            //mMapView.addLayer(basemap);
            // Add empty GraphicsLayer
            mMapView.addLayer(gLayer);

            pdLoading.dismiss();
        }
    }

}
