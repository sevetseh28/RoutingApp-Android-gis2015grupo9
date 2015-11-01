package com.example.hernan.examplegis;

import android.provider.BaseColumns;

/**
 * Created by Hernan on 31-Oct-15.
 */
public class RutaContract {
    // To prevent someone from accidentally instantiating the contract class,
    // give it an empty constructor.
    public RutaContract() {}

    /* Inner class that defines the table contents */
    public static abstract class RutaEntry implements BaseColumns {
        public static final String TABLE_NAME = "Rutas";
        public static final String COLUMN_NAME_ENTRY_ID = "objectid";
        public static final String COLUMN_NAME_DESC = "descripcion";
    }
}
