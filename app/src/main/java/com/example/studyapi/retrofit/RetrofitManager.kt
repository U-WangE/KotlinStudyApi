package com.example.studyapi.retrofit

import android.util.Log
import com.example.studyapi.model.Photo
import com.example.studyapi.utils.API
import com.example.studyapi.utils.Constants.TAG
import com.example.studyapi.utils.RESPONSE_STATUS
import com.google.gson.JsonElement
import retrofit2.Call
import retrofit2.Response
import java.text.SimpleDateFormat

class RetrofitManager {

    companion object {
        var instance = RetrofitManager()
    }

    // Retrofit interface 가져오기
    private var iRetrofit: IRetrofit? = RetrofitClient.getClient(API.BASE_URL)?.create(IRetrofit::class.java)

    // 사진 검색 api 호출
    fun searchPhotos(searchTerm: String?, completion: (RESPONSE_STATUS, ArrayList<Photo>?) -> Unit) {

        var term = searchTerm ?: ""

        val call = iRetrofit?.searchPhotos(searchTerm = term) ?: return

        call.enqueue(object: retrofit2.Callback<JsonElement> {
            // 응답 성공시
            override fun onResponse(call: Call<JsonElement>, response: Response<JsonElement>) {
                Log.d(TAG, "RetrofitManager - onResponse() called / response : ${response.body()}")

                when(response.code()) {
                    200 -> {
                        response.body()?.let {

                            var parsedPhotoDataArray = ArrayList<Photo>()

                            var body = it.asJsonObject

                            var results = body.getAsJsonArray("results")

                            val total = body.get("total").asInt

                            Log.d(TAG, "RetrofitManager - onResponse() called / total: ${total}")

                            // 데이터가 없으면 no_content로 보낸다.
                            if(total == 0) {
                                completion(RESPONSE_STATUS.NO_CONTENT, null)

                            } else {  // 데이터가 있다면

                                results.forEach { resultItem ->
                                    val resultItemObject = resultItem.asJsonObject

                                    val user = resultItemObject.get("user").asJsonObject

                                    val username: String = user.get("username").asString

                                    val likesCount = resultItemObject.get("likes").asInt

                                    val thumbnaillink =
                                        resultItemObject.get("urls").asJsonObject.get("thumb").asString

                                    val createdAt = resultItemObject.get("created_at").asString

                                    val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
                                    val formatter = SimpleDateFormat("yyyy년\nMM월 dd일")

                                    val outputDateString = formatter.format(parser.parse(createdAt))

                                    Log.d(
                                        TAG,
                                        "RetrofitManager - outputDateString : ${outputDateString}"
                                    )

                                    val photoItem = Photo(
                                        author = username,
                                        likesCount = likesCount,
                                        thumbnail = thumbnaillink,
                                        createdAt = outputDateString
                                    )
                                    parsedPhotoDataArray.add(photoItem)
                                }
                                completion(RESPONSE_STATUS.OKAY, parsedPhotoDataArray)
                            }
                        }
                    }
                }
            }

            // 응답 실패시
            override fun onFailure(call: Call<JsonElement>, t: Throwable) {
                Log.d(TAG, "RetrofitManager - onFailure() called / t : $t")

                completion(RESPONSE_STATUS.FAIL, null)
            }

        })

    }
}