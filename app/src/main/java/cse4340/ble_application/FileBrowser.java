package cse4340.ble_application;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;

public class FileBrowser extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_browser);

        getFiles();

    }

    public void getFiles(){

        File myDirectory = new File("path to some directory");
        File[] directories = myDirectory.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.isDirectory();
            }
        });

    }


}
