package com.example.studyapi.retrofit

import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.example.studyapi.App
import com.example.studyapi.utils.API
import com.example.studyapi.utils.Constants.TAG
import com.example.studyapi.utils.isJsonArray
import com.example.studyapi.utils.isJsonObject
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

// singleton
object RetrofitClient {
    // Retorfit Client 선언

    private var retrofitClient: Retrofit?= null

    // Retrofit Client 가져오기
    fun getClient(baseUrl: String): Retrofit? {
        Log.d(TAG, "RetorfitClient - getClient() called")

        // OkHttp instance 생성
        var client = OkHttpClient.Builder()

        // 로그를 찍기 위해 Logging interceptor 추가
        var loggingInterceptor = HttpLoggingInterceptor(object: HttpLoggingInterceptor.Logger {
            override fun log(message: String) {
//                Log.d(TAG, "RetrofitClient - log() called / message: $message")

                when {
                    message.isJsonObject() ->
                        Log.d(TAG, JSONObject(message).toString(4))
                    message.isJsonArray() ->
                        Log.d(TAG, JSONObject(message).toString(4))
                    else -> {
                        try {
                            Log.d(TAG, JSONObject(message).toString(4))
                        } catch (e: Exception) {
                            Log.d(TAG, message)
                        }
                    }
                }
            }
        })

        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY)

        // 위에서 설정한 Logging Interceptor를 OkHttp Client에 추가
        client.addInterceptor(loggingInterceptor)


        // 기본 파라메터 인터셉터 설정
        val baseParameterInterceptor: Interceptor = (Interceptor { chain ->
            Log.d(TAG, "RetrofitClient - intercept() called")
            // 오리지날 리퀘스트
            val originalRequest: Request = chain.request()

            // 쿼리 파라메터 추가하기
            val addedUrl = originalRequest.url.newBuilder().addQueryParameter("client_id", API.CLIENT_ID).build()

            val finalRequest = originalRequest.newBuilder()
                .url(addedUrl)
                .method(originalRequest.method, originalRequest.body)
                .build()

            val response = chain.proceed(finalRequest)

            if(response.code != 200) {
                Handler(Looper.getMainLooper()).post{
                    Toast.makeText(App.instance, "${response.code} 에러입니다.", Toast.LENGTH_SHORT).show()
                }
            }

            response
//            chain.proceed(finalRequest)

        })

        // 위에서 설정한 기본파라메터 Interceptor를 OkHttp Client에 추가
        client.addInterceptor(baseParameterInterceptor)

        // 커넥션 타임아웃
        client.connectTimeout(10, TimeUnit.SECONDS)
        client.readTimeout(10, TimeUnit.SECONDS)
        client.writeTimeout(10, TimeUnit.SECONDS)
        client.retryOnConnectionFailure(true)


        if(retrofitClient == null) {

            // Retrofit Builder를 통해 인스턴스 생성
            retrofitClient = Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())

                // 위에서 설정한 Client로 Retrofit Client를 설정한다.
                .client(client.build())
                .build()
        }

        return retrofitClient
    }
}