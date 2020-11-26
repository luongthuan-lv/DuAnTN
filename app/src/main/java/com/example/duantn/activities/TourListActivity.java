package com.example.duantn.activities;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.CompositePageTransformer;
import androidx.viewpager2.widget.MarginPageTransformer;
import androidx.viewpager2.widget.ViewPager2;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.example.duantn.R;
import com.example.duantn.adapter.TourAdapter;
import com.example.duantn.morder.Tour;
import com.example.duantn.morder.TourInfor;
import com.example.duantn.network.RetrofitService;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.example.duantn.view.CustomImageButton;
import com.facebook.login.LoginManager;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class TourListActivity extends BaseActivity implements View.OnClickListener {


    private ViewPager2 viewPager2;
    private List<Tour> tourList = new ArrayList<>();
    private TourAdapter tourAdapter;
    private EditText edt_search;
    private ImageView btnSearch;
    private ImageView imgAvatar;
    private String urlAvatar, name, id_user;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private static final int REQUEST_CODE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tour_list);
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE);
        initDialogLoading();
        showDialogLoading();
        viewPager2 = findViewById(R.id.viewPager2);
        edt_search = findViewById(R.id.edt_search);
        btnSearch = findViewById(R.id.btnSearch);
        imgAvatar = findViewById(R.id.imgAvatar);

        findViewById(R.id.layout).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               closeKeyboard();
            }
        });
        imgAvatar.setOnClickListener(this);
        findViewById(R.id.btnSearch).setOnClickListener(this);
        btnSearch.getLayoutParams().width = getSizeWithScale(45);
        btnSearch.getLayoutParams().height = getSizeWithScale(45);
        edt_search.getLayoutParams().width = getSizeWithScale(245);
        edt_search.getLayoutParams().height = getSizeWithScale(40);
        edt_search.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        edt_search.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                switch (actionId) {
                    case EditorInfo.IME_ACTION_SEARCH:
                        search();
                        closeKeyboard();
                        break;
                }
                return false;
            }
        });
        getIntent_bundle();
        getRetrofit();


    }

    private void getIntent_bundle() {
        Intent intent = getIntent();
        urlAvatar = intent.getStringExtra("urlAvatar");
        name = intent.getStringExtra("name");
        id_user = intent.getStringExtra("user_id");
        if (urlAvatar.equals("")) {
            Glide.with(this).load(R.drawable.img_avatar).transform(new RoundedCorners(80)).into(imgAvatar);
        } else {
            Glide.with(this).load(urlAvatar).transform(new RoundedCorners(80)).into(imgAvatar);
        }
    }

    private void getRetrofit() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://tourintro.herokuapp.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        RetrofitService retrofitService = retrofit.create(RetrofitService.class);

        retrofitService.getTourList(getIdLanguage()).enqueue(new Callback<List<Tour>>() {
            @Override
            public void onResponse(Call<List<Tour>> call, Response<List<Tour>> response) {
                tourList = response.body();
                setAdapter();
                setViewPager2();
            }

            @Override
            public void onFailure(Call<List<Tour>> call, Throwable t) {
                Toast.makeText(TourListActivity.this, t.getMessage(), Toast.LENGTH_SHORT).show();
                setAdapter();
                setViewPager2();
            }
        });

    }




    private void setAdapter() {
        tourAdapter = new TourAdapter(tourList, edt_search, this, new TourAdapter.OnClickItemListener() {
            @Override
            public void onClicked(int position) {
                if (isConnected(false)) {
                    setIdTour(tourList.get(position).getId());
                    Intent intent = new Intent(TourListActivity.this, TourIntroduceActivity.class);
                    Bundle bundle = new Bundle();
                    bundle.putString("tour_name", tourList.get(position).getCateName());
                    bundle.putString("avatar", tourList.get(position).getAvatar());
                    bundle.putInt("rating", 5);
                    bundle.putString("router", tourList.get(position).getRouter());
                    intent.putExtra("urlAvatar", urlAvatar);
                    intent.putExtra("name", name);
                    intent.putExtra("user_id", id_user);
                    intent.putExtras(bundle);
                    startActivity(intent);
                } else showDialogNoInternet();

            }

            @Override
            public void onSwitched(boolean isChecked) {

            }
        });
    }

    private void setViewPager2() {
        viewPager2.setAdapter(tourAdapter);
        viewPager2.setClipToPadding(false);
        viewPager2.setClipChildren(false);
        viewPager2.setOffscreenPageLimit(3);
        viewPager2.getChildAt(0).setOverScrollMode(RecyclerView.OVER_SCROLL_NEVER);
        CompositePageTransformer compositePageTransformer = new CompositePageTransformer();
        compositePageTransformer.addTransformer(new MarginPageTransformer(50));
        compositePageTransformer.addTransformer(new ViewPager2.PageTransformer() {
            @Override
            public void transformPage(@NonNull View page, float position) {
                float r = 1 - Math.abs(position);
                page.setScaleY(0.80f + r * 0.2f);

            }
        });
        viewPager2.setPageTransformer(compositePageTransformer);
        dismissDialog();
    }

    @Override
    public void onClick(View v) {
        if (isConnected(false)) {
            switch (v.getId()) {
                case R.id.btnSearch:
                    search();
                    closeKeyboard();
                    break;
                case R.id.imgAvatar:
                    showDialogLogout(this, name);
                    break;

            }
        } else {
            showDialogNoInternet();
        }
    }
    private void search() {
            tourAdapter.getFilter().filter(edt_search.getText().toString());
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finishAffinity();
    }

}

