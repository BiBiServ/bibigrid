package de.unibi.cebitec.bibigrid.core;

import de.unibi.cebitec.bibigrid.core.util.Status;

import java.util.concurrent.ConcurrentHashMap;

public class DataBase {

    public ConcurrentHashMap<String, Status> status = new ConcurrentHashMap();
    private static DataBase db;

    public static DataBase getDataBase(){
        if (db == null) {
            db = new DataBase();
        }
        return db;
    }
}
