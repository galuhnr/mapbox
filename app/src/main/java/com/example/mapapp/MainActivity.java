package com.example.mapapp;

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.mapapp.retrofit.ApiEndpoint;
import com.example.mapapp.retrofit.ApiService;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.style.expressions.Expression;
import com.mapbox.mapboxsdk.style.layers.FillLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import timber.log.Timber;

import static com.mapbox.mapboxsdk.style.expressions.Expression.get;
import static com.mapbox.mapboxsdk.style.expressions.Expression.match;
import static com.mapbox.mapboxsdk.style.expressions.Expression.rgba;
import static com.mapbox.mapboxsdk.style.expressions.Expression.stop;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.fillColor;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String STATE_JSON_FILE = "cluster.json";
    private static final String DATA_MATCH_PROP = "kecamatan";
    private static final String DATA_STYLE_UNEMPLOYMENT_PROP = "cluster";

    private final String TAG = "MainActivity";
    private List<ModelAPI> modelAPIList = new ArrayList<>();

    private MapView mapView;
    private MapboxMap mapboxMap;
    private JSONArray statesArray;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Mapbox.getInstance(this, getString(R.string.mapbox_access_token));
        setContentView(R.layout.activity_main);
        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);
    }

    @Override
    public void onMapReady(@NonNull final MapboxMap map) {
        this.mapboxMap = map;
        map.setStyle(Style.OUTDOORS, new Style.OnStyleLoaded() {
            @Override
            public void onStyleLoaded(@NonNull Style style) {
                try {
                    GeoJsonSource sbyRouteGeoJson = new GeoJsonSource(
                            "sbydata", new URI("asset://sby.geojson"));
                    style.addSource(sbyRouteGeoJson);
                    new LoadJson(MainActivity.this).execute();
                } catch (URISyntaxException exception) {
                    Timber.d(exception);
                }
            }
        });
    }

    private void addDataToMap(@NonNull Expression.Stop[] stops) {
        FillLayer statesJoinLayer = new FillLayer("states-join", "sbydata");
        statesJoinLayer.setSourceLayer("sbydata");
        statesJoinLayer.withProperties(
                fillColor(match(Expression.toString(get(DATA_MATCH_PROP)),
                        rgba(0, 0, 0, 1), stops))
        );
        // Add layer to map below the "waterway-label" layer
        if (mapboxMap != null) {
            mapboxMap.getStyle(style -> style.addLayerAbove(statesJoinLayer, "waterway-label"));
        }
    }

    private class LoadJson extends AsyncTask<Void, Void, Expression.Stop[]>{

        private WeakReference<MainActivity> weakReference;

        LoadJson(MainActivity activity) {
            this.weakReference = new WeakReference<>(activity);
        }

        @Override
        protected Expression.Stop[] doInBackground(Void... voids){
            try {
                MainActivity activity = weakReference.get();
                if (activity != null) {

                    // get data dari rest api http://10.0.0.2:8000/api/cluster2016
                    ApiEndpoint apiEndpoint = ApiService.getRetrofit().create(ApiEndpoint.class);
                    Call<List<ModelAPI>> call = apiEndpoint.getCluster2016();
                    call.enqueue(new Callback<List<ModelAPI>>() {
                        @Override
                        public void onResponse(Call<List<ModelAPI>> call, Response<List<ModelAPI>> response) {
                            modelAPIList = response.body();
//                            String json = new Gson().toJson(modelAPIList);
//                            try {
//                                statesArray = new JSONArray(json);
//                            } catch (JSONException e) {
//                                e.printStackTrace();
//                            }
                        }
                        @Override
                        public void onFailure(Call<List<ModelAPI>> call, Throwable t) {
                            Log.d( TAG, t.toString());
                        }
                    });

                    //coding asli dari tutorial mapbox baca data json dari local di folder assets,
                    // berhasil kalo baca json dari file local

                    InputStream inputStream = activity.getAssets().open(STATE_JSON_FILE);
                    activity.statesArray = new JSONArray(convertStreamToString(inputStream));

                    Expression.Stop[] stops = new Expression.Stop[activity.statesArray.length()];
                    for (int x = 0; x < activity.statesArray.length(); x++) {
                        try {
                            // pewarnaan tiap cluster
                            JSONObject singleState = activity.statesArray.getJSONObject(x);
                            if(singleState.getDouble(DATA_STYLE_UNEMPLOYMENT_PROP)==1){
                                stops[x] = stop(
                                        singleState.getString(DATA_MATCH_PROP),
                                        rgba(0, 255, 0, 1)
                                );
                            }else if(singleState.getDouble(DATA_STYLE_UNEMPLOYMENT_PROP)==2){
                                stops[x] = stop(
                                        singleState.getString(DATA_MATCH_PROP),
                                        rgba(255, 255, 0, 1)
                                );
                            }else if(singleState.getDouble(DATA_STYLE_UNEMPLOYMENT_PROP)==3){
                                stops[x] = stop(
                                        singleState.getString(DATA_MATCH_PROP),
                                        rgba(255, 0, 0, 1)
                                );
                            }
                        } catch (JSONException exception) {
                            throw new RuntimeException(exception);
                        }
                    }
                    return stops;
                }
            } catch (Exception exception) {
                Timber.d("Exception Loading GeoJSON: %s", exception.toString());
            }
            return null;
        }

        static String convertStreamToString(InputStream is) {
            Scanner scanner = new Scanner(is).useDelimiter("\\A");
            return scanner.hasNext() ? scanner.next() : "";
        }

        @Override
        protected void onPostExecute(@Nullable Expression.Stop[] stopsArray) {
            super.onPostExecute(stopsArray);
            MainActivity activity = weakReference.get();
            if (activity != null && stopsArray != null) {
                activity.addDataToMap(stopsArray);
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }
}