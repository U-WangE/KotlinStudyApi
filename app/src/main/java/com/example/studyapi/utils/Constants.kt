package com.example.studyapi.utils

object Constants {
    const val TAG : String = "로그"

}

enum class SEARCH_TYPE {
    PHOTO,
    USER
}

enum class RESPONSE_STATUS {
    OKAY,
    FAIL,
    NO_CONTENT
}


object API {
    const val BASE_URL : String = "https://api.unsplash.com/"

    const val CLIENT_ID : String = "OVgR5jM_AqnnpSX0WXG1EaWJNz0__9lKXreHjNn_UFk"

    const val SEARCH_PHOTOS : String = "search/photos"
    const val SEARCH_USERS : String = "search/users"
}