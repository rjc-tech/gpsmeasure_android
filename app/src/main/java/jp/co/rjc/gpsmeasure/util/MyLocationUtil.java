package jp.co.rjc.gpsmeasure.util;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.widget.Toast;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import static android.content.Context.LOCATION_SERVICE;

/**
 * 位置情報に関する機能を提供します.
 */
public class MyLocationUtil {

    private static MyLocationUtil sInstance;
    private Activity mActivity;

    private LocationManager mLocationManager;
    private LocationListener mLocationListener;
    private Timer mLocationTimer;
    private String mProvider;
    private long mTime;

    private OnProcessCallbackListener mOnProcessCallbackListener;

    private MyLocationUtil() {
    }

    private MyLocationUtil(final Activity activity) {
        mActivity = activity;
    }

    /**
     * 唯一のインスタンを返却します.
     *
     * @param activity
     * @return
     */
    public static MyLocationUtil getsInstance(final Activity activity) {
        if (sInstance == null) {
            sInstance = new MyLocationUtil(activity);
        }
        return sInstance;
    }

    /**
     * 位置情報取得後のプロセスコールバックリスナー.
     */
    public interface OnProcessCallbackListener {
        void onSuccessLocation(final double latitude, final double longitude);

        void onFailedLodation();
    }

    public void setOnProcessCallbackListener(final OnProcessCallbackListener onProcessCallbackListener) {
        mOnProcessCallbackListener = onProcessCallbackListener;
    }

    public void removeOnProcessCallbackListener() {
        mOnProcessCallbackListener = null;
    }

    /**
     * 位置情報マネージャを初期化します.
     *
     * @return
     */
    public boolean initCurrentLocation() {
        mLocationManager = (LocationManager) mActivity.getSystemService(LOCATION_SERVICE);
        if (mLocationManager == null) {
            return false;
        }
        if (mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            mProvider = "gps";
        } else if (mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            mProvider = "network";
        } else {
            mProvider = null;
        }
        return true;
    }

    public void startLocationService() {
        stopLocationService();
        initCurrentLocation();

        final Criteria criteria = new Criteria();
        criteria.setBearingRequired(false);    // 方位不要
        criteria.setSpeedRequired(false);    // 速度不要
        criteria.setAltitudeRequired(false);    // 高度不要

        if (mProvider == null) {
            new AlertDialog.Builder(mActivity)
                    .setTitle("現在地機能を改善")
                    .setMessage("現在、位置情報は一部有効ではないものがあります。次のように設定すると、もっともすばやく正確に現在地を検出できるようになります:\n\n● 位置情報の設定でGPSとワイヤレスネットワークをオンにする\n\n● Wi-Fiをオンにする")
                    .setPositiveButton("設定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(final DialogInterface dialog, final int which) {
                            // 端末の位置情報設定画面へ遷移
                            try {
                                mActivity.startActivity(new Intent("android.settings.LOCATION_SOURCE_SETTINGS"));
                            } catch (final ActivityNotFoundException e) {
                                // 位置情報設定画面がない糞端末の場合は、仕方ないので何もしない
                            }
                        }
                    })
                    .setNegativeButton("スキップ", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(final DialogInterface dialog, final int which) {
                        }    // 何も行わない
                    })
                    .create()
                    .show();
            stopLocationService();
            return;
        }

        // 最後に取得できた位置情報が5分以内のものであれば有効とします。
        if (ActivityCompat.checkSelfPermission(
                mActivity,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(
                mActivity,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        final Location lastKnownLocation = mLocationManager.getLastKnownLocation(mProvider);
        if (lastKnownLocation != null && (new Date().getTime() - lastKnownLocation.getTime()) <= (5 * 60 * 1000L)) {
            setLocation(lastKnownLocation);
            return;
        }

        mLocationTimer = new Timer(true);
        mTime = 0L;
        final Handler handler = new Handler();
        mLocationTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (mTime == 1000L) {
                            Toast.makeText(mActivity, "現在地を特定しています。", Toast.LENGTH_LONG).show();
                        } else if (mTime >= (30 * 1000L)) {
                            Toast.makeText(mActivity, "現在地を特定できませんでした。", Toast.LENGTH_LONG).show();
                            stopLocationService();
                        }
                        mTime = mTime + 1000L;
                    }
                });
            }
        }, 0L, 1000L);

        // 位置情報の取得を開始します。
        mLocationListener = new LocationListener() {
            @Override
            public void onLocationChanged(final Location location) {
                setLocation(location);
            }

            @Override
            public void onProviderDisabled(final String provider) {
            }

            @Override
            public void onProviderEnabled(final String provider) {
            }

            @Override
            public void onStatusChanged(final String provider, final int status, final Bundle extras) {
            }
        };
        mLocationManager.requestLocationUpdates(mProvider, 60000, 1, mLocationListener);
    }

    void stopLocationService() {
        if (mLocationTimer != null) {
            mLocationTimer.cancel();
            mLocationTimer.purge();
            mLocationTimer = null;
        }
        if (mLocationManager != null) {
            if (mLocationListener != null) {
                mLocationManager.removeUpdates(mLocationListener);
                mLocationListener = null;
            }
            mLocationManager = null;
        }
    }

    void setLocation(final Location location) {
        stopLocationService();
        if (mOnProcessCallbackListener != null) {
            mOnProcessCallbackListener.onSuccessLocation(location.getLatitude(), location.getLongitude());
        }
    }

    /**
     * 緯度経度から住所文字列を取得し、返却します.
     *
     * @param latitude
     * @param longitude
     * @return
     */
    public String getAddress(double latitude, double longitude) {

        Geocoder geocoder = new Geocoder(mActivity, Locale.getDefault());
        List<Address> addresses;
        StringBuilder result = new StringBuilder();
        try {
            addresses = geocoder.getFromLocation(latitude, longitude, 1);
        } catch (IOException e) {
            return "";
        }

        for (Address address : addresses) {
            int idx = address.getMaxAddressLineIndex();
            // 1番目のレコードは国名のため省略
            for (int i = 1; i <= idx; i++) {
                result.append(address.getAddressLine(i));
            }
        }
        result.append("\n\nhttps://www.google.co.jp/maps/@");
        result.append(String.valueOf(latitude));
        result.append(",");
        result.append(String.valueOf(longitude));
        return result.toString();
    }

    public static float distFrom(float lat1, float lng1, float lat2, float lng2) {
        double earthRadius = 6371000; //meters
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        float dist = (float) (earthRadius * c);
        return dist;
    }
}
