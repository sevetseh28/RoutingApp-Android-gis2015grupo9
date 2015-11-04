package com.example.hernan.examplegis;

import android.app.ProgressDialog;
//import android.graphics.Color;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Pair;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
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
import com.esri.core.symbol.PictureMarkerSymbol;
import com.esri.core.symbol.SimpleFillSymbol;
import com.esri.core.symbol.SimpleLineSymbol;
import com.esri.core.symbol.SimpleMarkerSymbol;
import com.esri.core.symbol.Symbol;
import com.esri.core.tasks.SpatialRelationship;
import com.esri.core.tasks.ags.query.Query;

import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.lang.Math;
import java.util.concurrent.ThreadPoolExecutor;

public class MapaActivity extends AppCompatActivity {
    private final Semaphore available = new Semaphore(1);
    private final Semaphore available2 = new Semaphore(1);
    public Graphic rutaGraphic;
    private Polyline rutaPolyline;
    private Point posActual;
    private int nextIndexPointOfPolyline = 1;

    private double velocidadActual = 5; // en metros por segundos

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mapa);
        MapView mMapView = (MapView) findViewById(R.id.map);

        AsyncTask<Void, Void, Void> MostrarRutaAsyncTask = new MostrarRutaAsyncTask().execute();

        SeekBar seekBar = (SeekBar) findViewById(R.id.seekBar);
        seekBar.setEnabled(false);

        TextView t = (TextView) findViewById(R.id.textViewSpeed);
        t.setVisibility(View.INVISIBLE);

        // Comportamiento de recorrido
        Button botonIniciarRecorrido = (Button) findViewById(R.id.buttonIniciarRecorrido);
        try {
            available2.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        final Executor exe = Executors.newFixedThreadPool(2);
        botonIniciarRecorrido.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                SeekBar seekBar = (SeekBar) findViewById(R.id.seekBar);
                seekBar.setEnabled(true);
                TextView t = (TextView) findViewById(R.id.textViewSpeed);
                t.setVisibility(View.VISIBLE);
                seekBar.setProgress((int) velocidadActual);
                MapView mMapView = (MapView) findViewById(R.id.map);

                GraphicsLayer countiesLayer = (GraphicsLayer) mMapView.getLayer(2);
                GraphicsLayer bufferLayer = (GraphicsLayer) mMapView.getLayer(3);
                GraphicsLayer pointLayer = (GraphicsLayer) mMapView.getLayer(4);
                AsyncTask<GraphicsLayer, Void, Void> RecorriendoTask = new RecorriendoTask().executeOnExecutor(exe, pointLayer, bufferLayer);
                AsyncTask<GraphicsLayer, Void, Void> CountiesTask = new CountiesTask().executeOnExecutor(exe, countiesLayer, bufferLayer);
            }
        });


        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                velocidadActual = (float) progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

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

            SpatialReference sr = SpatialReference.create(102100);


            // Creo la capa de poligonos de counties
            GraphicsLayer countiesLayer = new GraphicsLayer(); // creo la capa de puntos
            mMapView = (MapView) findViewById(R.id.map);
            mMapView.addLayer(countiesLayer); // id 2
         /*   Graphic bufferPunto = new Graphic(posActual, getPointSymbol(0));
            bufferPunto.addGraphic(puntoGraph);*/

            // Creo la capa del buffer y pongo el buffer
            GraphicsLayer bufferLayer = new GraphicsLayer(); // creo la capa de puntos
            mMapView = (MapView) findViewById(R.id.map);
            mMapView.addLayer(bufferLayer); // id 3
            Polygon p = GeometryEngine.buffer(posActual, sr, 5000, Unit.create(LinearUnit.Code.METER));
            Graphic buffer = new Graphic(p, new SimpleFillSymbol(Color.YELLOW, SimpleFillSymbol.STYLE.SOLID).setAlpha(60));
            bufferLayer.addGraphic(buffer);

            // Creo la capa de puntos y pongo el punto inicial
            GraphicsLayer pointLayer = new GraphicsLayer(); // creo la capa de puntos
            mMapView = (MapView) findViewById(R.id.map);
            mMapView.addLayer(pointLayer); // id 4
            Graphic puntoGraph = new Graphic(posActual, getPointSymbol(0));
            pointLayer.addGraphic(puntoGraph);
            pdLoading.dismiss();
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
                velocidadActual = 5; // arranco despacio
                MapView mMapView = (MapView) findViewById(R.id.map);
                GraphicsLayer pointsLayer = params[0];
                GraphicsLayer bufferLayer = params[1];
                Pair<Point, Integer> puntoMagico = null;
                Graphic puntoGraph = null;
                SpatialReference sr = SpatialReference.create(102100);
                int segundosPasados = 0;
                while (velocidadActual > 0) {
                    // Actualizo el punto
                    puntoMagico = getNextPoint(posActual, nextIndexPointOfPolyline, velocidadActual);
                    puntoGraph = new Graphic(puntoMagico.first, getPointSymbol(velocidadActual));
                    pointsLayer.updateGraphic(pointsLayer.getGraphicIDs()[0], puntoGraph);
                    MapaActivity.this.posActual = puntoMagico.first;
                    nextIndexPointOfPolyline = puntoMagico.second;

                    // Actualizo el buffer
                    Polygon p = GeometryEngine.buffer(posActual, sr, 5000, Unit.create(LinearUnit.Code.METER));
                    Graphic buffer = new Graphic(p, new SimpleFillSymbol(Color.YELLOW, SimpleFillSymbol.STYLE.SOLID).setAlpha(60));
                    bufferLayer.updateGraphic(bufferLayer.getGraphicIDs()[0], buffer);


                    Thread.sleep(1 * 1000); // once every 1 second
                    segundosPasados += 1;
                    if (segundosPasados % 5 == 0) {
                        available2.release(); // despierto la otra tarea que hace el llamado de los counties
                    }
                }
                puntoGraph = new Graphic(puntoMagico.first, getPointSymbol(velocidadActual));
                pointsLayer.updateGraphic(pointsLayer.getGraphicIDs()[0], puntoGraph);
                return null;

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

            // Rehabilito los botones
            SeekBar seekBar = (SeekBar) findViewById(R.id.seekBar);
            seekBar.setEnabled(false);
            TextView t = (TextView) findViewById(R.id.textViewSpeed);
            t.setVisibility(View.INVISIBLE);
            Button botonIniciarRecorrido = (Button) findViewById(R.id.buttonIniciarRecorrido);
            botonIniciarRecorrido.setEnabled(true);
        }
    }

    private class CountiesTask extends AsyncTask<GraphicsLayer, Void, Void> {

        @Override
        protected Void doInBackground(final GraphicsLayer... params) {
            SpatialReference sr = SpatialReference.create(102100);
            GraphicsLayer bufferLayer = params[1];

            while (true) {
                try {
                    available2.acquire();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Graphic buffer = bufferLayer.getGraphic(bufferLayer.getGraphicIDs()[0]);
                Polygon p = (Polygon) buffer.getGeometry();

                // Voy a verificar si es necesario realizar un pedido
                GraphicsLayer countiesLocalLayer = params[0];
                boolean necesitoRecalcular = false;
                if (countiesLocalLayer.getNumberOfGraphics() != 0) { // si no pasa el primer caso
                    Polygon[] intersecciones = new Polygon[countiesLocalLayer.getNumberOfGraphics()];
                    int[] graphicIdsOfLocalLayer = countiesLocalLayer.getGraphicIDs();
                    for (int i = 0; i < graphicIdsOfLocalLayer.length; i++) {
                        Geometry county = countiesLocalLayer.getGraphic(graphicIdsOfLocalLayer[i]).getGeometry();
                        intersecciones[i] = (Polygon) GeometryEngine.intersect(county, buffer.getGeometry(), sr);
                        if (intersecciones[i].isEmpty()) {
                            necesitoRecalcular = true;
                            break;
                        }
                    }
                    if (!necesitoRecalcular) {
                        Polygon uniondeIntersecciones = (Polygon) GeometryEngine.union(intersecciones, sr);
                        if (GeometryEngine.equals(uniondeIntersecciones, buffer.getGeometry(), sr)) {
                            continue;
                        }
                    }
                }

                // necesito hacer la llamada
                final ArcGISFeatureLayer countiesLayerService = new ArcGISFeatureLayer(
                        "http://services.arcgisonline.com/arcgis/rest/services/Demographics/USA_1990-2000_Population_Change/MapServer/3",
                        ArcGISFeatureLayer.MODE.ONDEMAND);

                Query q = new Query();
                q.setGeometry(p);
                q.setSpatialRelationship(SpatialRelationship.INTERSECTS);

                countiesLayerService.queryFeatures(q, new CallbackListener<FeatureSet>() {
                    @Override
                    public void onCallback(FeatureSet result) {
                        // MUESTRO LA RUTA EN EL MAPA
                        // Access the whole route geometry and add it as a graphic
                        //Geometry routeGeom = result.getGraphics()[0].getGeometry();
                        GraphicsLayer countiesLocalLayer = params[0];
                        SpatialReference sr = SpatialReference.create(102100);
                        boolean encontreigual = false;
                        boolean sondistintos = true;
                        int[] graphicIdsOfLocalLayer = countiesLocalLayer.getGraphicIDs();
                        if (countiesLocalLayer.getNumberOfGraphics() == result.getGraphics().length) { // si se cumple debo comparar
                            sondistintos = false;
                            // porque puede pasar que hayan algunos nuevos
                            for (int i = 0; i < result.getGraphics().length; i++) {
                                for (int j = 0; j < countiesLocalLayer.getNumberOfGraphics(); j++) {
                                    encontreigual = false;
                                    if (GeometryEngine.equals(countiesLocalLayer.getGraphic(graphicIdsOfLocalLayer[j]).getGeometry(), result.getGraphics()[i].getGeometry(), sr)) {
                                        encontreigual = true;
                                        break;
                                    }
                                }
                                if  (!encontreigual) {
                                    sondistintos = true;
                                    break;
                                }
                            }
                        }

                        if (sondistintos) {
                            countiesLocalLayer.removeAll();
                            for (int i = 0; i < result.getGraphics().length; i++) {
                                Polygon p = (Polygon) result.getGraphics()[i].getGeometry();
                                Graphic county = new Graphic(p, new SimpleFillSymbol(Color.CYAN, SimpleFillSymbol.STYLE.SOLID).setAlpha(50));
                                countiesLocalLayer.addGraphic(county);
                            }
                        }

                        available2.release();
                    }

                    @Override
                    public void onError(Throwable e) {
                        // handle the error
                    }
                });
            }
           // return null;
        }

    }


    private Symbol getPointSymbol(double velocidad) {
        float maxHue = 120;
        float speed = (float) velocidad;
        if (velocidad == 0) {
            SimpleMarkerSymbol s = new SimpleMarkerSymbol(Color.WHITE, 10, SimpleMarkerSymbol.STYLE.CIRCLE);
            s.setOutline(new SimpleLineSymbol(Color.BLACK, 1));
            return s;
        } else if (velocidad <= 1500 ) {
            int c = Color.HSVToColor(new float[]{((speed - 1) * (0 - maxHue)) / (1500 - 1) + maxHue, 1.0f, 1.0f});
            SimpleMarkerSymbol s = new SimpleMarkerSymbol(c, 10, SimpleMarkerSymbol.STYLE.CIRCLE);
            s.setOutline(new SimpleLineSymbol(Color.BLACK, 1));
            return s;
        } else {
            int c = Color.HSVToColor(new float[]{120f, 1.0f, 1.0f});
            SimpleMarkerSymbol s = new SimpleMarkerSymbol(c, 10, SimpleMarkerSymbol.STYLE.CIRCLE);
            s.setOutline(new SimpleLineSymbol(Color.BLACK, 1));
            return s;
        }
    }
}
