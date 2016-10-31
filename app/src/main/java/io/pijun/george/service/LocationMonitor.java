package io.pijun.george.service;

import android.app.Service;
import android.app.job.JobScheduler;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.support.annotation.WorkerThread;
import android.text.format.DateUtils;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.ActivityRecognitionApi;
import com.squareup.otto.Subscribe;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;

import io.pijun.george.App;
import io.pijun.george.DB;
import io.pijun.george.Hex;
import io.pijun.george.L;
import io.pijun.george.Prefs;
import io.pijun.george.Sodium;
import io.pijun.george.WorkerRunnable;
import io.pijun.george.api.OscarAPI;
import io.pijun.george.api.OscarClient;
import io.pijun.george.api.UserComm;
import io.pijun.george.crypto.KeyPair;
import io.pijun.george.crypto.PKEncryptedMessage;
import io.pijun.george.models.FriendRecord;
import retrofit2.Response;

public class LocationMonitor extends Service {

    public static Intent newIntent(Context ctx) {
        return new Intent(ctx, LocationMonitor.class);
    }

    private static Handler sServiceHandler;
    static {
        HandlerThread thread = new HandlerThread("LocationMonitor");
        thread.start();

        Looper looper = thread.getLooper();
        sServiceHandler = new Handler(looper);
    }

    public class LocalBinder extends Binder {
        LocationMonitor getService() {
            return LocationMonitor.this;
        }
    }

    private LinkedList<Location> mLocations = new LinkedList<>();
    private final IBinder mBinder = new LocalBinder();
    private GoogleApiClient mGoogleClient;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        L.i("LM.onBind " + this);

        return mBinder;
    }

    @Override
    @UiThread
    public void onCreate() {
        super.onCreate();

        L.i("LM.onCreate");
        JobScheduler scheduler = (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE);
        scheduler.schedule(LocationJobService.getJobInfo(this));

        App.runInBackground(new WorkerRunnable() {
            @Override
            public void run() {
                beginActivityMonitoring();
            }
        });

        App.registerOnBus(this);
    }

    @Override
    @UiThread
    public int onStartCommand(Intent intent, int flags, int startId) {
        L.i("LM.onStartCommand");
        return START_STICKY;
    }

    @Override
    @UiThread
    public void onDestroy() {
        L.i("LM.onDestroy " + this);
        App.unregisterFromBus(this);

        super.onDestroy();
    }

    @WorkerThread
    private void beginActivityMonitoring() {
        mGoogleClient = new GoogleApiClient.Builder(this)
                .addApi(ActivityRecognition.API)
                .build();
        ConnectionResult connectionResult = mGoogleClient.blockingConnect();
        if (!connectionResult.isSuccess()) {
            L.i("|  google client connect failed");
            L.i("|  has resolution? " + connectionResult.hasResolution());
        }

        PendingResult<Status> result = ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(
                mGoogleClient,
                5 * DateUtils.MINUTE_IN_MILLIS,
                ActivityMonitor.getPendingIntent(this));
        result.setResultCallback(new ResultCallback<Status>() {
            @Override
            public void onResult(@NonNull Status status) {
                if (status.isSuccess()) {
                    L.i("successfully registered for activity recognition");
                } else {
                    L.i("failed to register for activity recognition");
                }
            }
        });
    }

    @Subscribe
    @Keep
    public void onLocationChanged(final Location l) {
        sServiceHandler.post(new WorkerRunnable() {
            @Override
            public void run() {
                // check if this is a duplicate location
                if (!mLocations.isEmpty() && mLocations.getLast().getElapsedRealtimeNanos() == l.getElapsedRealtimeNanos()) {
                    return;
                }

                L.i("LM.onLocationChanged - " + l);
                mLocations.add(l);
            }
        });
    }

    /**
     * Get the most recent location and report it.
     */
    @WorkerThread
    void flush() {
        L.i("LM.flush");

        // If we have no location to report, just get out of here.
        if (mLocations.isEmpty()) {
            return;
        }

        Prefs prefs = Prefs.get(this);
        String token = prefs.getAccessToken();
        KeyPair keyPair = prefs.getKeyPair();
        if (token == null || keyPair == null) {
            L.i("LM.flush token or keypair was null, so skipping upload");
            mLocations.clear();
            return;
        }

        Location location = mLocations.getLast();
        UserComm locMsg = UserComm.newLocationInfo(location);
        byte[] msgBytes = locMsg.toJSON();
        ArrayList<FriendRecord> friends = DB.get(this).getFriendsToShareWith();
        OscarAPI api = OscarClient.newInstance(token);
        for (FriendRecord fr : friends) {
            L.i("|  friend: " + fr);
            L.i("|  send box: " + Hex.toHexString(fr.sendingBoxId));
            PKEncryptedMessage encryptedMessage = Sodium.publicKeyEncrypt(msgBytes, fr.publicKey, keyPair.secretKey);
            try {
                Response<Void> response = api.dropPackage(Hex.toHexString(fr.sendingBoxId), encryptedMessage).execute();
                if (!response.isSuccessful()) {
                    L.w("problem dropping location_info package");
                }
            } catch (IOException ex) {
                L.w("Serious error dropping location_info package", ex);
            }
        }

        mLocations.clear();
    }
}