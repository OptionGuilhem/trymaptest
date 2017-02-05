package com.option.guilhem.trymap;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;


import com.mapbox.geocoder.GeocoderCriteria;
import com.mapbox.geocoder.MapboxGeocoder;
import com.mapbox.geocoder.service.models.GeocoderFeature;
import com.mapbox.geocoder.service.models.GeocoderResponse;
import com.mapbox.mapboxsdk.MapboxAccountManager;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.location.LocationListener;
import com.mapbox.mapboxsdk.location.LocationServices;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.widgets.MyLocationViewSettings;

import com.mapbox.services.android.geocoder.ui.GeocoderAutoCompleteView;
import com.mapbox.services.commons.models.Position;
import com.mapbox.services.geocoding.v5.GeocodingCriteria;
import com.mapbox.services.geocoding.v5.models.CarmenFeature;


import java.util.List;

import retrofit.Callback;
import retrofit.Response;
import retrofit.Retrofit;


public class MainActivity extends AppCompatActivity {

    private MapView mapView;
    private MapboxMap map;
    private FloatingActionButton floatingActionButton;
    private LocationServices locationServices;
    private LocationListener locationListener;
    private GeocoderAutoCompleteView autoCompleteView;

    private static final int PERMISSIONS_LOCATION = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        MapboxAccountManager.start(this, getString(R.string.access_token));
        setContentView(R.layout.activity_main);
        locationServices = LocationServices.getLocationServices(MainActivity.this);

        mapView = (MapView) findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);

        //ajouter une icone de pin
        ImageView userLoc = new ImageView(this);
        userLoc.setImageResource(R.drawable.ic_loc_user);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
        userLoc.setLayoutParams(params);
        mapView.addView(userLoc);


        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(MapboxMap mapboxMap) {
                map = mapboxMap;

                //cacher le cercle accuracy bleu ?
                MyLocationViewSettings myLocationViewSettings = map.getMyLocationViewSettings();
                myLocationViewSettings.setAccuracyAlpha(100);

                //centrer la vue au demarrage
                toggleGps(!map.isMyLocationEnabled());


                mapboxMap.setOnMapClickListener(new MapboxMap.OnMapClickListener() {
                    @Override
                    public void onMapClick(@NonNull LatLng point) {


                        geocode(point);
                    }
                });
            }
        });

        autoCompleteView = (GeocoderAutoCompleteView) findViewById(R.id.query);
        autoCompleteView.setAccessToken(MapboxAccountManager.getInstance().getAccessToken());
        autoCompleteView.setType(GeocodingCriteria.TYPE_ADDRESS);
        autoCompleteView.setOnFeatureListener(new GeocoderAutoCompleteView.OnFeatureListener() {
            @Override
            public void OnFeatureClick(CarmenFeature feature) {
                Position position = feature.asPosition();
                updateMap(position.getLatitude(), position.getLongitude());
            }
        });

        floatingActionButton = (FloatingActionButton) findViewById(R.id.location_toggle_fab);
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (map != null) {
                    toggleGps(!map.isMyLocationEnabled());
                }
            }
        });
    }

    //honteusement copié sur un exemple
    private void geocode(final LatLng point) {
        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... params) {
                MapboxGeocoder client = new MapboxGeocoder.Builder()
                        .setAccessToken(MapboxAccountManager.getInstance().getAccessToken())
                        .setCoordinates(point.getLongitude(), point.getLatitude())
                        .setType(GeocoderCriteria.TYPE_ADDRESS)
                        .build();

                client.enqueue(new Callback<GeocoderResponse>() {
                                   @Override
                                   public void onResponse(Response<GeocoderResponse> response, Retrofit retrofit) {
                                       List<GeocoderFeature> results = response.body().getFeatures();
                                       if (results.size() > 0) {
                                           String placeName = results.get(0).getPlaceName();
                                           setSuccess(placeName);
                                       } else {
                                           setMessage("No results.");
                                       }
                                   }

                                   @Override
                                   public void onFailure(Throwable t) {
                                       setError(t.getMessage());
                                   }
                               }
                );
                return null;
            }
        }.execute();
    }

    private void setMessage(String message) {
        Log.d("DEBUG INFO", "Message: " + message);
    }

    private void setSuccess(String placeName) {
        Log.d("DEBUG INFO", "Place name: " + placeName);
        autoCompleteView.setText(placeName);
    }

    private void setError(String message) {
        Log.e("DEBUG INFO", "Error: " + message);
    }

    private void updateMap(double latitude, double longitude) {
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(new LatLng(latitude, longitude))
                .zoom(15)
                .build();
        map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), 5000, null);
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
        // Ensure no memory leak occurs if we register the location listener but the call hasn't
        // been made yet.
        if (locationListener != null) {
            locationServices.removeLocationListener(locationListener);
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    private void toggleGps(boolean enableGps) {
        if (enableGps) {
            // Check if user has granted location permission
            if (!locationServices.areLocationPermissionsGranted()) {
                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_LOCATION);
            } else {
                enableLocation(true);
            }
        } else {
            enableLocation(false);
        }
    }

    private void enableLocation(boolean enabled) {
        if (enabled) {
            // If we have the last location of the user, we can move the camera to that position.
            Location lastLocation = locationServices.getLastLocation();
            if (lastLocation != null) {
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lastLocation), 16));
            }

            locationListener = new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    if (location != null) {
                        // Move the map camera to where the user location is and then remove the
                        // listener so the camera isn't constantly updating when the user location
                        // changes. When the user disables and then enables the location again, this
                        // listener is registered again and will adjust the camera once again.
                        map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location), 16));
                        locationServices.removeLocationListener(this);
                    }
                }
            };
            locationServices.addLocationListener(locationListener);
       }
        // Enable or disable the location layer on the map
        map.setMyLocationEnabled(enabled);
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSIONS_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableLocation(true);
            }
        }
    }
}