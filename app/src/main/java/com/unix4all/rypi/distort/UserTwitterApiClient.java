package com.unix4all.rypi.distort;

import android.support.annotation.Nullable;

import com.twitter.sdk.android.core.TwitterApiClient;
import com.twitter.sdk.android.core.TwitterSession;
import com.twitter.sdk.android.core.models.User;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public class UserTwitterApiClient extends TwitterApiClient {
    public UserTwitterApiClient(TwitterSession session) {
        super(session);
    }

    public SearchUsersService getSearchUsersService() {
        return getService(SearchUsersService.class);
    }

    interface SearchUsersService {
        @GET("/1.1/users/search.json")
        Call<List<User>> search(@Query("q") String handle, @Query("count") Integer limit);
    }
}
