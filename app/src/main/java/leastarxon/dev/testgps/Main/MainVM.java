package leastarxon.dev.testgps.Main;

import android.content.Context;
import android.content.Intent;
import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.location.Location;
import android.location.LocationManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.google.android.gms.location.LocationRequest;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import leastarxon.dev.testgps.BR;
import leastarxon.dev.testgps.R;
import leastarxon.dev.testgps.Utils.PermissionHelper;
import pl.charmas.android.reactivelocation2.ReactiveLocationProvider;


public class MainVM extends BaseObservable {
    private AlertDialog alertGPS;
    private AppCompatActivity context;
    private LocationRequest request;
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
        onDestroy();
        subscriptions = new CompositeDisposable();
        setError(null);
        setCurrentLocation("");
        onResume();
    };

    private void startCheckGps() {
        if (PermissionHelper.checkPermissionsForGPS(context)) {
            if (checkLocationServiceEnabled()) {
                //startLocation
                takeCoords();
                setError(null);
            } else {
                //no gps
                setError("No gps");
            }
        } else {
            //no permission
            setError("No permissions");
        }
    }

    private void takeCoords() {
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
                    })
            );
        } catch (SecurityException ex) {
            //securityError
            setError("Throwable in getUpdatedLocation security (try/catch) " + ex.getMessage());
            setStart(false);
        }
    }

    private void saveLocation(Location location) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("lat = ");
        stringBuilder.append(location.getLatitude());
        stringBuilder.append(", lng = ");
        stringBuilder.append(location.getLongitude());
        setCurrentLocation(stringBuilder.toString());
    }

    private boolean checkLocationServiceEnabled() {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        try {
            geolocationEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception ex) {
            //error check location
            setError("error check location " + ex.getMessage());
        }
        return buildAlertMessageNoLocationService(geolocationEnabled);
    }

    private boolean buildAlertMessageNoLocationService(boolean network_enabled) {

        if (!network_enabled) {
            if (alertGPS != null && !alertGpsIsShowing) {
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
        subscriptions.dispose();
        locationProvider = null;
        request = null;
    }

    void onResume() {
        if (context != null) {
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
}
