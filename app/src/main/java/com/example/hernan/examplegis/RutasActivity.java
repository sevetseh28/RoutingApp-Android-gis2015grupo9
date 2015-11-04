package com.example.hernan.examplegis;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

/**
 * Created by Hernan on 31-Oct-15.
 */
public class RutasActivity extends AppCompatActivity {
    private int selectedPosition;
    private Cursor rutas;
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ruta);

        // Voy a guardar el object id en la db sqlite
        AppDbHelper mDbHelper = new AppDbHelper(getApplicationContext());
        // Gets the data repository in write mode
        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        Cursor rutas = db.rawQuery("SELECT * FROM Rutas", null);
        this.rutas = rutas;
        // Find ListView to populate
        ListView lvItems = (ListView) findViewById(R.id.rutasView);
        // Setup cursor adapter using cursor from last step
        final SimpleCursorAdapter dataAdapter = new SimpleCursorAdapter(
                getApplicationContext(),
                R.layout.list_rutas,
                rutas,
                new String[]{RutaContract.RutaEntry.COLUMN_NAME_DESC},
                new int[]{android.R.id.text1},
                0);
        // Attach cursor adapter to the ListView
        lvItems.setAdapter(dataAdapter);


        lvItems.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapter, View v, int position,
                                    long arg3) {
                RutasActivity.this.selectedPosition = position;
                Button buttonCargarRuta = (Button) findViewById(R.id.buttonCargarRuta);
                buttonCargarRuta.setEnabled(true);
                Button buttonBorrarRuta = (Button) findViewById(R.id.buttonBorrarRuta);
                buttonBorrarRuta.setEnabled(true);
                //String value = (String)adapter.getItemAtPosition(position);
                // assuming string and if you want to get the value on click of list item
                // do what you intend to do on click of listview row
            }
        });

        Button buttonCargarRuta = (Button) findViewById(R.id.buttonCargarRuta);
        buttonCargarRuta.setEnabled(false);
        Button buttonBorrarRuta = (Button) findViewById(R.id.buttonBorrarRuta);
        buttonBorrarRuta.setEnabled(false);

        buttonCargarRuta.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                // obtain the clicked point from a MouseEvent
                Cursor rutas = dataAdapter.getCursor();
                rutas.moveToPosition(selectedPosition);
                long objectid = (Long.valueOf(rutas.getString(rutas.getColumnIndex("objectid")))).longValue();

                Intent intent = new Intent(RutasActivity.this, MapaActivity.class);
                intent.putExtra("OBJECT_ID_RUTA", objectid);
                RutasActivity.this.startActivity(intent);
            }
        });

        buttonBorrarRuta.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                AppDbHelper mDbHelper = new AppDbHelper(getApplicationContext());
                // Gets the data repository in write mode
                SQLiteDatabase db = mDbHelper.getWritableDatabase();
                // obtain the clicked point from a MouseEvent
                Cursor rutas = dataAdapter.getCursor();
                rutas.moveToPosition(selectedPosition);
                long objectid = (Long.valueOf(rutas.getString(rutas.getColumnIndex("objectid")))).longValue();
                db.delete("Rutas", "objectid = " + rutas.getString(rutas.getColumnIndex("objectid")), null);
                rutas = db.rawQuery("SELECT * FROM Rutas", null);
                dataAdapter.swapCursor(rutas);
                if (rutas.getCount() == 0) {
                    Button buttonCargarRuta = (Button) findViewById(R.id.buttonCargarRuta);
                    buttonCargarRuta.setEnabled(false);
                    Button buttonBorrarRuta = (Button) findViewById(R.id.buttonBorrarRuta);
                    buttonBorrarRuta.setEnabled(false);
                }
            }
        });

    }
}
