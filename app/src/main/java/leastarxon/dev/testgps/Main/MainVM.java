package leastarxon.dev.testgps.Main;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.location.LocationRequest;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import leastarxon.dev.testgps.BR;
import leastarxon.dev.testgps.R;
import leastarxon.dev.testgps.Utils.PermissionHelper;
import pl.charmas.android.reactivelocation2.ReactiveLocationProvider;


public class MainVM extends BaseObservable {
    private static int GOOGLE_SERVICE_VERSION = 11800000;
    private AlertDialog alertGPS;
    private AppCompatActivity context;
    private LocationRequest request;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private String process;
    private StringBuilder processBuilder = new StringBuilder();
    private ReactiveLocationProvider locationProvider;
    private boolean alertGpsIsShowing = false;
    private static boolean geolocationEnabled = false;
    private boolean isStart = false;
    private String currentLocation;
    private String error;
    private CompositeDisposable subscriptions;

    void init() {
        subscriptions = new CompositeDisposable();
        startCheckGps();
    }

    public View.OnClickListener restart = v -> {
        processBuilder.append("Click restart\n");
        setProcess(processBuilder.toString());
        onDestroy();
        subscriptions = new CompositeDisposable();
        setError(null);
        setCurrentLocation("");
        setProcess("");
        processBuilder = new StringBuilder();
        onResume();
    };

    private void startCheckGps() {
        processBuilder.append("start check gps\n");
        setProcess(processBuilder.toString());
        if (PermissionHelper.checkPermissionsForGPS(context)) {
            processBuilder.append("permissions success\n");
            setProcess(processBuilder.toString());
            if (checkLocationServiceEnabled()) {
                processBuilder.append("gps enabled\n");
                setProcess(processBuilder.toString());
                //startLocation
                checkVersionGS();
                setError(null);
            } else {
                //no gps
                setError("No gps");
                processBuilder.append("gps error\n");
                setProcess(processBuilder.toString());
            }
        } else {
            //no permission
            setError("No permissions");
            processBuilder.append("permissions error\n");
            setProcess(processBuilder.toString());
        }
    }

    private void checkVersionGS() {
        processBuilder.append("check versions gs\n");
        setProcess(processBuilder.toString());
        try {
            //todo no const!
            PackageManager packageManager = context.getPackageManager();
            PackageInfo packageInfo = packageManager.getPackageInfo(GoogleApiAvailability.GOOGLE_PLAY_SERVICES_PACKAGE, 0);
            int v = packageInfo.versionCode;
            processBuilder.append("gs version =" + String.valueOf(v) + "\n");
            setProcess(processBuilder.toString());
            if (v < GOOGLE_SERVICE_VERSION) {
                takeCoordsOldApi();
            } else {
                takeCoordsNewApi();
            }
        } catch (PackageManager.NameNotFoundException e) {
            processBuilder.append("error check Versions gs\n");
            setProcess(processBuilder.toString());
            setError("PackageManager.NameNotFoundException " + e.getMessage());
        }


    }


    private void takeCoordsOldApi() {
        setStart(true);
        processBuilder.append("start old api gps\n");
        setProcess(processBuilder.toString());
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        locationListener = new OldLocationListener();
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            processBuilder.append("old api no permissions\n");
            setProcess(processBuilder.toString());
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 15_000, 0f, locationListener);
    }

    private void takeCoordsNewApi() {
        processBuilder.append("start new api gps\n");
        setProcess(processBuilder.toString());
        request = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(15_000);
        locationProvider = new ReactiveLocationProvider(context);
        setStart(true);
        try {
            subscriptions.add(locationProvider.getUpdatedLocation(request)
                    .observeOn(Schedulers.newThread())
                    .subscribe(this::saveLocation, throwable -> {
                        //error take location
                        setError("Throwable in getUpdatedLocation " + (throwable != null ? throwable.getMessage() : "Null throwable"));
                        processBuilder.append("new api error getUpdatedLocations\n");
                        setProcess(processBuilder.toString());
                    })
            );
        } catch (SecurityException ex) {
            //securityError
            setError("Throwable in getUpdatedLocation security (try/catch) " + ex.getMessage());
            processBuilder.append("new Api error security\n");
            setProcess(processBuilder.toString());
            setStart(false);
        }
    }

    private void saveLocation(Location location) {
        processBuilder.append("save location\n");
        setProcess(processBuilder.toString());
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("lat = ");
        stringBuilder.append(location.getLatitude());
        stringBuilder.append(", lng = ");
        stringBuilder.append(location.getLongitude());
        setCurrentLocation(stringBuilder.toString());
    }

    private boolean checkLocationServiceEnabled() {
        processBuilder.append("check gps on\n");
        setProcess(processBuilder.toString());
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        try {
            geolocationEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception ex) {
            //error check location
            setError("error check location " + ex.getMessage());
            processBuilder.append("error check gps\n");
            setProcess(processBuilder.toString());
        }
        return buildAlertMessageNoLocationService(geolocationEnabled);
    }

    private boolean buildAlertMessageNoLocationService(boolean network_enabled) {

        if (!network_enabled) {
            if (alertGPS != null && !alertGpsIsShowing) {
                processBuilder.append("dialog gps off build\n");
                setProcess(processBuilder.toString());
                alertGPS.show();
                alertGpsIsShowing = true;
            }
            return false;
        } else {
            if (alertGPS != null && alertGPS.isShowing()) {
                alertGPS.dismiss();
                alertGpsIsShowing = false;
            }
        }
        return true;
    }

    private void initDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setCancelable(false)
                .setMessage(R.string.gps_not_work)
                .setPositiveButton(context.getString(R.string.turn_on), (dialog, id) -> {
                    context.startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    dialog.dismiss();
                    alertGpsIsShowing = false;
                });
        alertGPS = builder.create();
    }

    void onDestroy() {
        processBuilder.append("destroy\n");
        setProcess(processBuilder.toString());
        subscriptions.dispose();
        if (locationManager != null && locationListener != null) {
            locationManager.removeUpdates(locationListener);
            processBuilder.append("old api remove updates\n");
            setProcess(processBuilder.toString());
        }
        locationProvider = null;
        request = null;
    }

    void onResume() {
        if (context != null) {
            processBuilder.append("restart check gps coords\n");
            setProcess(processBuilder.toString());
            startCheckGps();
        }
    }

    void setContext(AppCompatActivity context) {
        this.context = context;
        initDialog();
    }

    @Bindable
    public boolean isStart() {
        return isStart;
    }

    public void setStart(boolean start) {
        isStart = start;
        notifyPropertyChanged(BR.start);
    }

    @Bindable
    public String getCurrentLocation() {
        return currentLocation;

    }

    public void setCurrentLocation(String currentLocation) {
        this.currentLocation = currentLocation;
        notifyPropertyChanged(BR.currentLocation);
    }

    @Bindable
    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
        notifyPropertyChanged(BR.error);
    }

    @Bindable
    public String getProcess() {
        return process;
    }

    public void setProcess(String process) {
        this.process = process;
        notifyPropertyChanged(BR.process);
    }

    private class OldLocationListener implements LocationListener {

        @Override
        public void onLocationChanged(Location loc) {
            processBuilder.append("LocationListener onchanged\n");
            setProcess(processBuilder.toString());
            saveLocation(loc);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {
            setError("provider gps disabled " + provider);
            processBuilder.append("Location Listener provider disabled\n");
            setProcess(processBuilder.toString());
        }

    }
}
