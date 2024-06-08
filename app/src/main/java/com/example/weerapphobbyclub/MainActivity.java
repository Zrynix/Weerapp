package com.example.weerapphobbyclub;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.Color;
import android.os.Build;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.HashMap;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Query;

public class MainActivity extends AppCompatActivity {

    private EditText registerUsernameEditText;
    private EditText registerPasswordEditText;
    private Button registerButton;
    private EditText usernameEditText;
    private EditText passwordEditText;
    private Button loginButton;
    private EditText cityEditText;
    private Button getWeatherButton;
    private TextView weatherTextView;
    private TextView nameTextView;

    private static final String BASE_URL = "https://api.openweathermap.org/data/2.5/";
    private static final String API_KEY = "f55ab058c1a2e947271915a95258411f";
    private static final String TAG = "MainActivity";

    private HashMap<String, String> userDatabase = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set status bar color
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(Color.parseColor("#9D3A42"));
        }

        // Initialize UI components
        registerUsernameEditText = findViewById(R.id.registerUsername);
        registerPasswordEditText = findViewById(R.id.registerPassword);
        registerButton = findViewById(R.id.registerButton);
        usernameEditText = findViewById(R.id.username);
        passwordEditText = findViewById(R.id.password);
        loginButton = findViewById(R.id.loginButton);
        cityEditText = findViewById(R.id.cityEditText);
        getWeatherButton = findViewById(R.id.getWeatherButton);
        weatherTextView = findViewById(R.id.weatherTextView);
        nameTextView = findViewById(R.id.nameTextView);

        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = registerUsernameEditText.getText().toString();
                String password = registerPasswordEditText.getText().toString();

                if (username.isEmpty() || password.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Vul alle velden in", Toast.LENGTH_SHORT).show();
                } else if (userDatabase.containsKey(username)) {
                    Toast.makeText(MainActivity.this, "Gebruikersnaam is al geregistreerd", Toast.LENGTH_SHORT).show();
                } else {
                    userDatabase.put(username, password);
                    Toast.makeText(MainActivity.this, "Registratie succesvol", Toast.LENGTH_SHORT).show();
                    registerUsernameEditText.setVisibility(View.GONE);
                    registerPasswordEditText.setVisibility(View.GONE);
                    registerButton.setVisibility(View.GONE);

                    usernameEditText.setVisibility(View.VISIBLE);
                    passwordEditText.setVisibility(View.VISIBLE);
                    loginButton.setVisibility(View.VISIBLE);
                }
            }
        });

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = usernameEditText.getText().toString();
                String password = passwordEditText.getText().toString();

                if (username.isEmpty() || password.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Vul alle velden in", Toast.LENGTH_SHORT).show();
                } else if (!userDatabase.containsKey(username) || !userDatabase.get(username).equals(password)) {
                    Toast.makeText(MainActivity.this, "Onjuiste gebruikersnaam of wachtwoord", Toast.LENGTH_SHORT).show();
                } else {
                    usernameEditText.setVisibility(View.GONE);
                    passwordEditText.setVisibility(View.GONE);
                    loginButton.setVisibility(View.GONE);

                    cityEditText.setVisibility(View.VISIBLE);
                    getWeatherButton.setVisibility(View.VISIBLE);
                    weatherTextView.setVisibility(View.VISIBLE);
                }
            }
        });

        getWeatherButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String city = cityEditText.getText().toString();
                if (!city.isEmpty()) {
                    getWeatherData(city);
                } else {
                    Toast.makeText(MainActivity.this, "Voer een stad in", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void getWeatherData(String city) {
        Gson gson = new GsonBuilder().create();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();

        WeatherService service = retrofit.create(WeatherService.class);
        Call<WeatherResponse> call = service.getWeather(city, API_KEY);

        call.enqueue(new Callback<WeatherResponse>() {
            @Override
            public void onResponse(Call<WeatherResponse> call, Response<WeatherResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    WeatherResponse weatherResponse = response.body();
                    String weatherInfo = "Temperatuur: " + (weatherResponse.main.temp - 273.15) + "°C\n" +
                            "Luchtvochtigheid: " + weatherResponse.main.humidity + "%\n" +
                            "Wind Directie: " + convertDegreeToDirection(weatherResponse.wind.deg) + "\n" +
                            "Wind Snelheid: " + weatherResponse.wind.speed + " m/s\n" +
                            "Regen: " + (weatherResponse.weather[0].main.equals("Rain") ? "Ja" : "Nee");
                    weatherTextView.setText(weatherInfo);
                } else {
                    Toast.makeText(MainActivity.this, "Fout bij het ophalen van de gegevens", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Response code: " + response.code());
                    Log.e(TAG, "Response message: " + response.message());
                    try {
                        Log.e(TAG, "Response error body: " + response.errorBody().string());
                    } catch (Exception e) {
                        Log.e(TAG, "Fout bij het lezen van de foutboodschap", e);
                    }
                }
            }

            @Override
            public void onFailure(Call<WeatherResponse> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Netwerkfout: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Netwerkfout: ", t);
            }
        });
    }

    private String convertDegreeToDirection(float degree) {
        String[] directions = {"N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE",
                "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW"};
        int index = Math.round(degree / 22.5f) % 16;
        return directions[index];
    }

    interface WeatherService {
        @GET("weather")
        Call<WeatherResponse> getWeather(@Query("q") String city, @Query("appid") String apiKey);
    }

    public class WeatherResponse {
        Main main;
        Weather[] weather;
        Wind wind;

        public class Main {
            float temp;
            float humidity;
        }

        public class Weather {
            String main;
        }

        public class Wind {
            float speed;
            float deg;
        }
    }
}
