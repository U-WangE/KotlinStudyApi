package com.example.studyapi.utils

import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.EditText
import com.example.studyapi.utils.Constants.TAG
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.onStart
import java.text.SimpleDateFormat
import java.util.*


// 문자열이 JSON 형태인지
fun String?.isJsonObject(): Boolean {
    if(this?.startsWith( "{") == true && this.endsWith( "}") ) {
        return true
    } else {
        return false
    }
}

// 문자열이 JSON 배열 형태인지
fun String?.isJsonArray(): Boolean {
    if(this?.startsWith( "[") == true && this.endsWith( "]") ) {
        return true
    } else {
        return false
    }
}

// 날짜 포멧
fun Date.toSimpleString() : String {
    val format = SimpleDateFormat("HH:mm:ss")
    return format.format(this)
}

// 에딧 텍스트에 대한 익스텐션
fun EditText.onMyTextChanged(completion: (Editable?) -> Unit) {
    this.addTextChangedListener(object: TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

        }

        override fun afterTextChanged(editable: Editable?) {
            completion(editable)
        }

    })
}

// 에딧텍스트 텍스트 변경을 flow로 받기
fun EditText.textChangesToFlow(): Flow<CharSequence?> {
    // flow 콜백 받기
    return callbackFlow {
        val listener = object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                Unit
            }

            override fun afterTextChanged(s: Editable?) {
                Unit
            }

            override fun onTextChanged(text: CharSequence?, start: Int, before: Int, count: Int) {
                Log.d(TAG, "onTextChanged() / textChangesToFlow() 에 달려있는 텍스트 와쳐 / text : $text")
                // 값 내보내기
                trySend(text)
            }
        }
        // 위에서 설정한 listener 달아주기
        addTextChangedListener(listener)

        // 콜백이 사라질 때 실행 됨
        awaitClose{
            Log.d(TAG, "textChangesToFlow() awaitClose 실행")
            removeTextChangedListener(listener)
        }
    }.onStart {
        Log.d(TAG, "textChangesToFlow() / onStart 발동")
        // Rx 에서 onNext와 동일
        // emit 으로 이벤트를 전달
        emit(text)
    }
}