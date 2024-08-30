package kr.kro.barrierfree.recyclehelper;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class RecyclingGuideActivity extends AppCompatActivity implements View.OnClickListener {

    private ImageButton btnPaper;
    private ImageButton btnPet;
    private ImageButton btnMetal;
    private ImageButton btnGlass;
    private ImageButton btnVinyl;
    private ImageButton btnStyrofoam;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_recycling_guide);

        btnPaper = findViewById(R.id.btn_paper);
        btnPet = findViewById(R.id.btn_pet);
        btnMetal = findViewById(R.id.btn_metal);
        btnGlass = findViewById(R.id.btn_glass);
        btnVinyl = findViewById(R.id.btn_vinyl);
        btnStyrofoam = findViewById(R.id.btn_styrofoam);

        btnPaper.setOnClickListener(this);
        btnPet.setOnClickListener(this);
        btnMetal.setOnClickListener(this);
        btnGlass.setOnClickListener(this);
        btnVinyl.setOnClickListener(this);
        btnStyrofoam.setOnClickListener(this);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    @Override
    public void onClick(View view) {
        if (view == btnPaper) {
            Log.v("Paper", "Paper");
            Intent paperIntent = new Intent(RecyclingGuideActivity.this, PaperActivity.class);
            startActivity(paperIntent);
        } else if (view == btnPet) {
            Log.v("Pet", "Pet");
            Intent petIntent = new Intent(RecyclingGuideActivity.this, PetActivity.class);
            startActivity(petIntent);
        } else if (view == btnMetal) {
            Log.v("Metal", "Metal");
            Intent metalIntent = new Intent(RecyclingGuideActivity.this, MetalActivity.class);
            startActivity(metalIntent);
        } else if (view == btnGlass) {
            Log.v("Glass", "Glass");
            Intent glassIntent = new Intent(RecyclingGuideActivity.this, GlassActivity.class);
            startActivity(glassIntent);
        } else if (view == btnVinyl) {
            Log.v("Vinyl", "Vinyl");
            Intent vinylIntent = new Intent(RecyclingGuideActivity.this, VinylActivity.class);
            startActivity(vinylIntent);
        } else if (view == btnStyrofoam) {
            Log.v("Styrofoam", "Styrofoam");
            Intent styrofoamIntent = new Intent(RecyclingGuideActivity.this, StyrofoamActivity.class);
            startActivity(styrofoamIntent);
        }
    }
}