package com.mxt.anitrend.model.api.retro;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mxt.anitrend.BuildConfig;
import com.mxt.anitrend.base.custom.async.WebTokenRequest;
import com.mxt.anitrend.model.api.interceptor.AuthInterceptor;
import com.mxt.anitrend.model.api.interceptor.CacheInterceptor;
import com.mxt.anitrend.model.api.interceptor.NetworkCacheInterceptor;
import com.mxt.anitrend.model.api.retro.anilist.AuthModel;
import com.mxt.anitrend.model.api.retro.base.GiphyModel;
import com.mxt.anitrend.model.api.retro.base.RepositoryModel;
import com.mxt.anitrend.model.api.retro.crunchy.EpisodeModel;
import com.mxt.anitrend.model.entity.anilist.WebToken;
import com.mxt.anitrend.model.entity.base.AuthCode;
import com.mxt.anitrend.util.CompatUtil;
import com.mxt.anitrend.util.ErrorUtil;
import com.mxt.anitrend.util.KeyUtils;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.simplexml.SimpleXmlConverterFactory;

import static com.mxt.anitrend.util.KeyUtils.AUTHENTICATION_CODE;
import static com.mxt.anitrend.util.KeyUtils.GrantTypes;

/**
 * Created by max on 2017/10/14.
 * Retrofit service factory
 */

public class WebFactory {

    public final static Gson gson = new GsonBuilder()
            .enableComplexMapKeySerialization()
            .setLenient().create();

    private final static OkHttpClient.Builder baseClient = new OkHttpClient.Builder();

    private final static Retrofit.Builder anitrendBuilder = new Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create(gson))
            .baseUrl(BuildConfig.API_LINK);

    private final static Retrofit.Builder crunchyBuilder = new Retrofit.Builder()
            .addConverterFactory(SimpleXmlConverterFactory.createNonStrict());

    private final static Retrofit.Builder giphyBuilder = new Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create(gson))
            .baseUrl(BuildConfig.GIPHY_LINK);

    public final static String API_AUTH_LINK =
            String.format("https://anilist.co/api/auth/authorize?grant_type=%s&client_id=%s&redirect_uri=%s&response_type=%s",
            GrantTypes[AUTHENTICATION_CODE], BuildConfig.CLIENT_ID, BuildConfig.REDIRECT_URI, BuildConfig.RESPONSE_TYPE);

    private static Retrofit mRetrofit, mCrunchy, mGiphy;

    /**
     * Generates retrofit service classes in a background thread
     * and handles creation of API tokens or renewal of them
     * <br/>
     *
     * @param serviceClass The interface class to use such as
     *
     * @param context A valid application, fragment or activity context but must be application context
     */
    public static <S> S createService(@NonNull Class<S> serviceClass, Context context) {
        WebTokenRequest.getToken(context);
        if(mRetrofit == null) {
            OkHttpClient.Builder httpClient = new OkHttpClient.Builder()
                    .readTimeout(35, TimeUnit.SECONDS)
                    .connectTimeout(35, TimeUnit.SECONDS)
                    .addInterceptor(new AuthInterceptor());

            if(BuildConfig.DEBUG) {
                HttpLoggingInterceptor httpLoggingInterceptor = new HttpLoggingInterceptor()
                        .setLevel(HttpLoggingInterceptor.Level.BODY);
                httpClient.addInterceptor(httpLoggingInterceptor);
            }

            mRetrofit = anitrendBuilder.client(httpClient.build()).build();
        }
        return mRetrofit.create(serviceClass);
    }

    public static EpisodeModel createCrunchyService(boolean feeds, Context context) {
        if(mCrunchy == null) {
            OkHttpClient.Builder httpClient = new OkHttpClient.Builder()
                    .readTimeout(45, TimeUnit.SECONDS)
                    .connectTimeout(45, TimeUnit.SECONDS)
                    .addInterceptor(new CacheInterceptor(context, true))
                    .addNetworkInterceptor(new NetworkCacheInterceptor(context, true))
                    .cache(CompatUtil.cacheProvider(context));

            if(BuildConfig.DEBUG) {
                HttpLoggingInterceptor httpLoggingInterceptor = new HttpLoggingInterceptor()
                        .setLevel(HttpLoggingInterceptor.Level.BASIC);
                httpClient.addInterceptor(httpLoggingInterceptor);
            }
            crunchyBuilder.client(httpClient.build());
        }
        mCrunchy = crunchyBuilder.baseUrl(feeds?BuildConfig.FEEDS_LINK:BuildConfig.CRUNCHY_LINK).build();
        return mCrunchy.create(EpisodeModel.class);
    }

    public static GiphyModel createGiphyService(Context context) {
        if(mGiphy == null) {
            OkHttpClient.Builder httpClient = new OkHttpClient.Builder()
                    .readTimeout(35, TimeUnit.SECONDS)
                    .connectTimeout(35, TimeUnit.SECONDS)
                    .addInterceptor(new CacheInterceptor(context, true))
                    .addNetworkInterceptor(new NetworkCacheInterceptor(context, true))
                    .cache(CompatUtil.cacheProvider(context));

            if(BuildConfig.DEBUG) {
                HttpLoggingInterceptor httpLoggingInterceptor = new HttpLoggingInterceptor()
                        .setLevel(HttpLoggingInterceptor.Level.BASIC);
                httpClient.addInterceptor(httpLoggingInterceptor);
            }
            mGiphy = giphyBuilder.client(httpClient.build()).build();
        }
        return mGiphy.create(GiphyModel.class);
    }

    public static RepositoryModel createRepositoryService() {
        return new Retrofit.Builder().addConverterFactory(GsonConverterFactory.create(gson))
                .client(baseClient.build()).baseUrl(BuildConfig.APP_REPO)
                .build().create(RepositoryModel.class);
    }

    /**
     * Get a new token, the token that will be requested will vary depending on the authentication
     * state of the application
     */
    public static @Nullable WebToken refreshTokenSync(AuthCode authCode, boolean isAuthenticated) {
        try {
            Call<WebToken> refreshTokenCall;
            if (isAuthenticated)
                refreshTokenCall = anitrendBuilder.client(baseClient.build()).build()
                        .create(AuthModel.class).getAccessToken(GrantTypes[KeyUtils.REFRESH_TYPE],
                                BuildConfig.CLIENT_ID, BuildConfig.CLIENT_SECRET, authCode.getRefresh_code());
            else
                refreshTokenCall = anitrendBuilder.client(baseClient.build()).build()
                        .create(AuthModel.class).getAccessToken(GrantTypes[KeyUtils.AUTHENTICATION_TYPE],
                                BuildConfig.CLIENT_ID, BuildConfig.CLIENT_SECRET);

            Response<WebToken> response = refreshTokenCall.execute();
            if (!response.isSuccessful())
                Log.e("refreshTokenSync", ErrorUtil.getError(response));
            return response.body();
        } catch (Exception ex) {
            ex.printStackTrace();
            Log.e("refreshTokenSync", ex.getLocalizedMessage());
            return null;
        }
    }

    /**
     * Gets a new access token using the authentication code code provided from a callback
     */
    public static @Nullable WebToken requestCodeTokenSync(String code) {
        try {
            Call<WebToken> refreshTokenCall = anitrendBuilder.client(baseClient.build()).build()
                        .create(AuthModel.class).getAuthRequest(GrantTypes[KeyUtils.AUTHENTICATION_CODE],
                                BuildConfig.CLIENT_ID, BuildConfig.CLIENT_SECRET, BuildConfig.REDIRECT_URI, code);

            Response<WebToken> response = refreshTokenCall.execute();
            if(!response.isSuccessful())
                Log.e("requestCodeTokenSync", ErrorUtil.getError(response));
            return response.body();
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public static void invalidate() {
        mRetrofit = null;
        mCrunchy = null;
        mGiphy = null;
    }
}