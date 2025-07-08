package com.example.fowltyphoidmonitor.data.api;

import android.content.Context;
import android.util.Log;

import com.example.fowltyphoidmonitor.config.SupabaseConfig;
import com.example.fowltyphoidmonitor.services.auth.AuthManager;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Singleton class to handle Supabase API connections
 * Manages the Retrofit instances and provides API services
 */
public class SupabaseClient {
    private static final String TAG = "SupabaseClient";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

    private static SupabaseClient instance;
    private final Retrofit authRetrofit;
    private final Retrofit apiRetrofit;
    private final Context context;

    private SupabaseClient(Context context) {
        this.context = context.getApplicationContext();

        // Create HTTP client with logging and authentication
        OkHttpClient.Builder httpClientBuilder = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS);

        // Add logging interceptor
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        httpClientBuilder.addInterceptor(loggingInterceptor);

        // Auth client - just needs API key
        OkHttpClient authClient = httpClientBuilder
                .addInterceptor(chain -> {
                    Request original = chain.request();
                    Request.Builder requestBuilder = original.newBuilder()
                            .header(SupabaseConfig.API_KEY_HEADER, SupabaseConfig.SUPABASE_ANON_KEY)
                            .method(original.method(), original.body());
                    logRequest("Auth request", requestBuilder.build());
                    return chain.proceed(requestBuilder.build());
                })
                .build();

        // API client - needs both API key and authorization token
        OkHttpClient apiClient = httpClientBuilder
                .addInterceptor(new AuthInterceptor())
                .build();

        // Create Retrofit instances
        authRetrofit = new Retrofit.Builder()
                .baseUrl(SupabaseConfig.SUPABASE_URL + SupabaseConfig.AUTH_API_PATH)
                .addConverterFactory(GsonConverterFactory.create())
                .client(authClient)
                .build();

        apiRetrofit = new Retrofit.Builder()
                .baseUrl(SupabaseConfig.SUPABASE_URL + SupabaseConfig.REST_API_PATH)
                .addConverterFactory(GsonConverterFactory.create())
                .client(apiClient)
                .build();
    }

    public static synchronized SupabaseClient getInstance(Context context) {
        if (instance == null) {
            instance = new SupabaseClient(context);
            logInfo("SupabaseClient instance created at " + getCurrentTimestamp());
        }
        return instance;
    }

    public AuthService getAuthService() {
        return authRetrofit.create(AuthService.class);
    }

    public ApiService getApiService() {
        return apiRetrofit.create(ApiService.class);
    }

    /**
     * Creates an API interface using the apiRetrofit instance
     * @param apiClass The class of the API interface to create
     * @param <T> The type of the API interface
     * @return An instance of the requested API interface
     */
    public <T> T createApi(Class<T> apiClass) {
        logInfo("Creating API interface: " + apiClass.getSimpleName());
        return apiRetrofit.create(apiClass);
    }

    /**
     * Interceptor to add authentication headers to all API requests
     */
    private class AuthInterceptor implements Interceptor {
        @Override
        public Response intercept(Chain chain) throws IOException {
            if (context == null) {
                logError("Context is null, cannot proceed with authentication");
                throw new IllegalStateException("Context is null in AuthInterceptor");
            }

            AuthManager authManager = AuthManager.getInstance(context);
            String token = authManager.getAuthToken();

            Request original = chain.request();
            Request.Builder requestBuilder = original.newBuilder()
                    .header(SupabaseConfig.API_KEY_HEADER, SupabaseConfig.SUPABASE_ANON_KEY);

            if (token != null && !token.isEmpty()) {
                requestBuilder.header("Authorization", SupabaseConfig.AUTH_HEADER_PREFIX + token);
            }

            Request request = requestBuilder.method(original.method(), original.body()).build();
            logRequest("API request", request);

            Response response = chain.proceed(request);

            if (response.code() == 401) {
                logWarning("Received 401 Unauthorized at " + getCurrentTimestamp() + " - token may be expired");
                boolean refreshed = authManager.refreshToken();
                if (refreshed) {
                    token = authManager.getAuthToken();
                    if (token != null && !token.isEmpty()) {
                        request = original.newBuilder()
                                .header(SupabaseConfig.API_KEY_HEADER, SupabaseConfig.SUPABASE_ANON_KEY)
                                .header("Authorization", SupabaseConfig.AUTH_HEADER_PREFIX + token)
                                .method(original.method(), original.body())
                                .build();
                        response.close();
                        logInfo("Retrying request with refreshed token");
                        return chain.proceed(request);
                    }
                } else {
                    logError("Token refresh failed");
                }
            }

            return response;
        }
    }

    // Utility methods for logging with timestamps
    private static void logInfo(String message) {
        Log.i(TAG, "[" + getCurrentTimestamp() + "] " + message);
    }

    private static void logWarning(String message) {
        Log.w(TAG, "[" + getCurrentTimestamp() + "] " + message);
    }

    private static void logError(String message) {
        Log.e(TAG, "[" + getCurrentTimestamp() + "] " + message);
    }

    private static void logRequest(String type, Request request) {
        logInfo(type + ": " + request.url() + " - Headers: " + request.headers());
    }

    private static String getCurrentTimestamp() {
        return DATE_FORMAT.format(new Date());
    }
}