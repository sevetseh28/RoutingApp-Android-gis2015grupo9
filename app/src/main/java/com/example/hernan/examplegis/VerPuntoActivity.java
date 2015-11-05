package com.example.hernan.examplegis;

import android.content.Intent;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.esri.android.map.GraphicsLayer;
import com.esri.android.map.MapView;
import com.esri.android.map.event.OnStatusChangedListener;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.SpatialReference;
import com.esri.core.map.Graphic;
import com.esri.core.symbol.SimpleMarkerSymbol;

public class VerPuntoActivity extends AppCompatActivity {
    MapView mapa;
    Graphic puntoGraph;
    Point punto;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ver_punto);

        Intent intent = getIntent();
        SpatialReference sr = SpatialReference.create(102100);
        punto = GeometryEngine.project(intent.getDoubleExtra("posX", 0), intent.getDoubleExtra("posY", 0), sr);
        SimpleMarkerSymbol s = new SimpleMarkerSymbol(Color.BLUE, 10, SimpleMarkerSymbol.STYLE.CIRCLE);
        puntoGraph = new Graphic(punto, s);
        GraphicsLayer pointLayer = new GraphicsLayer(); // creo la capa de puntos
        mapa = (MapView) findViewById(R.id.mapViewPoint);
        mapa.addLayer(pointLayer);
        //mapa.setExtent(punto);
        mapa.setOnStatusChangedListener(new OnStatusChangedListener() {
            private static final long serialVersionUID = 1L;

            public void onStatusChanged(Object source, STATUS status) {
                if (OnStatusChangedListener.STATUS.INITIALIZED == status && source == mapa) {
                    GraphicsLayer pointLayer = (GraphicsLayer) mapa.getLayer(1);
                    pointLayer.addGraphic(puntoGraph);
                    mapa.centerAt(punto, true);
                    mapa.setScale(25000, true);
                }
            }
        });
    }
}
