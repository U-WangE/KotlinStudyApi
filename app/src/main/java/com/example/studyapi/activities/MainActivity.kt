package com.example.studyapi.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import com.example.studyapi.R
import com.example.studyapi.databinding.ActivityMainBinding
import com.example.studyapi.databinding.LayoutButtonSearchBinding
import com.example.studyapi.retrofit.RetrofitManager
import com.example.studyapi.utils.Constants.TAG
import com.example.studyapi.utils.RESPONSE_STATUS
import com.example.studyapi.utils.SEARCH_TYPE
import com.example.studyapi.utils.onMyTextChanged


class MainActivity : AppCompatActivity() {

    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding!!

    private var currentSearchType: SEARCH_TYPE = SEARCH_TYPE.PHOTO


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Log.d(TAG, "MainActivity - onCreate() called")

        // 라디오 그룹 가져오기
        binding.searchTermRadioGroup.setOnCheckedChangeListener { _, checkedId->

            // when
            when(checkedId) {
                R.id.photo_search_radio_btn -> {
                    Log.d(TAG, "사진검색 버튼 클릭")
                    binding.searchTermTextLayout.hint = resources.getString(R.string.photo_search_term_hint)
                    binding.searchTermTextLayout.startIconDrawable = resources.getDrawable(R.drawable.ic_baseline_person_24, resources.newTheme())
                    this.currentSearchType = SEARCH_TYPE.PHOTO
                }
                R.id.user_search_radio_btn -> {
                    Log.d(TAG, "사용자검색 버튼 클릭")
                    binding.searchTermTextLayout.hint = resources.getString(R.string.user_search_term_hint)
                    binding.searchTermTextLayout.startIconDrawable = resources.getDrawable(R.drawable.ic_baseline_photo_library_24, resources.newTheme())
                    this.currentSearchType = SEARCH_TYPE.USER
                }
            }
            Log.d(TAG, "MainActivity - OnCheckedChanged() called / currentSearchType : $currentSearchType")
        }

        val btn_binding = binding.frameSearchBtn
        //텍스트가 변경 되었을 때
        binding.searchTermEditText.onMyTextChanged {
            // 입력된 글자가 하나라도 있다면
            if(it.toString().count() > 0) {
                // 검색 버튼을 보여준다
                btn_binding.root.visibility = View.VISIBLE
                binding.searchTermTextLayout.helperText = ""
                // 스크롤뷰를 올린다.
                binding.mainScrollview.scrollTo(0, 200)
            } else {
                btn_binding.root.visibility = View.INVISIBLE
                binding.searchTermTextLayout.helperText = resources.getString(R.string.none_search_term_text)
            }

            if(it.toString().count() == 12) {
                Log.d(TAG, "MainActivity - 에러 띄우기")
                Toast.makeText(this, resources.getString(R.string.over_search_term_text), Toast.LENGTH_SHORT).show()
            }
        }

        // 검색 버튼 클릭시
        btn_binding.btnSearch.setOnClickListener {
            Log.d(TAG, "MainActivity - 검색 버튼이 클릭되었다. / currentSearchType : $currentSearchType")

            this.handleSearchButton(btn_binding)

            val userSearchInput = binding.searchTermEditText.text.toString()
            // 검색 api 호출
            RetrofitManager.instance.searchPhotos(searchTerm = userSearchInput, completion = {
                responseState, responseDataArrayList ->

                when(responseState) {
                    RESPONSE_STATUS.OKAY -> {
                        Log.d(TAG, "api 호출 성공 : ${responseDataArrayList?.size}")

                        val intent = Intent(this, PhotoCollectionActivity::class.java)
                        val bundle = Bundle()

                        bundle.putSerializable("photo_array_list", responseDataArrayList)
                        intent.putExtra("array_bundle", bundle)
                        intent.putExtra("search_term", userSearchInput)

                        startActivity(intent)
                    }
                    RESPONSE_STATUS.FAIL -> {
                        Toast.makeText(this, "api 호출 실패 : $responseDataArrayList", Toast.LENGTH_SHORT).show()
                    }
                    RESPONSE_STATUS.NO_CONTENT -> {
                        Toast.makeText(this, "검색 결과가 없습니다.", Toast.LENGTH_SHORT).show()
                    }
                }
                btn_binding.btnProgress.visibility = View.INVISIBLE
                btn_binding.btnSearch.text = "검색"
                binding.searchTermEditText.setText("")
            })
        }

    } // onCreate

    private fun handleSearchButton(btn_binding: LayoutButtonSearchBinding) {
        btn_binding.btnProgress.visibility = View.VISIBLE

        btn_binding.btnSearch.text = ""


//        Handler(Looper.getMainLooper()).postDelayed({
//            btn_binding.btnProgress.visibility = View.INVISIBLE
//            btn_binding.btnSearch.text = "검색"
//        }, 1500)
    }
}