package cse4340.ble_application;

import android.app.Activity;
import android.content.ContextWrapper;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Scanner;

public class FileServerAsyncTaskRetrieve extends AsyncTask<Void, Void, String> {
    InputStream inputstream;
    String files;

    ArrayList<String> file_array;

    int bytesRead;
    int current = 0;

    File file_location  =  new File(Environment.getExternalStorageDirectory() + "/shared");

    Activity fileBrowser;
    private boolean gotData = false;

    public FileServerAsyncTaskRetrieve() {

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
            serverSocket.bind(new InetSocketAddress(7777)); // <-- now bind it

            Socket clientSocket = null;
            Log.d("FileServerRetrieve","Waiting for client to connect");
            clientSocket = serverSocket.accept();
            Log.d("FileServerRetrieve","Client accepted connection");

            inputstream = clientSocket.getInputStream();
            String fileName = inBoundMessage(inputstream);
            File file = new File(file_location + "/" + fileName.trim());

            Log.d("FileServerRetrieve","Sending file to client..." + file);


            byte[] bytes = new byte[(int) file.length()];
            BufferedInputStream bis;

            bis = new BufferedInputStream(new FileInputStream(file));
            bis.read(bytes, 0, bytes.length);
            OutputStream os = clientSocket.getOutputStream();
            os.write(bytes, 0, bytes.length);
            gotData = true;
            os.flush();
            clientSocket.close();



/*
            while (true) {

                Socket clientSocket = null;
                Log.d("FileServerTaskRetrieve", "waiting to download files....");
                clientSocket = serverSocket.accept();

                InputStream in = clientSocket.getInputStream();
                Log.d("FileServerTaskRetrieve", in.toString());

                DataInputStream clientData = new DataInputStream(in);

                String fileName = clientData.readUTF();
                OutputStream output = new FileOutputStream(fileName);
                long size = clientData.readLong();
                byte[] buffer = new byte[1024];
                while (size > 0 && (bytesRead = clientData.read(buffer, 0, (int) Math.min(buffer.length, size))) != -1) {
                    output.write(buffer, 0, bytesRead);
                    size -= bytesRead;
                }

                // Closing the FileOutputStream handle
                output.close();
            }

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

            //serverSocket.close();
            */
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;

    }

    protected boolean onPostExecute() {
        //Do stuff with data..
        return gotData;

    }

    private String inBoundMessage(final InputStream input) {

        final int BUFFER_SIZE = 1024;
        byte[] buffer = new byte[BUFFER_SIZE];
        int bytes = 0;
        int b = BUFFER_SIZE;
        String message = "";


        // globally

        try {
            if (input != null) {

                Log.d("FileServerRetrieve", "reading string....");
                bytes = input.read(buffer, bytes, BUFFER_SIZE - bytes);
                message = new String(buffer,"UTF-8");
                Log.d("FileServerRetrieve", message.trim());

            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return message;


    }

}