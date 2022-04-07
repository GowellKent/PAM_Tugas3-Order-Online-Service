package com.kent.pam_firebase;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMapClickListener {

    private GoogleMap gMap;
    private Marker selectedMarker;
    private LatLng selectedPlace;

    private FirebaseFirestore db;

    private TextView txtOrderId, txtSelectedPlace, txtLat, txtLng;
    private EditText editTextName;
    private Button btnUbah, btnOrder;

    private boolean isNewOrder = true;

    int PERMISSION_ID = 44;
    String latAwal, lngAwal;
    double ltaw, lnaw;
    FusedLocationProviderClient mFusedLocationClient;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtOrderId = findViewById(R.id.txtOrderId);
        txtSelectedPlace = findViewById(R.id.txtSelectedPlace);
        editTextName = findViewById(R.id.editText_nama);
        btnUbah = findViewById(R.id.btnUbah);
        btnOrder = findViewById(R.id.btnOrder);

        txtLat = findViewById(R.id.txtLat);
        txtLng = findViewById(R.id.txtLng);

        db = FirebaseFirestore.getInstance();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        getLastLocation();

        btnOrder.setOnClickListener(view -> {saveOrder(); });
        btnUbah.setOnClickListener(view -> {updateOrder(); });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        gMap = googleMap;

        LatLng salatiga = new LatLng(-7.3305, 110.5084);

        selectedPlace = salatiga;
        selectedMarker = gMap.addMarker(new MarkerOptions().position(selectedPlace));

        gMap.animateCamera(CameraUpdateFactory.newLatLngZoom(selectedPlace, 15.0f));

        gMap.setOnMapClickListener(this);
    }

    @Override
    public void onMapClick(@NonNull LatLng latLng) {
        selectedPlace = latLng;
        selectedMarker.setPosition(selectedPlace);
        gMap.animateCamera(CameraUpdateFactory.newLatLng(selectedPlace));

        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try{
            List<Address> addresses = geocoder.getFromLocation(selectedPlace.latitude, selectedPlace.longitude, 1);
            if (addresses != null){
                Address place = addresses.get(0);
                StringBuilder street = new StringBuilder();

                for (int i = 0 ; i <= place.getMaxAddressLineIndex(); i++){
                    street.append(place.getAddressLine(i)).append("\n");
                }

                txtSelectedPlace.setText(street.toString());
            }else{
                Toast.makeText(this, "Could Not Find Address!", Toast.LENGTH_SHORT).show();
            }

        }catch(Exception e){
            Toast.makeText(this, "Error get Address!", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveOrder() {
        Map<String, Object> order = new HashMap<>();
        Map<String, Object> place = new HashMap<>();

        String nama = editTextName.getText().toString();

        place.put("address", txtSelectedPlace.getText().toString());
        place.put("lat", selectedPlace.latitude);
        place.put("lng", selectedPlace.longitude);

        order.put("name", nama);
        order.put("createdDate", new Date());
        order.put("place", place);
        order.put("latAwal", latAwal);
        order.put("lngAwal", lngAwal);
        order.put("alamatAwal", ubahAddr(ltaw, lnaw));

        String orderId = txtOrderId.getText().toString();

        if (isNewOrder){
            db.collection("orders1")
                    .add(order)
                    .addOnSuccessListener(documentReference -> {
                        editTextName.setText("");
                        txtSelectedPlace.setText("Pilih Tempat");
                        txtOrderId.setText(documentReference.getId());
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Gagal Tambah Data Order!", Toast.LENGTH_SHORT).show();
                    });
        }
        else{
            db.collection("orders1").document(orderId)
                    .set(order)
                    .addOnSuccessListener(unused -> {
                        editTextName.setText("");
                        txtSelectedPlace.setText("");
                        txtOrderId.setText(orderId);

                        isNewOrder = true;
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Gagal Ubah Data Order!", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void updateOrder(){
        isNewOrder = false;

        String orderId = txtOrderId.getText().toString();
        DocumentReference order = db.collection("orders1").document(orderId);

        order.get().addOnCompleteListener(task -> {
           if (task.isSuccessful()){
               DocumentSnapshot document = task.getResult();
               if (document.exists()){
                   String name = document.get("name").toString();
                   Map<String, Object> place = (HashMap<String, Object>) document.get("place");

                   editTextName.setText(name);
                   txtSelectedPlace.setText(place.get("address").toString());

                   LatLng resultPlace = new LatLng((double) place.get("lat"), (double) place.get("lng"));
                   selectedPlace = resultPlace;
                   selectedMarker.setPosition(selectedPlace);
                   gMap.animateCamera(CameraUpdateFactory.newLatLng(selectedPlace));
               }
               else{
                   isNewOrder = true;
                   Toast.makeText(this, "Dokumen Tidak Tersedia!", Toast.LENGTH_SHORT).show();
               }
           }
           else{
               Toast.makeText(this, "Gagal Membaca DB!", Toast.LENGTH_SHORT).show();
           }
        });
    }

    //mendapatkan lokasi awal terakhir
    @SuppressLint("MissingPermission")
    private void getLastLocation(){
        if (checkPermissions()){ //cek permission
            if(isLocationEnabled()) { //cek gps dinyalakan
                mFusedLocationClient.getLastLocation().addOnCompleteListener(
                        new OnCompleteListener<Location>() {
                            @Override
                            public void onComplete(@NonNull Task<Location> task) {
                                Location location = task.getResult();
                                if (location == null){
                                    requestNewLocationData();
                                }else{
                                    txtLat.setText(location.getLatitude()+"");
                                    txtLng.setText(location.getLongitude()+"");

                                    latAwal = location.getLatitude()+"";
                                    lngAwal = location.getLongitude()+"";

                                    ltaw = location.getLatitude();
                                    lnaw = location.getLongitude();
                                }
                            }
                        }
                );
            }else{
                Toast.makeText(this, "Lokasi Belum Dinyalakan!", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            }
        }else{
            requestPermission();
        }
    }

    private String ubahAddr(double lat, double lng){
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try{
            List<Address> addr = geocoder.getFromLocation(lat, lng, 1);
            StringBuilder sb = new StringBuilder();
            if (addr.size()>0){

                return addr.get(0).getLocality();
            }

        }catch (Exception e){
            Toast.makeText(this, "Gagal Ubah Alamat Awal!", Toast.LENGTH_SHORT).show();
        }
        return "";
    }

    //mencari data lokasi baru
    @SuppressLint("MissingPermission")
    private void requestNewLocationData(){
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(0);
        mLocationRequest.setFastestInterval(0);
        mLocationRequest.setNumUpdates(1);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mFusedLocationClient.requestLocationUpdates(
                mLocationRequest, mLocationCallback, Looper.myLooper()
        );
    }

    private LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(@NonNull LocationResult locationResult) {
            Location mLastLocation = locationResult.getLastLocation();
            txtLat.setText(mLastLocation.getLatitude()+"");
            txtLng.setText(mLastLocation.getLongitude()+"");

        }
    };

    //cek permission coarse dan Fine apabila 2 2 nya granted return value true
    private boolean checkPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            return true;
        }
        return false;
    }

    //request permission untuk lokasi
    private void requestPermission() {
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION},
                PERMISSION_ID
        );
    }

    //return value untuk cek apabila fungsi GPS nyala
    private boolean isLocationEnabled(){
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_ID) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                getLastLocation();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (checkPermissions()){
            getLastLocation();
        }
    }
}