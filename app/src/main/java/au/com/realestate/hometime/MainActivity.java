package au.com.realestate.hometime;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import au.com.realestate.hometime.models.ApiResponse;
import au.com.realestate.hometime.models.Token;
import au.com.realestate.hometime.models.Tram;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.moshi.MoshiConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

import java.io.IOException;
import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.ExecutionException;

public class MainActivity extends Activity {

    private List<Tram> southTrams;
    private List<Tram> northTrams;

    private int countNorth = 0;
    private int countSouth = 0;

    ListView listView;
//    Check_Internet check;

    Button btnRefresh, btnClear, btnNorth, btnSouth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        check = new Check_Internet();


        listView = (ListView) findViewById(R.id.listView);

        btnRefresh = (Button) findViewById(R.id.refreshButton);
        btnClear = (Button) findViewById(R.id.clearButton);
        btnNorth = (Button) findViewById(R.id.northButton);
        btnSouth = (Button) findViewById(R.id.southButton);

        if(!haveNetworkConnection())
        {
            Toast.makeText(getApplication(), "No internet ", Toast.LENGTH_SHORT).show();
        }

        btnRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(!haveNetworkConnection())
                {
                    finish();
                    System.exit(0);
                }

                if (countNorth == 0 && countSouth == 0)
                {
                    Toast.makeText(MainActivity.this, "Please select a direction first!", Toast.LENGTH_LONG).show();
                }
                else if (countNorth == 1 || countSouth == 1)
                {
                    showList();
                }
            }
        });

        btnClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                    northTrams = new ArrayList<>();
                    southTrams = new ArrayList<>();
                    showTrams();
                    btnNorth.setVisibility(View.VISIBLE);
                    btnSouth.setVisibility(View.VISIBLE);
                    countNorth = 0;
                    countSouth = 0;
                }
        });

        btnNorth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(!haveNetworkConnection())
                {
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("No Internet")
                            .setMessage("Please check your Internet Connection !!")
                            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    finish();
                                    System.exit(0);

                                }
                            })
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .show();
                }
                else {
                    countNorth = 1;
                    countSouth = 0;
                    showList();
                }
            }
        });

        btnSouth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!haveNetworkConnection())
                {
                    Toast.makeText(getApplication(), "No internet ", Toast.LENGTH_SHORT).show();
                }
                countSouth = 1;
                countNorth = 0;
                showList();
            }
        });

    }
    boolean haveNetworkConnection() {
        boolean haveConnectedWifi = false;
        boolean haveConnectedMobile = false;

        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo[] netInfo = cm.getAllNetworkInfo();
        for (NetworkInfo ni : netInfo) {
            if (ni.getTypeName().equalsIgnoreCase("WIFI") ||  ni.getTypeName().equalsIgnoreCase("MOBILE"))
            {
                if (ni.isConnected())
                    haveConnectedWifi = true;
            }

//            if (ni.getTypeName().equalsIgnoreCase("MOBILE"))
//                if (ni.isConnected())
//                    haveConnectedMobile = true;
        }
        return haveConnectedWifi;
    }

    private void showList()
    {
            TramsApi tramsApi = createApiClient();

            if (countNorth == 1)
            {
                countSouth = 0;
                btnSouth.setVisibility(View.GONE);
                btnNorth.setVisibility(View.VISIBLE);
                try {
                    String token = new RequestToken(tramsApi).execute("").get();
                    northTrams = new RequestTrams(tramsApi, token).execute("4055").get();
                    showTrams();
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }

            }
             else if (countSouth == 1)
            {
                countNorth = 0;
                btnNorth.setVisibility(View.GONE);
                btnSouth.setVisibility(View.VISIBLE);
                try {
                    String token = new RequestToken(tramsApi).execute("").get();
                    southTrams = new RequestTrams(tramsApi, token).execute("4155").get();
                    showTrams();
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }

            }
    }

    private void showTrams() {
        listView.setVisibility(View.VISIBLE);
        List<String> northValues = new ArrayList<>();
        List<String> southValues = new ArrayList<>();

        SimpleDateFormat gmtDateFormat = new SimpleDateFormat("dd-MMM-yyyy hh:mm:ss a");

        if (countNorth == 1)
        {

            for (Tram tram : northTrams) {
                String date= gmtDateFormat.format(dateFromDotNetDate(tram.predictedArrival)).toString();
                String destName = tram.destination.toString();
                String routeNo = tram.routeNo.toString();
                String waitingTime = timeDiffFromDotNetDate(tram.predictedArrival).toString();
                String listDetails = "Destination : "+destName +"\nRoute No : "+ routeNo +"\non "
                        +date+"\nWaiting time in hh:mm:ss is "+ waitingTime;
                northValues.add(listDetails);
            }

            listView.setAdapter(new ArrayAdapter<>(
                    this,
                    R.layout.list_layout,
                    northValues));
        }
        else if (countSouth == 1)
        {
            for (Tram tram : southTrams) {

                String date= gmtDateFormat.format(dateFromDotNetDate(tram.predictedArrival)).toString();
                String destName = tram.destination.toString();
                String routeNo = tram.routeNo.toString();
                String waitingTime = timeDiffFromDotNetDate(tram.predictedArrival).toString();
                String listDetails = "Destination : "+destName +"\nRoute No : "+ routeNo +"\non "
                        +date+"\nWaiting time (hh:mm:ss) "+ waitingTime ;
                southValues.add(listDetails);
            }
            listView.setAdapter(new ArrayAdapter<>(
                    this,
                    R.layout.list_layout,
                    southValues));
        }
    }



    /////////////
    // Convert .NET Date to Date
    ////////////

    private  String dateExtract(String dotNetDate)
    {
        int startIndex = dotNetDate.indexOf("(") + 1;
        int endIndex = dotNetDate.indexOf("+");
        String date = dotNetDate.substring(startIndex, endIndex);
        return date;
    }

    private Date dateFromDotNetDate(String dotNetDate ) {
        String date = dateExtract(dotNetDate);
        Long unixTime = Long.parseLong(date);
        return new Date(unixTime);
    }

    /////////////
    // Calculate Waiting time for Trams
    ////////////

    private String timeDiffFromDotNetDate(String dotNetDate) {

        int startIndex = dotNetDate.indexOf("(") + 1;
        int endIndex = dotNetDate.indexOf("+");
        String date = dotNetDate.substring(startIndex, endIndex);
        Long unixTime = Long.parseLong(date);
        Date tramArrivalDate = new Date(unixTime);

       // TimeZone.setDefault(TimeZone.getTimeZone("GMT+10"));
        //Calendar cal = Calendar.getInstance("GMT");
        //TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
        Calendar cal = Calendar.getInstance();
        Date currentDateTime = cal.getTime();
        int ch = currentDateTime.getHours()*60;
        int cm = currentDateTime.getMinutes();

        int ah = tramArrivalDate.getHours()*60;
        int am = tramArrivalDate.getMinutes();
        double min = 0;
        double hr = 0;

        int total1 = ah +am;
        int total2 = ch+cm;
        int total = total1-total2;
        if (total > 60)
        {
            min = total % 60;
             hr = total/60 ;
        }
        else
        {
             min = total;
             hr = 0;

        }
        Toast.makeText(this, "time = "+total+"hrs: "+hr+"min"+min, Toast.LENGTH_SHORT).show();

        String diff = String.valueOf(total);
//        long difference =0;
//        Toast.makeText(this, "Time1 = "+ah+"hrs:"+, Toast.LENGTH_SHORT).show();
        return (diff);
    }

    ////////////
    // API
    ////////////

    private interface TramsApi {

        @GET("/TramTracker/RestService/GetDeviceToken/?aid=TTIOSJSON&devInfo=HomeTimeAndroid")
        Call<ApiResponse<Token>> token();

        @GET("/TramTracker/RestService//GetNextPredictedRoutesCollection/{stopId}/78/false/?aid=TTIOSJSON&cid=2")
        Call<ApiResponse<Tram>> trams(
                @Path("stopId") String stopId,
                @Query("tkn") String token
        );
    }

    private TramsApi createApiClient() {

        String BASE_URL = "http://ws3.tramtracker.com.au";

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(MoshiConverterFactory.create())
                .build();

        return retrofit.create(TramsApi.class);
    }

    private class RequestToken extends AsyncTask<String, Integer, String> {

        TramsApi api;

        RequestToken(TramsApi api) {
            this.api = api;
        }

        @Override
        protected String doInBackground(String... params) {
            Call<ApiResponse<Token>> call = api.token();
            try {
                return call.execute().body().responseObject.get(0).value;
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    private class RequestTrams extends AsyncTask<String, Integer, List<Tram>> {

        private TramsApi api;
        private String token;

        RequestTrams(TramsApi api, String token) {
            this.api = api;
            this.token = token;
        }

        @Override
        protected List<Tram> doInBackground(String... stops) {

            Call<ApiResponse<Tram>> call = api.trams(stops[0], token);
            try {
                Response<ApiResponse<Tram>> resp = call.execute();
                return resp.body().responseObject;
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

}
