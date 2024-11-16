package kr.akotis.recyclehelper;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class RecycleGuideActivity extends AppCompatActivity implements View.OnClickListener {

    private ImageButton btnPaper;
    private ImageButton btnPet;
    private ImageButton btnCan;
    private ImageButton btnGlass;
    private ImageButton btnVinyl;
    private ImageButton btnStyrofoam;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_recycle_guide);

        btnPaper = findViewById(R.id.btn_paper_guide);
        btnPet = findViewById(R.id.btn_plastic_guide);
        btnCan = findViewById(R.id.btn_can_guide);
        btnGlass = findViewById(R.id.btn_glass_guide);
        btnVinyl = findViewById(R.id.btn_vinyl_guide);
        btnStyrofoam = findViewById(R.id.btn_styrofoam_guide);

        btnPaper.setOnClickListener(this);
        btnPet.setOnClickListener(this);
        btnCan.setOnClickListener(this);
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
            Intent paperIntent = new Intent(RecycleGuideActivity.this, PaperGuideActivity.class);
            startActivity(paperIntent);
        } else if (view == btnPet) {
            Log.v("Plastic", "Plastic");
            Intent petIntent = new Intent(RecycleGuideActivity.this, PlasticGuideActivity.class);
            startActivity(petIntent);
        } else if (view == btnCan) {
            Log.v("Can", "Can");
            Intent metalIntent = new Intent(RecycleGuideActivity.this, CanGuideActivity.class);
            startActivity(metalIntent);
        } else if (view == btnGlass) {
            Log.v("Glass", "Glass");
            Intent glassIntent = new Intent(RecycleGuideActivity.this, GlassGuideActivity.class);
            startActivity(glassIntent);
        } else if (view == btnVinyl) {
            Log.v("Vinyl", "Vinyl");
            Intent vinylIntent = new Intent(RecycleGuideActivity.this, VinylGuideActivity.class);
            startActivity(vinylIntent);
        } else if (view == btnStyrofoam) {
            Log.v("Styrofoam", "Styrofoam");
            Intent styrofoamIntent = new Intent(RecycleGuideActivity.this, StyrofoamGuideActivity.class);
            startActivity(styrofoamIntent);
        }
    }
}