package com.ines.banque_serveur;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;

public class MainActivity extends AppCompatActivity {
    //base de donnée SQLite
    private static SQLiteDatabase db;
    //socket , ServerSocket etc..
    private  Socket socket;
    private ServerSocket serverSocket;
    private int serverPort=1234;
    private Button cliButton,opButton;
    private TextView cliTextArea;
    private TextView opTextArea;
    private boolean quitter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        quitter=true;


        // création des Base de données et tables
        db = openOrCreateDatabase("banque",MODE_PRIVATE,null);
        //db.execSQL("DROP TABLE IF EXISTS clients;");
        db.execSQL("CREATE TABLE IF NOT EXISTS clients (\n"
                + "	rib integer PRIMARY KEY,\n"
                + " cin text NOT NULL,\n"
                + " nom text NOT NULL,\n"
                + "	password text NOT NULL,\n"
                + "	credit real NOT NULL\n"
                + ");");
        //db.execSQL("DROP TABLE IF EXISTS operations;");
        db.execSQL("CREATE TABLE IF NOT EXISTS operations (\n"
                + "	num integer PRIMARY KEY,\n"
                + " cincli text NOT NULL, \n"
                + " date text NOT NULL,\n"
                + " valeur real NOT NULL,\n"
                + "	info text NOT NULL\n"
                + ");");

        //accepter les clients - plusieurs
        Thread accept = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    serverSocket = new ServerSocket(serverPort);
                    while (true) {
                        socket = serverSocket.accept();
                        new ClientThread(socket,db).start();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        accept.start();

        initButtons();


    }
    //bouton retour : décider de quitter au de revenir
    @Override
    public void onBackPressed()
    {
        if(quitter){
            finishAffinity();
        }
        else {
            setContentView(R.layout.activity_main);
            initButtons(); //initialiser les actions des boutons pour afficher les clients et les opérations
        }
    }

    public void initButtons(){
        cliButton=findViewById(R.id.clients);
        cliButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setContentView(R.layout.clients);
                quitter=false;
                Cursor c = db.rawQuery("SELECT * FROM clients;", null);
                cliTextArea = findViewById(R.id.cliTextView);
                while(c.moveToNext()){
                    String nom,cin,pwd;
                    int rib;
                    float credit;
                    nom = c.getString(c.getColumnIndex("nom"));
                    cin=c.getString(c.getColumnIndex("cin"));
                    pwd=c.getString(c.getColumnIndex("password"));
                    credit = c.getFloat(c.getColumnIndex("credit"));
                    rib = c.getInt(c.getColumnIndex("rib"));
                    String s = "____\n"+"Nom : "+nom+"\nCin : "+cin+"\nRIB : "+rib+"\nCrédit : "+credit+" DT\nMot de passe : "+pwd+"\n____\n";
                    cliTextArea.append(s);
                }
            }
        });

        opButton=findViewById(R.id.operations);
        opButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setContentView(R.layout.operations);
                quitter=false;
                Cursor c = db.rawQuery("SELECT * FROM operations;", null);
                cliTextArea = findViewById(R.id.opTextView);
                while(c.moveToNext()){
                    String info,date,cin;
                    int num;
                    float val;
                    info = c.getString(c.getColumnIndex("info"));
                    cin=c.getString(c.getColumnIndex("cincli"));
                    date=c.getString(c.getColumnIndex("date"));
                    val = c.getFloat(c.getColumnIndex("valeur"));
                    num = c.getInt(c.getColumnIndex("num"));
                    String s = "____\n"+"Numéro : "+num+"\nCin du bénéficiaire : "+cin+"\nDate : "+date+"\n"+info+"\nValeur : "+val+" DT\n____\n";
                    cliTextArea.append(s);
                }
            }
        });
    }
}