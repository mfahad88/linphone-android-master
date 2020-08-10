package org.vokka.Interfaces;

import com.google.gson.JsonElement;
import org.vokka.models.login.LoginResponse;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface ApiInterface {
    @POST("/api/createuser?username=admin&password=Tringy%232020$&reseller=0&signup=0&otp=1")
    Call<LoginResponse> createuser(
            @Query("createuser") String number, @Query("createpassword") String password);

    @POST("/api/activateaccount")
    Call<JsonElement> activateaccount(
            @Query("username") String number,
            @Query("password") String password,
            @Query("otp") String otp);

    @GET("/Vokka/getclientbalance.do")
    Call<JsonElement> getclientbalance(@Query("pin") String pin);
}
