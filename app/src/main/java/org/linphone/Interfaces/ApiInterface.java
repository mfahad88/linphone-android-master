package org.linphone.Interfaces;

import org.linphone.models.login.LoginResponse;
import retrofit2.Call;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface ApiInterface {
    @POST("/api/createuser?username=admin&password=Tringy%232020$&reseller=0&signup=0&otp=1")
    Call<LoginResponse> createuser(
            @Query("createuser") String number, @Query("createpassword") String password);
}
