package com.example.studyapi.activities

import android.app.SearchManager
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.text.InputFilter
import android.util.Log
import android.view.Menu
import android.view.View
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.studyapi.R
import com.example.studyapi.databinding.ActivityPhotoCollectionBinding
import com.example.studyapi.model.Photo
import com.example.studyapi.model.SearchData
import com.example.studyapi.recyclerview.ISearchHistoryRecyclerView
import com.example.studyapi.recyclerview.PhotoGridRecyclerViewAdapter
import com.example.studyapi.recyclerview.SearchHistoryRecyclerViewAdapter
import com.example.studyapi.retrofit.RetrofitManager
import com.example.studyapi.utils.Constants.TAG
import com.example.studyapi.utils.RESPONSE_STATUS
import com.example.studyapi.utils.SharedPrefManager
import com.example.studyapi.utils.textChangesToFlow
import com.example.studyapi.utils.toSimpleString
import com.jakewharton.rxbinding4.widget.textChanges
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList
import kotlin.coroutines.CoroutineContext

class PhotoCollectionActivity: AppCompatActivity(),
                                SearchView.OnQueryTextListener,
                                CompoundButton.OnCheckedChangeListener,
                                View.OnClickListener,
                                ISearchHistoryRecyclerView
{

    private var _binding: ActivityPhotoCollectionBinding? = null
    private val binding get() = _binding!!

    // 데이터
    private var photoList = ArrayList<Photo>()

    // 검색 기록 배열
    private var searchHistoryList = ArrayList<SearchData>()

    // 어답터
    private lateinit var photoGridRecyclerViewAdapter: PhotoGridRecyclerViewAdapter
    private lateinit var mySearchHistoryRecyclerViewAdapter: SearchHistoryRecyclerViewAdapter
    
    // 서치뷰
    private lateinit var mySearchView: SearchView

    // 서치뷰 에딧 텍스트
    private lateinit var mySearchViewEditText: EditText

//    // 옵저버블 통합 제거를 위한 CompositeDisposable
//    private val myCompositeDisposable = CompositeDisposable()

    private var myCoroutineJob: Job = Job()
    private val myCoroutineContext: CoroutineContext
        get() = Dispatchers.IO + myCoroutineJob

   override fun onCreate(savedInstanceState: Bundle?) {
       super.onCreate(savedInstanceState)
       _binding = ActivityPhotoCollectionBinding.inflate(layoutInflater)
       setContentView(binding.root)

       val bundle = intent.getBundleExtra("array_bundle")
       val searchTerm = intent.getStringExtra("search_term")

       binding.searchHistoryModeSwitch.setOnCheckedChangeListener(this)
       binding.clearSearchHistoryButton.setOnClickListener(this)

       binding.searchHistoryModeSwitch.isChecked = SharedPrefManager.checkSearchHistoryMode()

       binding.topAppBar.title = searchTerm

        // 액티비티에서 어떤 액션바를 사용할지 설정한다.
       setSupportActionBar(binding.topAppBar)

       photoList = bundle?.getSerializable("photo_array_list") as ArrayList<Photo>

       // 사진 RecyclerView 세팅
       this.photoCollectionRecyclerViewSetting(this.photoList)

       // 저장된 검색 기록 가져오기
       this.searchHistoryList = SharedPrefManager.getSearchHistoryList() as ArrayList<SearchData>

       this.searchHistoryList.forEach {
           Log.d(TAG, "저장된 검색 기록 - it.term : ${it.term}, it.timestamp ${it.timestamp}")
       }

       // ui 처리
       handleSearchViewUi()

       // 검색 기록 RecyclerView 준비
       this.searchHistoryRecyclerViewSetting(this.searchHistoryList)

       if(searchTerm?.isNotEmpty() == true) {
           val term = searchTerm ?: ""
           this.insertSearchTermHistory(term)
       }

    } // onCreate

    override fun onDestroy() {
        Log.d(TAG, "PhotoCollectionActivity - onDestroy() called")
//        // 모두 삭제
//        this.myCompositeDisposable.clear()
        myCoroutineContext.cancel()

        super.onDestroy()
    }

    // 그리드 사진 RecyclerView 준비
    private fun photoCollectionRecyclerViewSetting(photoList: ArrayList<Photo>) {
        Log.d(TAG, "PhotoCollectionActivity - photoGridRecyclerViewSetting() called")
        this.photoGridRecyclerViewAdapter = PhotoGridRecyclerViewAdapter()
        this.photoGridRecyclerViewAdapter.submitList(photoList)

        binding.myPhotoRecyclerView.layoutManager = GridLayoutManager(this,
            2,
            GridLayoutManager.VERTICAL,
            false)
        binding.myPhotoRecyclerView.adapter = this.photoGridRecyclerViewAdapter
    }

    // 검색 기록 RecyclerView 준비
    private fun searchHistoryRecyclerViewSetting(searchHistoryList: ArrayList<SearchData>) {
        Log.d(TAG, "PhotoCollectionActivity - searchHistoryRecyclerViewSetting() called")

        this.mySearchHistoryRecyclerViewAdapter = SearchHistoryRecyclerViewAdapter(this)
        this.mySearchHistoryRecyclerViewAdapter.submitList(searchHistoryList)

        val myLinearLayoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, true)
        myLinearLayoutManager.stackFromEnd = true

        binding.searchHistoryRecyclerView.apply {
            layoutManager = myLinearLayoutManager
            this.scrollToPosition(mySearchHistoryRecyclerViewAdapter.itemCount - 1)
            adapter = mySearchHistoryRecyclerViewAdapter
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {

        Log.d(TAG, "PhotoCollectionActivity - onCreateOptionsMenu() called")

        val inflater = menuInflater
        inflater.inflate(R.menu.top_app_bar_menu, menu)

        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
        this.mySearchView = menu?.findItem(R.id.search_menu_item)?.actionView as SearchView
        this.mySearchView.apply {
            this.queryHint = "검색어를 입력해주세요"

            this.setOnQueryTextListener(this@PhotoCollectionActivity)

            this.setOnQueryTextFocusChangeListener { _, hasExpaned ->
                when(hasExpaned) {
                    true -> {
                        Log.d(TAG, "서치뷰 열림")
//                        binding.linearSearchHistoryView.visibility = View.VISIBLE
                        // ui 처리
                        handleSearchViewUi()
                    }
                    false -> {
                        Log.d(TAG, "서치뷰 닫힘")
                        binding.linearSearchHistoryView.visibility = View.INVISIBLE
                    }
                }
            }

            // 서치뷰에서 에딧텍스트를 가져온다.
            mySearchViewEditText = this.findViewById(androidx.appcompat.R.id.search_src_text)

//            // EditText 옵저버블 만들기
//            val editTextChangeObservable = mySearchViewEditText.textChanges()
//
//            val searchEditTextSubscription : Disposable =
//                // 옵저버블에 오퍼레이터 추가
//                editTextChangeObservable
//                    // 글자가 입력 되고 나서 0.8 초 후에 onNext 이벤트로 데이터 흘려보내기
//                    .debounce(1000, TimeUnit.MILLISECONDS)
//                    // IO 스레드에서 돌리겠다.
//                    // Scheduler instance intended for 10-bound work
//                    // 네트워크 요청, 파일 읽기, 쓰기, 디비처리 등
//                    .subscribeOn(Schedulers.io())
//                    // 구독을 통해 이벤트 응답 받기
//                    .subscribeBy(
//                        onNext = {
//                            Log.d("RX", "onNext : $it")
//                            // TODO:: 흘러들어온 이벤트 데이터로 api 호출
//                            if(it.isNotEmpty()) {
//                                searchPhotoApiCall(it.toString())
//                            }
//                        },
//                        onComplete = {
//                            Log.d("RX", "onComplete")
//                        },
//                        onError = {
//                            Log.d("RX", "onError : $it")
//                        }
//                    )
//            myCompositeDisposable.add(searchEditTextSubscription)

            // Rx의 스케줄러와 비슷
            // IO 스레드에서 돌리겠다
            GlobalScope.launch(context = myCoroutineContext) {
                // EditText가 변경 되었을 때
                val editTextFlow = mySearchViewEditText.textChangesToFlow()
                editTextFlow
                    // 입려되고 나서 2초 뒤에 받는다
                    .debounce(1000)
                    .filter {
                        it?.length!! > 0
                    }
                    .onEach {
                    Log.d(TAG, "flow로 받는다 $it")
                    }
                    .launchIn(this)
            }
        }

        this.mySearchViewEditText.apply {
            this.filters = arrayOf(InputFilter.LengthFilter(12))
            this.setTextColor(Color.WHITE)
            this.setHintTextColor(Color.WHITE)
        }

        return true
    }

    // 서치뷰 검색어 입력 이벤트
    // 검색버튼이 클릭 되었을 때
    override fun onQueryTextSubmit(query: String?): Boolean {

        Log.d(TAG, "PhotoCollectionActivity - onQueryTextSubmit() called / query : $query")

        if(!query.isNullOrEmpty()) {
            this.binding.topAppBar.title = query

            this.insertSearchTermHistory(query)
            this.searchPhotoApiCall(query)
        }

//        this.mySearchView.setQuery("", false)
//        this.mySearchView.clearFocus()
        this.binding.topAppBar.collapseActionView()

        return true
    }

    override fun onQueryTextChange(newText: String?): Boolean {

//        Log.d(TAG, "PhotoCollectionActivity - onQueryTextChange() called / newText: $newText")

        val userInputText = newText ?: ""

        if(userInputText.count() == 12) {
            Toast.makeText(this, "검색어는 12자까지만 가능합니다.", Toast.LENGTH_SHORT).show()
        }

//        if(userInputText.length in 1..12) {
//            searchPhotoApiCall(userInputText)
//        }

        return true
    }

    override fun onCheckedChanged(switch: CompoundButton?, isChecked: Boolean) {
        when(switch) {
            binding.searchHistoryModeSwitch -> {
                if(isChecked == true) {
                    Log.d(TAG, "검색어 저장기능 On")
                    SharedPrefManager.setSearchHistoryMode(isActivated = true)
                } else {
                    Log.d(TAG, "검색어 저장기능 Off")
                    SharedPrefManager.setSearchHistoryMode(isActivated = false)
                }
            }
        }
    }

    override fun onClick(view: View?) {
        when(view) {
            binding.clearSearchHistoryButton -> {
                Log.d(TAG, "검색 기록 삭제 버튼이 클릭 되었다.")
                SharedPrefManager.clearSearchHistoryList()
                this.searchHistoryList.clear()
                // ui 처리
                handleSearchViewUi()
            }
        }
    }

    // 검색 아이템삭제 버튼 이벤트
    override fun onSearchItemDeleteClicked(position: Int) {
        Log.d(TAG, "PhotoCollectionActivity - onSearchItemDeleteClicked() called / position $position")
        // 해당 요소 삭제
        this.searchHistoryList.removeAt(position)
        // 데이터 덮어쓰기
        SharedPrefManager.storeSearchHistoryList(this.searchHistoryList)
        // 데이터 변경 됨을 알려줌
        this.mySearchHistoryRecyclerViewAdapter.notifyDataSetChanged()
        // ui 처리
        handleSearchViewUi()
    }

    // 검색 아이템 버튼 이벤트
    override fun onSearchItemClicked(position: Int) {
        Log.d(TAG, "PhotoCollectionActivity - onSearchItemClicked() called / position: $position")

        var queryString = this.searchHistoryList[position].term
        searchPhotoApiCall(queryString)
        binding.topAppBar.title = queryString

        this.insertSearchTermHistory(searchTerm = queryString)

        this.binding.topAppBar.collapseActionView()
    }

    // 사진 검색 API 호출
    private fun searchPhotoApiCall(query: String) {
        RetrofitManager.instance.searchPhotos(searchTerm = query, completion = { status, list ->
            when(status) {
                RESPONSE_STATUS.OKAY -> {
                    Log.d(TAG, "PhotoCollectionActivity - searchPhotoApiCall() called 응답 성공 / list.size : ${list?.size}")
                    if(list != null) {
                        this.photoList.clear()
                        this.photoList = list
                        this.photoGridRecyclerViewAdapter.submitList(this.photoList)
                        this.photoGridRecyclerViewAdapter.notifyDataSetChanged()
                    }
                }
                RESPONSE_STATUS.NO_CONTENT -> {
                    Toast.makeText(this, "$query 에 대한 검색 결과가 없습니다.", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    private fun handleSearchViewUi() {
        Log.d(TAG, "PhotoCollectionActivity - handleSearchViewUi() called / size : ${this.searchHistoryList.size}")

        if(this.searchHistoryList.size > 0) {
            binding.linearSearchHistoryView.visibility = View.VISIBLE
            binding.searchHistoryRecyclerView.visibility = View.VISIBLE
            binding.searchHistoryRecyclerViewLabel.visibility = View.VISIBLE
            binding.clearSearchHistoryButton.visibility = View.VISIBLE
        } else {
            binding.linearSearchHistoryView.visibility = View.INVISIBLE
            binding.searchHistoryRecyclerView.visibility = View.INVISIBLE
            binding.searchHistoryRecyclerViewLabel.visibility = View.INVISIBLE
            binding.clearSearchHistoryButton.visibility = View.INVISIBLE
        }
    }

    // 검색어 저장
    private fun insertSearchTermHistory(searchTerm: String) {
        Log.d(TAG, "PhotoCollectionActivity - insertSearchTermHistory() called")

        if(SharedPrefManager.checkSearchHistoryMode() == true) {
            // 중복 아이템 삭제
            var indexListToRemove = ArrayList<Int>()

            this.searchHistoryList.forEachIndexed{ index, searchDataItem ->
                if(searchDataItem.term == searchTerm) {
                    Log.d(TAG, "index: $index")
                    indexListToRemove.add(index)
                }
            }
            indexListToRemove.forEach {
                this.searchHistoryList.removeAt(it)
            }

            // 새 아이템 넣기
            val newSearchData = SearchData(term = searchTerm, timestamp = Date().toSimpleString())
            this.searchHistoryList.add(newSearchData)

            // 기존 데이터 덮어쓰기
            SharedPrefManager.storeSearchHistoryList(this.searchHistoryList)

            this.mySearchHistoryRecyclerViewAdapter.notifyDataSetChanged()
        }
    }
}