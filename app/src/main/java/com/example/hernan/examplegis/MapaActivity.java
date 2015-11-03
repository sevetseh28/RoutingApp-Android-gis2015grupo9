package com.example.hernan.examplegis;

import android.app.ProgressDialog;
import android.graphics.Color;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Pair;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.esri.android.map.GraphicsLayer;
import com.esri.android.map.MapView;
import com.esri.android.map.ags.ArcGISFeatureLayer;
import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.Line;
import com.esri.core.geometry.LinearUnit;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.Polygon;
import com.esri.core.geometry.Polyline;
import com.esri.core.geometry.Segment;
import com.esri.core.geometry.SpatialReference;
import com.esri.core.geometry.Unit;
import com.esri.core.map.CallbackListener;
import com.esri.core.map.FeatureSet;
import com.esri.core.map.Graphic;
import com.esri.core.symbol.SimpleLineSymbol;
import com.esri.core.symbol.SimpleMarkerSymbol;
import com.esri.core.tasks.ags.query.Query;

import java.util.Map;
import java.util.concurrent.Semaphore;
import java.lang.Math;

public class MapaActivity extends AppCompatActivity {
    private final Semaphore available = new Semaphore(1);
    public Graphic rutaGraphic;
    private Polyline rutaPolyline;
    private Point posActual;
    private int nextIndexPointOfPolyline = 1;

    private double velocidadActual = 0; // en metros por segundos

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mapa);
        MapView mMapView = (MapView) findViewById(R.id.map);

        AsyncTask<Void, Void, Void> MostrarRutaAsyncTask = new MostrarRutaAsyncTask().execute();

        // Comportamiento de recorrido
        Button botonIniciarRecorrido = (Button) findViewById(R.id.buttonIniciarRecorrido);


        botonIniciarRecorrido.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                GraphicsLayer pointLayer = new GraphicsLayer(); // creo la capa de puntos
                MapView mMapView = (MapView) findViewById(R.id.map);
                mMapView.addLayer(pointLayer);
                AsyncTask<GraphicsLayer, Void, Void> RecorriendoTask = new RecorriendoTask().execute(pointLayer);

              /*
                for(int i = 1; i<= 8; i++) {
                    Pair<Point, Integer> puntoMagico = getNextPoint(posActual, nextIndexPointOfPolyline, 1000);
                    Graphic puntoGraph = new Graphic(puntoMagico.first, new SimpleMarkerSymbol(Color.RED, 10, SimpleMarkerSymbol.STYLE.CIRCLE));
                    pointLayer.addGraphic(puntoGraph);
                    MapaActivity.this.posActual = puntoMagico.first;
                    nextIndexPointOfPolyline = puntoMagico.second;
                }
              */
               // mMapView.getLayer(2); // Map Layer 2 is point Layer
            }
        });
        botonIniciarRecorrido.setEnabled(false);
    }

    private Pair<Point, Integer> getNextPoint (Point posActual, int nextPointOfPolyline, double distanceChosen) {

        Line segmentRaw = new Line();
        segmentRaw.setStart(posActual);
        segmentRaw.setEnd(MapaActivity.this.rutaPolyline.getPoint(nextPointOfPolyline));

        Polyline segment = new Polyline();
        segment.addSegment(segmentRaw, true);

        //GeometryEngine engine = new GeometryEngine();
        SpatialReference sr = SpatialReference.create(102100);
        Polygon p = GeometryEngine.buffer(posActual, sr, distanceChosen, Unit.create(LinearUnit.Code.METER));
        Polyline intersection = (Polyline) GeometryEngine.intersect(segment, p, sr);
        Point endPointIntersection = new Point();
        endPointIntersection = intersection.getPoint(1);

        if (GeometryEngine.equals(intersection, segment, sr)) {
            Point startPoint = segment.getPoint(0);
            Point endPoint = segment.getPoint(1);

            double distance = GeometryEngine.distance(startPoint, endPoint, sr);
            double remainingDistance = distanceChosen - distance;
            if (remainingDistance == 0) { // necesito achicar el buffer
                return new Pair(endPoint, nextPointOfPolyline + 1);
            }
            else {
                return getNextPoint(endPoint, nextPointOfPolyline + 1, remainingDistance);
            }
        } else { // ya tengo el punto
            return new Pair(endPointIntersection, nextPointOfPolyline);
        }
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
            // Voy a usar esta capa para obtener la rutaGraphic
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

                                MapaActivity.this.rutaPolyline = (Polyline) g;
                                MapaActivity.this.posActual = ((Polyline) g).getPoint(0); // setup first inital point
                                MapaActivity.this.nextIndexPointOfPolyline = 1;
                                MapaActivity.this.rutaGraphic = new Graphic(g, new SimpleLineSymbol(Color.BLUE, 3));

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

            gLayer.addGraphic(MapaActivity.this.rutaGraphic);
            // Add basemap layer first
            //mMapView.addLayer(basemap);
            // Add empty GraphicsLayer
            mMapView.addLayer(gLayer);

            Graphic ruta = MapaActivity.this.rutaGraphic;
            Polyline polilinea =(Polyline) ruta.getGeometry();
            mMapView.setExtent(ruta.getGeometry());
            pdLoading.dismiss();
        }
    }


    private class AumentarVelocidad extends AsyncTask<Void, Void, Void> {
        ProgressDialog pdLoading = new ProgressDialog(MapaActivity.this);

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... params) {
            velocidadActual =+ 10;
            return null;
        }

        @Override
        /** The system calls this to perform work in the UI thread and delivers
         * the result from doInBackground() */
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
        }
    }

    private class DisminuirVelocidad extends AsyncTask<Void, Void, Void> {
        ProgressDialog pdLoading = new ProgressDialog(MapaActivity.this);

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... params) {
            if (velocidadActual-10 > 0) {
                velocidadActual =- 10;
            }
            return null;
        }

        @Override
        /** The system calls this to perform work in the UI thread and delivers
         * the result from doInBackground() */
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
        }
    }

    private class RecorriendoTask extends AsyncTask<GraphicsLayer, Void, Void> {
        ProgressDialog pdLoading = new ProgressDialog(MapaActivity.this);

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(GraphicsLayer... params) {
            try {
                MapView mMapView = (MapView) findViewById(R.id.map);
                GraphicsLayer pointsLayer = params[0];
                Pair<Point, Integer> puntoMagico = getNextPoint(posActual, nextIndexPointOfPolyline, 200);
                Graphic puntoGraph = new Graphic(puntoMagico.first, new SimpleMarkerSymbol(Color.RED, 10, SimpleMarkerSymbol.STYLE.CIRCLE));
                pointsLayer.addGraphic(puntoGraph);
                Thread.sleep(1 * 1000);
                while (true) {
                    puntoMagico = getNextPoint(posActual, nextIndexPointOfPolyline, 200);
                    puntoGraph = new Graphic(puntoMagico.first, new SimpleMarkerSymbol(Color.RED, 10, SimpleMarkerSymbol.STYLE.CIRCLE));
                    pointsLayer.updateGraphic(pointsLayer.getGraphicIDs()[0], puntoGraph);
                    MapaActivity.this.posActual = puntoMagico.first;
                    nextIndexPointOfPolyline = puntoMagico.second;
                    Thread.sleep(1 * 1000); // once every 1 second:
                }
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
        }
    }

}
