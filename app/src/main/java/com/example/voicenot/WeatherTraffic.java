package com.example.voicenot;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import org.json.JSONException;

import java.util.Locale;

public class WeatherTraffic extends AppCompatActivity {

    private Switch weatherSwitch, trafficSwitch;
    private TextView weatherTextView, trafficTextView;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private LocationRequest locationRequest;
    private static final int LOCATION_PERMISSION_REQUEST = 1;

    // Replace with your valid OpenWeatherMap API key
    private final String weatherApiKey = "caa54860384cba80ae7bf712048b5312"; // Replace with actual key

    // Text-to-Speech
    private TextToSpeech textToSpeech;

    // Handler for periodic updates
    private Handler handler = new Handler();
    private Runnable periodicUpdateRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather_traffic);

        // Initialize UI elements
        weatherSwitch = findViewById(R.id.weatherSwitch);
        trafficSwitch = findViewById(R.id.trafficSwitch);
        weatherTextView = findViewById(R.id.weatherTextView);
        trafficTextView = findViewById(R.id.trafficTextView);

        // Initialize FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Create Location Request for continuous updates
        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(5000); // 5 seconds interval

        // Initialize Text-to-Speech
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = textToSpeech.setLanguage(Locale.US);
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("WeatherTraffic", "Language not supported");
                }
            } else {
                Log.e("WeatherTraffic", "Text-to-Speech initialization failed");
            }
        });

        // Location callback
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    Log.e("WeatherTraffic", "Location result is null");
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    Log.d("WeatherTraffic", "Lat: " + location.getLatitude() + ", Lon: " + location.getLongitude());
                    fetchWeatherData(location.getLatitude(), location.getLongitude());
                }
            }
        };

        // Weather updates toggle
        weatherSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                getWeatherUpdates();
                startPeriodicUpdates();
            } else {
                weatherTextView.setText("Weather updates disabled");
                stopPeriodicUpdates();
            }
        });

        // Traffic updates toggle
        trafficSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                getTrafficUpdates();
            } else {
                trafficTextView.setText("Traffic updates disabled");
            }
        });
    }

    // Fetch weather updates
    private void getWeatherUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST);
            return;
        }

        // Request location updates
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
    }

    // Fetch weather data from OpenWeatherMap
    private void fetchWeatherData(double lat, double lon) {
        if (!isNetworkAvailable()) {
            weatherTextView.setText("No internet connection");
            return;
        }

        String url = "https://api.openweathermap.org/data/2.5/weather?lat=" + lat + "&lon=" + lon + "&appid=" + weatherApiKey + "&units=metric";

        RequestQueue queue = Volley.newRequestQueue(this);
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        Log.d("WeatherTraffic", "Response: " + response.toString());
                        String weatherDescription = response.getJSONArray("weather").getJSONObject(0).getString("description");
                        double temp = response.getJSONObject("main").getDouble("temp");
                        String weatherText = "Weather: " + weatherDescription + ", Temperature: " + temp + "°C";
                        weatherTextView.setText(weatherText);

                        // Speak the weather update
                        speakWeatherUpdate(weatherText);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        weatherTextView.setText("Error parsing weather data");
                        Log.e("WeatherTraffic", "JSON parsing error: " + e.getMessage());
                    }
                },
                error -> {
                    weatherTextView.setText("Failed to fetch weather updates");
                    Log.e("WeatherTraffic", "Error fetching weather data: " + error.toString());
                });

        queue.add(request);
    }

    // Speak the weather update
    private int speakCount = 0;

    private void speakWeatherUpdate(String weatherText) {
        if (textToSpeech != null && speakCount < 1) { // 🔹 Speak only twice
            speakCount++;

            new Handler().postDelayed(() -> {
                if (textToSpeech != null) {
                    textToSpeech.speak(weatherText, TextToSpeech.QUEUE_ADD, null, null);
                }
            }, 500);

            // 🔹 Reset speakCount after 5 minutes (so it can speak again)
            new Handler().postDelayed(() -> {
                speakCount = 0;
            }, 120000); // 5 minutes
        }
    }


    // Start periodic updates every hour
    private void startPeriodicUpdates() {
        periodicUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                if (ActivityCompat.checkSelfPermission(WeatherTraffic.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                        if (location != null) {
                            fetchWeatherData(location.getLatitude(), location.getLongitude()); // 🔹 Directly fetch weather data
                        }
                    });
                }
                handler.postDelayed(this, 120000); // 2 minutes interval
            }
        };
        handler.post(periodicUpdateRunnable);
    }


    // Stop periodic updates
    private void stopPeriodicUpdates() {
        if (periodicUpdateRunnable != null) {
            handler.removeCallbacks(periodicUpdateRunnable);
        }
    }

    // Fetch traffic updates using Google Maps deep link
    private void getTrafficUpdates() {
        
        String uri = "https://www.waze.com/ul?ll=LAT,LNG&navigate=yes";
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
        startActivity(intent);
       /* // Replace "Your+Destination" with the actual destination
        String destination = "Sargodha"; // Example: "New+York+City"
       String uri = "https://www.google.com/maps/dir/?api=1&destination=" + destination + "&travelmode=driving";
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
        startActivity(intent); */

        // Update the traffic TextView
        trafficTextView.setText("Opening Maps for traffic updates...");
    }

    // Handle permission request result
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getWeatherUpdates();
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Check if network is available
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Shutdown Text-to-Speech
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
    }
}