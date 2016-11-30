package io.pijun.george.api;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;

import com.google.firebase.crash.FirebaseCrash;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.squareup.tape.FileObjectQueue;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import io.pijun.george.api.adapter.BytesToBase64Adapter;
import io.pijun.george.api.adapter.CommTypeAdapter;
import io.pijun.george.api.task.AddFcmTokenTask;
import io.pijun.george.api.task.DeleteFcmTokenTask;
import io.pijun.george.api.task.DeleteMessageTask;
import io.pijun.george.api.task.OscarTask;
import io.pijun.george.api.task.QueueConverter;
import io.pijun.george.api.task.SendMessageTask;
import io.pijun.george.crypto.EncryptedData;
import io.pijun.george.service.OscarTasksService;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class OscarClient {

    public static final Gson sGson;
    private static volatile FileObjectQueue<OscarTask> sQueue;
    private static final String QUEUE_FILENAME = "oscar.queue";

    static {
        sGson = new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .registerTypeAdapter(byte[].class, new BytesToBase64Adapter())
                .registerTypeAdapter(CommType.class, new CommTypeAdapter())
                .create();
    }

    public static FileObjectQueue<OscarTask> getQueue(Context context) {
        if (sQueue == null) {
            synchronized (OscarClient.class) {
                if (sQueue == null) {
                    File queueFile = new File(context.getFilesDir(), QUEUE_FILENAME);
                    try {
                        sQueue = new FileObjectQueue<>(queueFile, new QueueConverter());
                    } catch (IOException ex) {
                        // out of disk space?
                        FirebaseCrash.report(ex);
                    }
                }
            }
        }

        return sQueue;
    }

    public static OscarAPI newInstance(final String accessToken) {
//        String url = "http://192.168.1.76:9999/alpha/";
        String url = "https://api.pijun.io/alpha/";

        OkHttpClient.Builder httpBuilder = new OkHttpClient.Builder();
//        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
//        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
//        httpBuilder.addInterceptor(interceptor);
        if (accessToken != null) {
            httpBuilder.addInterceptor(new Interceptor() {
                @Override
                public Response intercept(Chain chain) throws IOException {
                    Request request = chain.request();
                    request = request.newBuilder()
                            .addHeader("X-Oscar-Access-Token", accessToken)
                            .build();

                    return chain.proceed(request);
                }
            });
        }
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(url)
                .addConverterFactory(GsonConverterFactory.create(sGson))
                .client(httpBuilder.build())
                .build();

        return retrofit.create(OscarAPI.class);
    }

    @WorkerThread
    public static void queueAddFcmToken(@NonNull Context context, @NonNull String accessToken, @NonNull Map<String, String> body) {
        AddFcmTokenTask aftt = new AddFcmTokenTask(accessToken);
        aftt.body = body;
        getQueue(context).add(aftt);
        context.startService(OscarTasksService.newIntent(context));
    }

    @WorkerThread
    public static void queueDeleteFcmToken(@NonNull Context context, @NonNull String accessToken, @NonNull String fcmToken) {
        DeleteFcmTokenTask dftt = new DeleteFcmTokenTask(accessToken);
        dftt.fcmToken = fcmToken;
        getQueue(context).add(dftt);
        context.startService(OscarTasksService.newIntent(context));
    }

    @WorkerThread
    public static void queueDeleteMessage(@NonNull Context context, @NonNull String accessToken, long msgId) {
        DeleteMessageTask dmt = new DeleteMessageTask(accessToken);
        dmt.messageId = msgId;
        getQueue(context).add(dmt);
        context.startService(OscarTasksService.newIntent(context));
    }

    @WorkerThread
    public static void queueSendMessage(@NonNull Context context, @NonNull String accessToken, @NonNull String toUserId, @NonNull EncryptedData msg) {
        SendMessageTask smt = new SendMessageTask(accessToken);
        smt.hexUserId = toUserId;
        smt.message = msg;
        getQueue(context).add(smt);
        context.startService(OscarTasksService.newIntent(context));
    }

}
