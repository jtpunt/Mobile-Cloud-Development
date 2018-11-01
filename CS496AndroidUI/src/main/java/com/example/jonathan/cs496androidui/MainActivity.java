package com.example.jonathan.cs496androidui;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.content.Intent;
public class MainActivity extends AppCompatActivity {
    // Override the onCreate method of the AppCompatActivity
    @Override
    // If our state is saved, it'll get passed here. Helps store old settings
    protected void onCreate(Bundle savedInstanceState) {
        // We pass savedInstanceState to the parent constructor
        super.onCreate(savedInstanceState); // initialize our activity
        // R is a set of resources, where +id syntax
        setContentView(R.layout.activity_main); // set the design for our activity res->layout->activity_main
        // Here we are adding some interactivity to our page
        Button btn_hor = (Button) findViewById(R.id.horizontal); // cast to Button variable type
        btn_hor.setOnClickListener(new View.OnClickListener(){
            // The new click listener created will have an OnClick method that gets passed the view of what was just clicked
            @Override
            public void onClick(View v) {
                // Create a new activity - intent - and include the context of the current app (MainActivity - which contains variables, state, info about current resources/res labeling)
                // Because we are inside of a difference instance, this OnClickListener, we need to reference explicitly the parent class here 'MainActivity' and use it's constructor
                // We also list what we want to be the target of the intent (display_horizontal)
                Intent intent = new Intent(MainActivity.this, display_horizontal.class);
                startActivity(intent);
            }
        });

        Button btn_vert = (Button) findViewById(R.id.verticle);
        btn_vert.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, display_verticle.class);
                startActivity(intent);
            }
        });

        Button btn_grid = (Button) findViewById(R.id.grid);
        btn_grid.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, display_grid.class);
                startActivity(intent);
            }
        });

        Button btn_rel = (Button) findViewById(R.id.relative);
        btn_rel.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, display_relative.class);
                startActivity(intent);
            }
        });
    }
}
