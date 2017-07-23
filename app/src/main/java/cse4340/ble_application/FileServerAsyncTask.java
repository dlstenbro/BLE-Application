package cse4340.ble_application;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Adapter;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;

public class FileServerAsyncTask extends AsyncTask<Void, Void, String> {
    InputStream inputstream;
    String files;

    ArrayList<String> file_array;
    ArrayAdapter file_array_adapter;
    ListView file_list;

    public FileServerAsyncTask(ListView view, ArrayAdapter adapterName, ArrayList<String> file_info) {
        file_list = view;
        file_array_adapter = adapterName;
        file_array = file_info;

    }

    @Override
    protected String doInBackground(Void... params) {

        try {

            /**
             * Create a server socket and wait for client connections. This
             * call blocks until a connection is accepted from a client
             *
             */

            ServerSocket serverSocket = new ServerSocket();
            serverSocket.setReuseAddress(true);
            serverSocket.bind(new InetSocketAddress(8888)); // <-- now bind it

            Log.d("FileServerAsyncTask", "waiting for client to connect....");
            Socket client = serverSocket.accept();


            Log.d("FileServerAsyncTask", "Connection was accepted!");

            inputstream = client.getInputStream();
            file_array.clear();
            String  response = convertStreamToString(inputstream);
            Log.d("inputStreamServer", response);

            Log.d("file_array", file_array.toString());

            client.close();
            serverSocket.close();


            /**
             * If this code is reached, a client has connected and transferred data
             * Save the input stream from the client as a JPEG file
             */
/*
            final File f = new File(Environment.getExternalStorageDirectory() + "/"
                    + context.getPackageName() + "/wifip2pshared-" + System.currentTimeMillis()
                    + ".jpg");

            File dirs = new File(f.getParent());
            if (!dirs.exists())
                dirs.mkdirs();
            f.createNewFile();

            InputStream inputstream = client.getInputStream();

            serverSocket.close();

            return f.getAbsolutePath();
 */
            //serverSocket.close();

        } catch (IOException e) {
            Log.d("FileServerAsyncTask", e.getMessage());
            return null;
        }


        return null;
    }

    protected ArrayAdapter onPostExecute() {
        //Do stuff with data..

        file_list.invalidate();
        return file_array_adapter;

    }

    private String convertStreamToString(InputStream is) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();

        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
                file_array.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }

    public String getFileNames(){
        return files;
    }

    private void convertTextToArray(){
        Scanner linereader = new Scanner(files);
        String line = linereader.nextLine();

        file_array = new ArrayList<>();

        while (linereader.hasNext()){
            file_array.add(line);
            line = linereader.nextLine();
        }
    }

    public ArrayList<String> getArrayList(){
        return file_array;
    }
}