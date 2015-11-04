package com.example.hernan.examplegis;

import android.content.Intent;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.esri.android.map.GraphicsLayer;
import com.esri.android.map.MapView;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.SpatialReference;
import com.esri.core.map.Graphic;
import com.esri.core.symbol.SimpleMarkerSymbol;

public class VerPuntoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ver_punto);

        Intent intent = getIntent();
        SpatialReference sr = SpatialReference.create(102100);
        Point punto = GeometryEngine.project(intent.getDoubleExtra("posX", 0), intent.getDoubleExtra("posY", 0), sr);
        GraphicsLayer pointLayer = new GraphicsLayer(); // creo la capa de puntos
        MapView mapa = (MapView) findViewById(R.id.mapViewPoint);
        mapa.addLayer(pointLayer);
        SimpleMarkerSymbol s = new SimpleMarkerSymbol(Color.BLUE, 10, SimpleMarkerSymbol.STYLE.CIRCLE);
        Graphic puntoGraph = new Graphic(punto, s);
        pointLayer.addGraphic(puntoGraph);
        mapa.centerAt(punto, true);
        mapa.setExtent(punto);
    }
}
