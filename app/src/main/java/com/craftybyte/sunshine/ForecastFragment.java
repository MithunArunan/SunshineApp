package com.craftybyte.sunshine;

import android.app.Fragment;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

/**
 * Created by Mithun on 27-Jan-15.
 */
public class ForecastFragment extends Fragment {
    ArrayAdapter listData;
    static final String DATA = "data";
    public ForecastFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.forecastmenu,menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch(item.getItemId())
        {
            case R.id.menuRefresh:
                updateWeather();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onStart() {
        super.onStart();
        updateWeather();
    }

    public void updateWeather(){
        Toast.makeText(getActivity(),"Refreshed",Toast.LENGTH_LONG ).show();
        FetchWeatherTask fwt = new FetchWeatherTask();
        String locationCode = PreferenceManager.getDefaultSharedPreferences(getActivity()).getString(getString(R.string.location_key),getString(R.string.default_loc));
        fwt.execute(locationCode);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_activity1, container, false);
        ListView lv = (ListView) rootView.findViewById(R.id.listview);
        String[] fakeData = new String[]{"Mon, Jan 1 30/11", "Mon, Jan 1 25/11", "Tue, Jan 2 22/15", "Wed, Jan 3 39/09", "Thurs, Jan 4 35/20","Fri, Jan 5 30/5","Sat, Jan 6 33/16","Sun, Jan 7 30/11"};
        ArrayList<String> forecastData = new ArrayList<String>(Arrays.asList(fakeData));
        listData = new ArrayAdapter(getActivity(), R.layout.list_item_view,R.id.list_textview,forecastData);
        lv.setAdapter(listData);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
           //     Toast.makeText(getActivity(),listData.getItem(position).toString(),Toast.LENGTH_LONG).show();
                Intent intent = new Intent(getActivity(),DetailActivity.class);
                intent.putExtra(DATA,listData.getItem(position).toString());
                startActivity(intent);
            }
        });
        return rootView;
    }
    public class FetchWeatherTask extends AsyncTask<String,Void,String[]>
    {

        protected String[] doInBackground(String... params) {

            // These two need to be declared outside the try/catch
// so that they can be closed in the finally block.
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            String format = "json";
            String units = "metric";
            int numDays = 7;

// Will contain the raw JSON response as a string.
            String forecastJsonStr = null;
            String BASE = "http://api.openweathermap.org/data/2.5/forecast/daily?";
            final String QUERY_PARAM = "q";
            final String FORMAT_PARAM = "mode";
            final String UNITS_PARAM = "units";
            final String DAYS_PARAM = "cnt";
            Uri builtUri = Uri.parse(BASE).buildUpon().appendQueryParameter(QUERY_PARAM,params[0])
                    .appendQueryParameter(FORMAT_PARAM,format).appendQueryParameter(UNITS_PARAM,units)
                    .appendQueryParameter(DAYS_PARAM,Integer.toString(numDays)).build();
            try {
                // Construct the URL for the OpenWeatherMap query
                // Possible parameters are avaiable at OWM's forecast API page, at
                // http://openweathermap.org/API#forecast
                URL url = new URL(builtUri.toString());

                // Create the request to OpenWeatherMap, and open the connection
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    // Nothing to do.
                    forecastJsonStr = null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    forecastJsonStr = null;
                }
                forecastJsonStr = buffer.toString();
                Log.v("Data","Here it is "+ forecastJsonStr);

            }
            catch (IOException e) {
                Log.e("PlaceholderFragment", "Error ", e);
                forecastJsonStr = null;
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            finally{
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e("PlaceholderFragment", "Error closing stream", e);
                    }
                }
            }

            try {
                return getWeatherDataFromJson(forecastJsonStr, numDays);
            } catch (JSONException e1) {
                e1.printStackTrace();
            }

            return null;
        }

        private String[] getWeatherDataFromJson(String forecastJsonStr, int numDays) throws JSONException {
            final String PARSE_LIST = "list";
            final String PARSE_DT="dt";final String PARSE_weather="weather";
            final String PARSE_DES="description";
            JSONObject forecastJson = new JSONObject(forecastJsonStr);
            JSONArray listArray = forecastJson.getJSONArray(PARSE_LIST);
            String resultStr[] = new String[numDays];
            for(int i=0;i<listArray.length();i++)
            {
                JSONObject dayObject = listArray.getJSONObject(i);
                Long l = dayObject.getLong(PARSE_DT);
                JSONObject weatherObj = dayObject.getJSONArray(PARSE_weather).getJSONObject(0);
                String description = weatherObj.getString(PARSE_DES);
                JSONObject tempObj = dayObject.getJSONObject("temp");
                Double min = tempObj.getDouble("min");
                Double maxi = tempObj.getDouble("max");
                String date = getDateformat(l);
                String minMax = formatMinMaxTemp(min,maxi);
                resultStr[i]= date + "  " + description +"   "+ minMax;
            }
        return  resultStr;
        }

        private String formatMinMaxTemp(Double min, Double maxi) {
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
            String unitType = sharedPref.getString(getString(R.string.units_key),getString(R.string.units_metric));
            if(unitType.equals(getString(R.string.units_imperial)))
            {
                min = (1.8*min) +32;
                maxi = (1.8 * maxi) + 32;
            }
            long mi = Math.round(min);
            long ma = Math.round(maxi);
            Log.v("MinMax", mi +"/"+ma);
            String val = mi +"/" +ma;
            return val;
        }

        private String getDateformat(Long l) {
            Date date = new Date(l * 1000);
            SimpleDateFormat format = new SimpleDateFormat("E, MMM d");
            return format.format(date).toString();
        }

        @Override
        protected void onPostExecute(String[] str) {
            if(str != null) {
                listData.clear();
                for (String s : str)
                {
                    listData.add(s);
                }
            }
        }
    }
}