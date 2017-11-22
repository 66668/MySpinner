package com.demo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import lib.demo.spinner.MaterialSpinner;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        MaterialSpinner spinner = (MaterialSpinner) findViewById(R.id.spinner);
        String[] typeArrays = getResources().getStringArray(R.array.identify_types);
        spinner.setItems(typeArrays);
        spinner.setSelectedIndex(0);
        spinner.setOnItemSelectedListener(new MaterialSpinner.OnItemSelectedListener<String>() {

            @Override
            public void onItemSelected(MaterialSpinner view, int position, long id, String item) {

            }
        });

        spinner.setOnNothingSelectedListener(new MaterialSpinner.OnNothingSelectedListener() {

            @Override
            public void onNothingSelected(MaterialSpinner spinner) {
                spinner.getSelectedIndex();
            }
        });

    }
}
