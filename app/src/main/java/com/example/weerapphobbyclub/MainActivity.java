package com.example.weerapphobbyclub;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Query;

import java.util.HashMap;
import java.util.Map;

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
    private static final String TAG = "MainActivity";
    private static final String PREFS_NAME = "UserPrefs";
    private static final String USERS_KEY = "users";

    private HashMap<String, String> userDatabase = new HashMap<>();
    private boolean isLoggedIn = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO); // Enforce light theme

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Ensure the background is white
        getWindow().getDecorView().setBackgroundColor(Color.WHITE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(Color.parseColor("#9D3A42"));
        }

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

        loadUserData();

        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleRegister();
            }
        });

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleLogin();
            }
        });

        getWeatherButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleGetWeather();
            }
        });

        if (savedInstanceState != null) {
            restoreInstanceState(savedInstanceState);
        }
    }

    private void restoreInstanceState(Bundle savedInstanceState) {
    }

    private void handleRegister() {
        String username = registerUsernameEditText.getText().toString().trim();
        String password = registerPasswordEditText.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(MainActivity.this, "Vul alle velden in", Toast.LENGTH_SHORT).show();
        } else if (userDatabase.containsKey(username)) {
            Toast.makeText(MainActivity.this, "Gebruikersnaam is al geregistreerd", Toast.LENGTH_SHORT).show();
        } else {
            userDatabase.put(username, password);
            saveUserData();
            Toast.makeText(MainActivity.this, "Registratie succesvol", Toast.LENGTH_SHORT).show();
            showLoginFields();
        }
    }

    private void handleLogin() {
        String username = usernameEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(MainActivity.this, "Vul alle velden in", Toast.LENGTH_SHORT).show();
        } else if (!userDatabase.containsKey(username) || !userDatabase.get(username).equals(password)) {
            Toast.makeText(MainActivity.this, "Onjuiste gebruikersnaam of wachtwoord", Toast.LENGTH_SHORT).show();
        } else {
            isLoggedIn = true;
            showWeatherFields();
        }
    }

    private void handleGetWeather() {
        String city = cityEditText.getText().toString().trim();
        if (!city.isEmpty()) {
            getWeatherData(city);
        } else {
            Toast.makeText(MainActivity.this, "Voer een stad in", Toast.LENGTH_SHORT).show();
        }
    }

    private void getWeatherData(String city) {
        Gson gson = new GsonBuilder().create();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();

        WeatherService service = retrofit.create(WeatherService.class);
        String apiKey = getString(R.string.weather_api_key);  // Retrieve the API key from resources
        Call<WeatherResponse> call = service.getWeather(city, apiKey);

        call.enqueue(new Callback<WeatherResponse>() {
            @Override
            public void onResponse(Call<WeatherResponse> call, Response<WeatherResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    displayWeatherData(response.body());
                } else {
                    showError(response);
                }
            }

            @Override
            public void onFailure(Call<WeatherResponse> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Netwerkfout: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Netwerkfout: ", t);
            }
        });
    }

    private void displayWeatherData(WeatherResponse weatherResponse) {
        double temperatureCelsius = weatherResponse.main.temp - 273.15;
        String temperatureFormatted = String.format("%.2f", temperatureCelsius);

        String weatherInfo = "Temperatuur: " + temperatureFormatted + "°C\n" +
                "Luchtvochtigheid: " + weatherResponse.main.humidity + "%\n" +
                "Wind Directie: " + convertDegreeToDirection(weatherResponse.wind.deg) + "\n" +
                "Wind Snelheid: " + weatherResponse.wind.speed + " m/s\n" +
                "Regen: " + (weatherResponse.weather[0].main.equals("Rain") ? "Ja" : "Nee");
        weatherTextView.setText(weatherInfo);
        weatherTextView.setVisibility(View.VISIBLE);
    }

    private void showError(Response<WeatherResponse> response) {
        Toast.makeText(MainActivity.this, "Fout bij het ophalen van de gegevens", Toast.LENGTH_SHORT).show();
        Log.e(TAG, "Response code: " + response.code());
        Log.e(TAG, "Response message: " + response.message());
        try {
            Log.e(TAG, "Response error body: " + response.errorBody().string());
        } catch (Exception e) {
            Log.e(TAG, "Fout bij het lezen van de foutboodschap", e);
        }
    }

    private String convertDegreeToDirection(float degree) {
        String[] directions = {"N", "NNO", "NO", "ONO", "O", "OZO", "ZO", "ZZO",
                "Z", "ZZW", "ZW", "WZW", "W", "WNW", "NW", "NNW"};
        int index = Math.round(degree / 22.5f) % 16;
        return directions[index];
    }

    private void showLoginFields() {
        registerUsernameEditText.setVisibility(View.GONE);
        registerPasswordEditText.setVisibility(View.GONE);
        registerButton.setVisibility(View.GONE);

        usernameEditText.setVisibility(View.VISIBLE);
        passwordEditText.setVisibility(View.VISIBLE);
        loginButton.setVisibility(View.VISIBLE);
    }

    private void showWeatherFields() {
        usernameEditText.setVisibility(View.GONE);
        passwordEditText.setVisibility(View.GONE);
        loginButton.setVisibility(View.GONE);

        cityEditText.setVisibility(View.VISIBLE);
        getWeatherButton.setVisibility(View.VISIBLE);
        weatherTextView.setVisibility(View.VISIBLE);
    }

    private void saveUserData() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        Gson gson = new Gson();
        String json = gson.toJson(userDatabase);
        editor.putString(USERS_KEY, json);
        editor.apply();
    }

    private void loadUserData() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Gson gson = new Gson();
        String json = prefs.getString(USERS_KEY, "");
        if (!json.isEmpty()) {
            userDatabase = gson.fromJson(json, HashMap.class);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("registerUsernameVisibility", registerUsernameEditText.getVisibility());
        outState.putInt("registerPasswordVisibility", registerPasswordEditText.getVisibility());
        outState.putInt("registerButtonVisibility", registerButton.getVisibility());
        outState.putInt("usernameVisibility", usernameEditText.getVisibility());
        outState.putInt("passwordVisibility", passwordEditText.getVisibility());
        outState.putInt("loginButtonVisibility", loginButton.getVisibility());
        outState.putInt("cityEditTextVisibility", cityEditText.getVisibility());
        outState.putInt("getWeatherButtonVisibility", getWeatherButton.getVisibility());
        outState.putInt("weatherTextViewVisibility", weatherTextView.getVisibility());
        outState.putBoolean("isLoggedIn", isLoggedIn);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        restoreVisibilityState(savedInstanceState);
        isLoggedIn = savedInstanceState.getBoolean("isLoggedIn");
    }

    private void restoreVisibilityState(Bundle savedInstanceState) {
        registerUsernameEditText.setVisibility(savedInstanceState.getInt("registerUsernameVisibility"));
        registerPasswordEditText.setVisibility(savedInstanceState.getInt("registerPasswordVisibility"));
        registerButton.setVisibility(savedInstanceState.getInt("registerButtonVisibility"));
        usernameEditText.setVisibility(savedInstanceState.getInt("usernameVisibility"));
        passwordEditText.setVisibility(savedInstanceState.getInt("passwordVisibility"));
        loginButton.setVisibility(savedInstanceState.getInt("loginButtonVisibility"));
        cityEditText.setVisibility(savedInstanceState.getInt("cityEditTextVisibility"));
        getWeatherButton.setVisibility(savedInstanceState.getInt("getWeatherButtonVisibility"));
        weatherTextView.setVisibility(savedInstanceState.getInt("weatherTextViewVisibility"));
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
