package io.github.intimidate.decamincruise;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.PreferenceManager;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.libraries.places.api.Places;

import java.util.Objects;

import io.github.intimidate.decamincruise.login.LoginActivity;
import io.github.intimidate.decamincruise.remote.ApiManager;
import io.github.intimidate.decamincruise.remote.VerifyTokenBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DriverMapActivity extends FragmentActivity implements OnMapReadyCallback, LocationListener {
    private static final int MY_LOCATION_REQUEST_CODE = 0;
    private static final long MIN_TIME = 400;
    private static final float MIN_DISTANCE = 1000;
    private GoogleMap mMap;
    private LocationManager locationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int token = PreferenceManager.getDefaultSharedPreferences(this).getInt("token", -1);
        if (token != -1) {
            Call<VerifyTokenBody> call = ApiManager.api.verifyToken(token);
            call.enqueue(new Callback<VerifyTokenBody>() {
                @Override
                public void onResponse(Call<VerifyTokenBody> call, Response<VerifyTokenBody> response) {
                    Log.d("TAG", response.toString());
                    if (response.code() == 400) {
                        startActivity(new Intent(DriverMapActivity.this, LoginActivity.class));
                        finish();
                    }
                }

                @Override
                public void onFailure(Call<VerifyTokenBody> call, Throwable t) {
                    Log.d("TAG", call.toString());
                    t.printStackTrace();
                }
            });
        }
        setContentView(R.layout.activity_driver_map);
        Places.initialize(getApplicationContext(), getResources().getString(R.string.google_maps_key));
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == MY_LOCATION_REQUEST_CODE) {
            if (permissions.length == 1 &&
                    Objects.equals(permissions[0], Manifest.permission.ACCESS_FINE_LOCATION) &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setLocationTrackingEnabled();
            } else {
                Toast.makeText(
                        this,
                        "Permission denied. App cannot work. Please approve permissions in settings",
                        Toast.LENGTH_LONG
                ).show();
            }
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 15);
        mMap.animateCamera(cameraUpdate);
        Call<Boolean> call = ApiManager.api.updatePosition(
                PreferenceManager.getDefaultSharedPreferences(this).getInt("token", -1),
                latLng.latitude,
                latLng.longitude
        );
        call.enqueue(new Callback<Boolean>() {
            @Override
            public void onResponse(Call<Boolean> call, Response<Boolean> response) {
                Log.d("TAG", response.toString());
            }

            @Override
            public void onFailure(Call<Boolean> call, Throwable t) {
                Log.d("TAG", call.toString());
                t.printStackTrace();
            }
        });
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onProviderDisabled(String provider) {
    }

    private void setLocationTrackingEnabled() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mMap.setMyLocationEnabled(true);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, MIN_TIME, MIN_DISTANCE, this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            setLocationTrackingEnabled();
        } else {
            // Show rationale and request permission.
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_LOCATION_REQUEST_CODE);
        }
    }
}
