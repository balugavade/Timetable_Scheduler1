package com.example.timetablescheduler;

import android.app.Application;
import com.parse.Parse;

public class MyApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Parse.initialize(new Parse.Configuration.Builder(this)
                .applicationId("2wiI3JowNifa4JEgMJ6W2tdrj86wxxziFkaH2JBz") // From Back4App dashboard
                .clientKey("fjF0JOoKBvhBJkkbKdCcuttRJuDTHXyED8dP6Qm1")         // From Back4App dashboard
                .server("https://parseapi.back4app.com/")
                .build()
        );
    }
}

