package com.ines.banque_serveur;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

public class ClientThread extends Thread{
    ObjectOutputStream objectOutputStream;
    ObjectInputStream objectInputStream;
    SQLiteDatabase db;
    public ClientThread(Socket socket, SQLiteDatabase db){//initialisation du base , socket
        this.db=db;
        try {
            OutputStream outputStream = socket.getOutputStream();
            objectOutputStream = new ObjectOutputStream(outputStream);
            InputStream inputStream = socket.getInputStream();
            objectInputStream = new ObjectInputStream(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void run(){
        //liste de réception
        LinkedList list;
        //pour formater les objets date
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy_HH:mm:ss", Locale.getDefault());
        while(true){

            try {
                try {
                    list =(LinkedList) objectInputStream.readObject();
                }
                catch (EOFException e){
                    continue;
                }


                String type = (String) list.get(0);
                //répondre à création
                if(type.equals("creer")){
                    LinkedList info = new LinkedList();
                    Cursor c = db.rawQuery("SELECT * FROM clients WHERE cin = '"+list.get(1)+"';", null);
                    if(c.moveToNext()){
                        info.add("cin existe");
                        objectOutputStream.writeObject(info);
                    }
                    else {
                        ContentValues row = new ContentValues();
                        row.put("cin", (String) list.get(1));
                        row.put("nom", (String) list.get(2));
                        row.put("password", (String) list.get(3));
                        row.put("credit", 0);
                        db.insert("clients", null, row);
                        info.add("creation terminee");
                        objectOutputStream.writeObject(info);
                    }
                }

                //répondre à connexion
                else if(type.equals("connecter")){
                    Cursor c = db.rawQuery("SELECT * FROM clients WHERE cin = '"+list.get(1)+"';", null);
                    LinkedList info = new LinkedList();
                    boolean correcte = c.moveToNext() && c.getString(c.getColumnIndex("password")).equals((String)list.get(2));
                    if(correcte) {
                        info.add("connecter correcte");
                        info.add(c.getInt(c.getColumnIndex("rib")));
                        info.add(c.getString(c.getColumnIndex("cin")));
                        info.add(c.getString(c.getColumnIndex("nom")));
                        info.add(c.getString(c.getColumnIndex("password")));
                        info.add(c.getFloat(c.getColumnIndex("credit")));
                    }
                    else{
                        info.add("connecter incorrecte");
                    }
                    objectOutputStream.writeObject(info);
                }
                //répondre à débiter
                else if(type.equals("debiter")){
                    String cin = (String) list.get(1);
                    float val = (float)list.get(2);
                    Cursor c = db.rawQuery("SELECT credit,nom FROM clients WHERE cin = '"+cin+"';", null);
                    c.moveToNext();
                    float s = c.getFloat(c.getColumnIndex("credit")) - val;
                    String nom = c.getString(c.getColumnIndex("nom"));
                    String strSQL = "UPDATE clients SET credit = "+s+" WHERE cin = "+(String) list.get(1) ;
                    db.execSQL(strSQL);
                    LinkedList info = new LinkedList();
                    info.add("succes");
                    objectOutputStream.writeObject(info);
                    String dateEtHeure = sdf.format(new Date());
                    ContentValues row = new ContentValues();
                    row.put("date",dateEtHeure);
                    row.put("cincli",cin);
                    row.put("valeur",val);
                    row.put("info","[Débit] de la part de : "+nom);
                    db.insert("operations", null, row);
                }
                //répondre à créditer
                else if(type.equals("crediter")){
                    String cin = (String) list.get(1);
                    float val = (float)list.get(2);
                    Cursor c = db.rawQuery("SELECT credit,nom FROM clients WHERE cin = '"+cin+"';", null);
                    c.moveToNext();
                    float s = c.getFloat(c.getColumnIndex("credit")) + val;
                    String nom = c.getString(c.getColumnIndex("nom"));
                    String strSQL = "UPDATE clients SET credit = "+s+" WHERE cin = "+(String) list.get(1) ;
                    db.execSQL(strSQL);
                    LinkedList info = new LinkedList();
                    info.add("succes");
                    objectOutputStream.writeObject(info);
                    String dateEtHeure = sdf.format(new Date());
                    ContentValues row = new ContentValues();
                    row.put("date",dateEtHeure);
                    row.put("cincli",cin);
                    row.put("valeur",val);
                    row.put("info","[Crédit] de la part de : "+nom);
                    db.insert("operations", null, row);
                }
                //répondre à transférer
                else if(type.equals("transferer")){
                    Cursor c = db.rawQuery("SELECT * FROM clients WHERE rib = '"+(int)list.get(2)+"';", null);;
                    if(!c.moveToNext()){
                        LinkedList info = new LinkedList();
                        info.add("rib invalide");
                        objectOutputStream.writeObject(info);
                    }
                    else{
                        String cin = (String) list.get(1);
                        float val = (float)list.get(3);
                        float s1 = val;
                        float s2 = val;
                        String txtInfo = "[Transfert] de ";
                        c = db.rawQuery("SELECT credit,nom FROM clients WHERE cin = '"+cin+"';", null);
                        c.moveToNext();
                        s1 = c.getFloat(c.getColumnIndex("credit")) - s1;
                        txtInfo +=c.getString(c.getColumnIndex("nom"))+ " à ";
                        String strSQL = "UPDATE clients SET credit = "+s1+" WHERE cin = "+(String) list.get(1) ;
                        db.execSQL(strSQL);

                        c = db.rawQuery("SELECT credit,nom FROM clients WHERE rib = '"+(int)list.get(2)+"';", null);
                        c.moveToNext();
                        s2 = c.getFloat(c.getColumnIndex("credit")) + s2;
                        txtInfo += c.getString(c.getColumnIndex("nom"));
                        strSQL = "UPDATE clients SET credit = "+s2+" WHERE rib = "+(int) list.get(2) ;
                        db.execSQL(strSQL);
                        LinkedList info = new LinkedList();
                        info.add("succes");
                        objectOutputStream.writeObject(info);
                        String dateEtHeure = sdf.format(new Date());
                        ContentValues row = new ContentValues();
                        row.put("date",dateEtHeure);
                        row.put("cincli",cin);
                        row.put("valeur",val);
                        row.put("info",txtInfo);
                        db.insert("operations", null, row);
                    }
                }
            }
            catch (ClassNotFoundException | IOException e){
                e.printStackTrace();
            }
        }


    }
}
