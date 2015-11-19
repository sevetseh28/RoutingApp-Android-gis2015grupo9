package com.example.hernan.examplegis;

import android.app.ProgressDialog;
//import android.graphics.Color;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Trace;
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
import com.esri.android.map.event.OnStatusChangedListener;
import com.esri.core.geometry.AreaUnit;
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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.lang.Math;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class MapaActivity extends AppCompatActivity {
    private MapView mapa;
    private boolean rutaObtenidaConExito = true;
    private boolean llegueAlFinalDelRecorrido = false;
    private ReentrantReadWriteLock lockCountiesLayer = new ReentrantReadWriteLock();
    private final Semaphore available = new Semaphore(1);
    private final Semaphore available2 = new Semaphore(1);
    private final Semaphore termineDePedirLosCounties = new Semaphore(1);
    private final Semaphore semaforoTexto = new Semaphore(1);
    public Graphic rutaGraphic;
    private Polyline rutaPolyline;
    private Point posActual;
    private int nextIndexPointOfPolyline = 1;

    private double velocidadActual = 5; // en metros por segundos
    private double radioBufferActual = 2500;

    private AsyncTask<GraphicsLayer, Void, Void> taskRecorriendo = null;
    private AsyncTask<GraphicsLayer, Void, Void> taskCounties = null;
    private AsyncTask<Void, Void, Boolean> taskMostrarRuta = null;
    @Override
    public void onDestroy() {
        super.onDestroy();
        if(taskRecorriendo != null && taskRecorriendo.getStatus() != AsyncTask.Status.FINISHED) {
            taskRecorriendo.cancel(true);
        }
        if(taskCounties != null && taskCounties.getStatus() != AsyncTask.Status.FINISHED) {
            taskCounties.cancel(true);
        }
        if(taskMostrarRuta != null && taskMostrarRuta.getStatus() != AsyncTask.Status.FINISHED) {
            taskMostrarRuta.cancel(true);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_mapa);
        mapa = (MapView) findViewById(R.id.map);
        Button botonIniciarRecorrido = (Button) findViewById(R.id.buttonIniciarRecorrido);
        botonIniciarRecorrido.setEnabled(false);
        taskMostrarRuta = new MostrarRutaAsyncTask().execute();

        SeekBar seekBar = (SeekBar) findViewById(R.id.seekBar);
        seekBar.setEnabled(false);
        SeekBar seekBarBuf = (SeekBar) findViewById(R.id.seekBarBuffer);
        seekBarBuf.setEnabled(false);

        TextView t = (TextView) findViewById(R.id.textViewSpeed);
        t.setEnabled(false);
        TextView t2 = (TextView) findViewById(R.id.textBuffer);
        t2.setEnabled(false);

        try {
            available2.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

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

        seekBarBuf.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress == 0) { // no lo dejo poner buffer 0
                    radioBufferActual = 100;
                } else {
                    radioBufferActual = (float) progress;
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

    }

    private Pair<Point, Integer> getNextPoint(Point posActual, int nextPointOfPolyline, double distanceChosen) {
        Line segmentRaw = new Line();
        segmentRaw.setStart(posActual);
        segmentRaw.setEnd(rutaPolyline.getPoint(nextPointOfPolyline));

        Polyline segment = new Polyline();
        segment.addSegment(segmentRaw, true);

        //GeometryEngine engine = new GeometryEngine();
        SpatialReference sr = SpatialReference.create(102100);
        Polygon p = GeometryEngine.buffer(posActual, sr, distanceChosen, Unit.create(LinearUnit.Code.METER));
        Polyline intersection = (Polyline) GeometryEngine.intersect(segment, p, sr);
        Point endPointIntersection = new Point();
        endPointIntersection = intersection.getPoint(1);

        if (GeometryEngine.equals(intersection, segment, sr)) { // si son iguales es porque el buffer
            // es mas grande o entró justo en la distancia elegida
            Point startPoint = segment.getPoint(0);
            Point endPoint = segment.getPoint(1);

            double distance = GeometryEngine.distance(startPoint, endPoint, sr);
            double remainingDistance = distanceChosen - distance;
            boolean llegueAlPuntoFinal = GeometryEngine.equals(rutaPolyline.getPoint(nextPointOfPolyline), rutaPolyline.getPoint(rutaPolyline.getPointCount() - 1), sr);
            if (remainingDistance == 0 && !(llegueAlPuntoFinal)) { // ya llegue al punto justito pero no es el final
                return new Pair(endPoint, nextPointOfPolyline + 1);
            }
            else if (llegueAlPuntoFinal) {
                // si el proximo punto es el final no importa el buffer, ya llegue
                return new Pair(endPoint, nextPointOfPolyline);
            }
            else {
                return getNextPoint(endPoint, nextPointOfPolyline + 1, remainingDistance);
            }
        } else { // ya tengo el punto
            return new Pair(endPointIntersection, nextPointOfPolyline);
        }
    }

    private class MostrarRutaAsyncTask extends AsyncTask<Void, Void, Boolean> {
        ProgressDialog pdLoading = new ProgressDialog(MapaActivity.this);

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            //this method will be running on UI thread
            pdLoading.setMessage("Obteniendo y mostrando ruta en mapa...");
            pdLoading.show();
        }

        @Override
        protected Boolean doInBackground(Void... params) {
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
                                if (result.getGraphics().length == 1) {
                                    Geometry routeGeom = result.getGraphics()[0].getGeometry();
                                    Geometry g = GeometryEngine.project(routeGeom, SpatialReference.create(4326), SpatialReference.create(102100));

                                    rutaPolyline = (Polyline) g;
                                    posActual = ((Polyline) g).getPoint(0); // setup first inital point
                                    nextIndexPointOfPolyline = 1;
                                    rutaGraphic = new Graphic(g, new SimpleLineSymbol(Color.BLUE, 3));
                                } else {
                                    rutaObtenidaConExito = false;
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
            if (rutaObtenidaConExito) {
                return true;
            } else {
                return false;
            }
        }


        @Override
        /** The system calls this to perform work in the UI thread and delivers
         * the result from doInBackground() */
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);

            if (rutaObtenidaConExito) {


                // Create GraphicsLayer
                GraphicsLayer gLayer = new GraphicsLayer();

                gLayer.addGraphic(rutaGraphic);
                // Add basemap layer first
                //mMapView.addLayer(basemap);
                // Add empty GraphicsLayer
                mapa.addLayer(gLayer); // capa de la ruta 1



                SpatialReference sr = SpatialReference.create(102100);


                // Creo la capa de poligonos de counties
                GraphicsLayer countiesLayer = new GraphicsLayer(); // creo la capa de puntos
                mapa = (MapView) findViewById(R.id.map);
                mapa.addLayer(countiesLayer); // id 2
            /*   Graphic bufferPunto = new Graphic(posActual, getPointSymbol(0));
                bufferPunto.addGraphic(puntoGraph);*/

                // Creo la capa del buffer y pongo el buffer
                GraphicsLayer bufferLayer = new GraphicsLayer(); // creo la capa de puntos
                mapa.addLayer(bufferLayer); // id 3
                Polygon p = GeometryEngine.buffer(posActual, sr, radioBufferActual, Unit.create(LinearUnit.Code.METER));
                Graphic buffer = new Graphic(p, new SimpleFillSymbol(Color.YELLOW, SimpleFillSymbol.STYLE.SOLID).setAlpha(60));
                bufferLayer.addGraphic(buffer);

                // Creo la capa de puntos y pongo el punto inicial
                GraphicsLayer pointLayer = new GraphicsLayer(); // creo la capa de puntos
                mapa.addLayer(pointLayer); // id 4
                Graphic puntoGraph = new Graphic(posActual, getPointSymbol(0));
                pointLayer.addGraphic(puntoGraph);
                pdLoading.dismiss();

                final Executor exe = Executors.newFixedThreadPool(2);
                // Comportamiento de recorrido
                Button botonIniciarRecorrido = (Button) findViewById(R.id.buttonIniciarRecorrido);
                botonIniciarRecorrido.setEnabled(true);
                botonIniciarRecorrido.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        v.setEnabled(false);
                        SeekBar seekBar = (SeekBar) findViewById(R.id.seekBar);
                        seekBar.setEnabled(true);
                        seekBar.setProgress((int) velocidadActual);
                        SeekBar seekBarBuf = (SeekBar) findViewById(R.id.seekBarBuffer);
                        seekBarBuf.setEnabled(true);
                        seekBarBuf.setProgress((int) radioBufferActual);
                        TextView t = (TextView) findViewById(R.id.textViewSpeed);
                        t.setEnabled(true);
                        TextView t2 = (TextView) findViewById(R.id.textBuffer);
                        t2.setEnabled(true);

                        MapView mMapView = (MapView) findViewById(R.id.map);

                        GraphicsLayer countiesLayer = (GraphicsLayer) mMapView.getLayer(2);
                        GraphicsLayer bufferLayer = (GraphicsLayer) mMapView.getLayer(3);
                        GraphicsLayer pointLayer = (GraphicsLayer) mMapView.getLayer(4);
                        taskRecorriendo = new RecorriendoTask().executeOnExecutor(exe, pointLayer, bufferLayer);
                        taskCounties = new CountiesTask().executeOnExecutor(exe, countiesLayer, bufferLayer);
                    }
                });

                // Si el mapa esta inicializado voy a centrarlo en la ruta cargada
                Graphic ruta = rutaGraphic;
                Polyline polilinea =(Polyline) ruta.getGeometry();
                mapa.setExtent(ruta.getGeometry());
                mapa.setOnStatusChangedListener(new OnStatusChangedListener() {
                    private static final long serialVersionUID = 1L;

                    public void onStatusChanged(Object source, STATUS status) {
                        if (OnStatusChangedListener.STATUS.INITIALIZED == status && source == mapa) {
                            mapa.setExtent(rutaGraphic.getGeometry());
                        }
                    }
                });

            } else {
                pdLoading.dismiss();
                Toast.makeText(MapaActivity.this,
                        "La ruta fue borrada del servicio",
                        Toast.LENGTH_LONG).show();
                MapaActivity.this.finish();
            }
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
                    Trace.beginSection("Obteniendo proximo punto");
                    puntoMagico = getNextPoint(posActual, nextIndexPointOfPolyline, velocidadActual);
                    Trace.endSection();
                    posActual = puntoMagico.first;
                    nextIndexPointOfPolyline = puntoMagico.second;
                    if (nextIndexPointOfPolyline == rutaPolyline.getPointCount() - 1) { // si legue al final del recorrido
                        velocidadActual = 0;
                        llegueAlFinalDelRecorrido = true;
                        break;
                    }

                    // Actualizando el punto
                    puntoGraph = new Graphic(puntoMagico.first, getPointSymbol(velocidadActual));
                    pointsLayer.updateGraphic(pointsLayer.getGraphicIDs()[0], puntoGraph);
                    // Actualizo el buffer
                    Polygon p = GeometryEngine.buffer(posActual, sr, radioBufferActual, Unit.create(LinearUnit.Code.METER));
                    Graphic buffer = new Graphic(p, new SimpleFillSymbol(Color.YELLOW, SimpleFillSymbol.STYLE.SOLID).setAlpha(60));
                    bufferLayer.updateGraphic(bufferLayer.getGraphicIDs()[0], buffer);


                    Thread.sleep(1 * 100); // once every 100 ms
                    // Reseteo el texto de la poblacion
                    Trace.beginSection("Recalculando poblacion");
                    String nuevaPoblacon = Double.toString(recalcularPoblacion());
                    Trace.endSection();
                    actualizarTextoEnUI(nuevaPoblacon);
                    segundosPasados += 1;
                    if (segundosPasados % 2 == 0) {
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

            // Reinicio los botones
            SeekBar seekBar = (SeekBar) findViewById(R.id.seekBar);
            seekBar.setEnabled(false);
            TextView t = (TextView) findViewById(R.id.textViewSpeed);
            t.setEnabled(false);
            TextView t2 = (TextView) findViewById(R.id.textBuffer);
            t2.setEnabled(false);
            SeekBar seekBarBuf = (SeekBar) findViewById(R.id.seekBarBuffer);
            seekBarBuf.setEnabled(false);
            if (llegueAlFinalDelRecorrido) {
                Toast.makeText(MapaActivity.this,
                        "Has llegado al final de tu recorrido",
                        Toast.LENGTH_LONG).show();
            } else {
                Button botonIniciarRecorrido = (Button) findViewById(R.id.buttonIniciarRecorrido);
                botonIniciarRecorrido.setEnabled(true);
            }
        }
    }

    private class CountiesTask extends AsyncTask<GraphicsLayer, Void, Void> {

        @Override
        protected Void doInBackground(final GraphicsLayer... params) {
            SpatialReference sr = SpatialReference.create(102100);
            GraphicsLayer bufferLayer = params[1];

            while (velocidadActual > 0) {
                try {
                    available2.acquire();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Graphic buffer = bufferLayer.getGraphic(bufferLayer.getGraphicIDs()[0]);
                Polygon p = (Polygon) buffer.getGeometry();

                // Voy a verificar si es necesario realizar un pedido
                Trace.beginSection("Viendo si es necesario realizar el pedido de Counties");
                GraphicsLayer countiesLocalLayer = params[0];
                boolean necesitoRecalcular = false;
                if (countiesLocalLayer.getNumberOfGraphics() != 0) { // si no pasa el primer caso
                    Polygon[] intersecciones = new Polygon[countiesLocalLayer.getNumberOfGraphics()];
                    lockCountiesLayer.readLock().lock();
                    int[] graphicIdsOfLocalLayer = countiesLocalLayer.getGraphicIDs();
                    for (int i = 0; i < graphicIdsOfLocalLayer.length; i++) {
                        Geometry county = countiesLocalLayer.getGraphic(graphicIdsOfLocalLayer[i]).getGeometry();
                        intersecciones[i] = (Polygon) GeometryEngine.intersect(county, buffer.getGeometry(), sr);
                        if (intersecciones[i].isEmpty()) {
                            necesitoRecalcular = true;
                            break;
                        }
                    }
                    lockCountiesLayer.readLock().unlock();
                    if (!necesitoRecalcular) {
                        Polygon uniondeIntersecciones = (Polygon) GeometryEngine.union(intersecciones, sr);
                        if (GeometryEngine.equals(uniondeIntersecciones, buffer.getGeometry(), sr)) {
                            //String nuevaPoblacon = Double.toString(recalcularPoblacion());
                            //actualizarTextoEnUI(nuevaPoblacon);
                            Trace.endSection();
                            continue;
                        }
                    }
                }
                Trace.endSection();

                Trace.beginSection("Realizando llamada de counties");
                // necesito hacer la llamada
                final ArcGISFeatureLayer countiesLayerService = new ArcGISFeatureLayer(
                        "http://services.arcgisonline.com/arcgis/rest/services/Demographics/USA_1990-2000_Population_Change/MapServer/3",
                        ArcGISFeatureLayer.MODE.ONDEMAND);

                Query q = new Query();
                q.setGeometry(p);
                q.setSpatialRelationship(SpatialRelationship.INTERSECTS);

                try {
                    termineDePedirLosCounties.acquire();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                countiesLayerService.queryFeatures(q, new CallbackListener<FeatureSet>() {
                    @Override
                    public void onCallback(FeatureSet result) {
                        GraphicsLayer countiesLocalLayer = params[0];
                        SpatialReference sr = SpatialReference.create(102100);

                        // esto que se va a hacer es para que no se remuevan y añadan los counties que ya estan
                        boolean encontreigual = false;
                        boolean sondistintos = true;
                        lockCountiesLayer.readLock().lock();
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
                        lockCountiesLayer.readLock().unlock();

                        if (sondistintos) {
                            lockCountiesLayer.writeLock().lock();
                            countiesLocalLayer.removeAll();
                            for (int i = 0; i < result.getGraphics().length; i++) {
                                Polygon p = (Polygon) result.getGraphics()[i].getGeometry();
                                // Le pongo informacion de poblacion y area
                                Map<String, Object> attributes = new HashMap<String, Object>();
                                attributes.put("Poblacion", result.getGraphics()[i].getAttributes().get("TOTPOP_CY")); // es un Integer
                                attributes.put("Area", result.getGraphics()[i].getAttributes().get("LANDAREA") ); // Es un Double
                                Graphic county = new Graphic(p, new SimpleFillSymbol(Color.CYAN, SimpleFillSymbol.STYLE.SOLID).setAlpha(50), attributes);
                                countiesLocalLayer.addGraphic(county);
                            }
                            lockCountiesLayer.writeLock().unlock();
                        }
                        termineDePedirLosCounties.release();
                     //   String nuevaPoblacion = Double.toString(recalcularPoblacion());
                      //  actualizarTextoEnUI(nuevaPoblacion);
                    }

                    @Override
                    public void onError(Throwable e) {
                        // handle the error
                    }
                });
                try {
                    termineDePedirLosCounties.acquire();
                    Trace.endSection();
                    termineDePedirLosCounties.release();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

    }

    private void actualizarTextoEnUI(final String nuevaPoblacion) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView t = (TextView) findViewById(R.id.textPoblacion);
                t.setText("Población calculada: " + nuevaPoblacion);
                //semaforoTexto.release();
            }
        });
    }


    private Symbol getPointSymbol(double velocidad) {
        float maxHue = 100;
        float speed = (float) velocidad;
        if (velocidad == 0) {
            SimpleMarkerSymbol s = new SimpleMarkerSymbol(Color.WHITE, 10, SimpleMarkerSymbol.STYLE.CIRCLE);
            s.setOutline(new SimpleLineSymbol(Color.BLACK, 1));
            return s;
        } else if (velocidad <= 4000 ) {
            int c = Color.HSVToColor(new float[]{((speed - 1) * (0 - maxHue)) / (4000 - 1) + maxHue, 1.0f, 1.0f});
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

    private double recalcularPoblacion() {
        MapView mMapView = (MapView) findViewById(R.id.map);
        GraphicsLayer countiesLayer = (GraphicsLayer) mMapView.getLayer(2);
        GraphicsLayer bufferLayer = (GraphicsLayer) mMapView.getLayer(3);
        SpatialReference sr = SpatialReference.create(102100);

        Graphic buffer = bufferLayer.getGraphic(bufferLayer.getGraphicIDs()[0]);

        lockCountiesLayer.readLock().lock();

        Polygon[] intersecciones = new Polygon[countiesLayer.getNumberOfGraphics()];
        Graphic[] counties = new Graphic[countiesLayer.getNumberOfGraphics()];
        Double[] porcentajedelCountyEnElBuffer = new Double[countiesLayer.getNumberOfGraphics()]; //  aca guardo que porcentaje del county se encuentra en el buffer

        int[] graphicIdsOfLocalLayer = countiesLayer.getGraphicIDs();
        double resultadoPoblacion = 0;
        if (graphicIdsOfLocalLayer == null || buffer == null) {
            lockCountiesLayer.readLock().unlock();
            return 0;
        }
        Trace.beginSection("(for) Recalculando poblacion)");
        for (int i = 0; i < graphicIdsOfLocalLayer.length; i++) {
            Graphic county = countiesLayer.getGraphic(graphicIdsOfLocalLayer[i]);
            counties[i] = county;
            intersecciones[i] = (Polygon) GeometryEngine.intersect(county.getGeometry(), buffer.getGeometry(), sr);
            double superficieDeInterseccion = GeometryEngine.geodesicArea(intersecciones[i], sr, (AreaUnit) Unit.create(AreaUnit.Code.SQUARE_MILE_US));
            double superficieDeCounty = GeometryEngine.geodesicArea(county.getGeometry(), sr, (AreaUnit) Unit.create(AreaUnit.Code.SQUARE_MILE_US));
            porcentajedelCountyEnElBuffer[i] = new Double(superficieDeInterseccion / superficieDeCounty); // numero entre 0 y 1
            resultadoPoblacion += Math.floor(((Integer) county.getAttributeValue("Poblacion")).doubleValue() * porcentajedelCountyEnElBuffer[i]);
        }
        Trace.endSection();
        lockCountiesLayer.readLock().unlock();
        return resultadoPoblacion;
    }
}
