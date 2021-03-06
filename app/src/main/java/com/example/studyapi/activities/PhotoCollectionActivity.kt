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

    // ?????????
    private var photoList = ArrayList<Photo>()

    // ?????? ?????? ??????
    private var searchHistoryList = ArrayList<SearchData>()

    // ?????????
    private lateinit var photoGridRecyclerViewAdapter: PhotoGridRecyclerViewAdapter
    private lateinit var mySearchHistoryRecyclerViewAdapter: SearchHistoryRecyclerViewAdapter
    
    // ?????????
    private lateinit var mySearchView: SearchView

    // ????????? ?????? ?????????
    private lateinit var mySearchViewEditText: EditText

//    // ???????????? ?????? ????????? ?????? CompositeDisposable
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

        // ?????????????????? ?????? ???????????? ???????????? ????????????.
       setSupportActionBar(binding.topAppBar)

       photoList = bundle?.getSerializable("photo_array_list") as ArrayList<Photo>

       // ?????? RecyclerView ??????
       this.photoCollectionRecyclerViewSetting(this.photoList)

       // ????????? ?????? ?????? ????????????
       this.searchHistoryList = SharedPrefManager.getSearchHistoryList() as ArrayList<SearchData>

       this.searchHistoryList.forEach {
           Log.d(TAG, "????????? ?????? ?????? - it.term : ${it.term}, it.timestamp ${it.timestamp}")
       }

       // ui ??????
       handleSearchViewUi()

       // ?????? ?????? RecyclerView ??????
       this.searchHistoryRecyclerViewSetting(this.searchHistoryList)

       if(searchTerm?.isNotEmpty() == true) {
           val term = searchTerm ?: ""
           this.insertSearchTermHistory(term)
       }

    } // onCreate

    override fun onDestroy() {
        Log.d(TAG, "PhotoCollectionActivity - onDestroy() called")
//        // ?????? ??????
//        this.myCompositeDisposable.clear()
        myCoroutineContext.cancel()

        super.onDestroy()
    }

    // ????????? ?????? RecyclerView ??????
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

    // ?????? ?????? RecyclerView ??????
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
            this.queryHint = "???????????? ??????????????????"

            this.setOnQueryTextListener(this@PhotoCollectionActivity)

            this.setOnQueryTextFocusChangeListener { _, hasExpaned ->
                when(hasExpaned) {
                    true -> {
                        Log.d(TAG, "????????? ??????")
//                        binding.linearSearchHistoryView.visibility = View.VISIBLE
                        // ui ??????
                        handleSearchViewUi()
                    }
                    false -> {
                        Log.d(TAG, "????????? ??????")
                        binding.linearSearchHistoryView.visibility = View.INVISIBLE
                    }
                }
            }

            // ??????????????? ?????????????????? ????????????.
            mySearchViewEditText = this.findViewById(androidx.appcompat.R.id.search_src_text)

//            // EditText ???????????? ?????????
//            val editTextChangeObservable = mySearchViewEditText.textChanges()
//
//            val searchEditTextSubscription : Disposable =
//                // ??????????????? ??????????????? ??????
//                editTextChangeObservable
//                    // ????????? ?????? ?????? ?????? 0.8 ??? ?????? onNext ???????????? ????????? ???????????????
//                    .debounce(1000, TimeUnit.MILLISECONDS)
//                    // IO ??????????????? ????????????.
//                    // Scheduler instance intended for 10-bound work
//                    // ???????????? ??????, ?????? ??????, ??????, ???????????? ???
//                    .subscribeOn(Schedulers.io())
//                    // ????????? ?????? ????????? ?????? ??????
//                    .subscribeBy(
//                        onNext = {
//                            Log.d("RX", "onNext : $it")
//                            // TODO:: ??????????????? ????????? ???????????? api ??????
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

            // Rx??? ??????????????? ??????
            // IO ??????????????? ????????????
            GlobalScope.launch(context = myCoroutineContext) {
                // EditText??? ?????? ????????? ???
                val editTextFlow = mySearchViewEditText.textChangesToFlow()
                editTextFlow
                    // ???????????? ?????? 2??? ?????? ?????????
                    .debounce(1000)
                    .filter {
                        it?.length!! > 0
                    }
                    .onEach {
                    Log.d(TAG, "flow??? ????????? $it")
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

    // ????????? ????????? ?????? ?????????
    // ??????????????? ?????? ????????? ???
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
            Toast.makeText(this, "???????????? 12???????????? ???????????????.", Toast.LENGTH_SHORT).show()
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
                    Log.d(TAG, "????????? ???????????? On")
                    SharedPrefManager.setSearchHistoryMode(isActivated = true)
                } else {
                    Log.d(TAG, "????????? ???????????? Off")
                    SharedPrefManager.setSearchHistoryMode(isActivated = false)
                }
            }
        }
    }

    override fun onClick(view: View?) {
        when(view) {
            binding.clearSearchHistoryButton -> {
                Log.d(TAG, "?????? ?????? ?????? ????????? ?????? ?????????.")
                SharedPrefManager.clearSearchHistoryList()
                this.searchHistoryList.clear()
                // ui ??????
                handleSearchViewUi()
            }
        }
    }

    // ?????? ??????????????? ?????? ?????????
    override fun onSearchItemDeleteClicked(position: Int) {
        Log.d(TAG, "PhotoCollectionActivity - onSearchItemDeleteClicked() called / position $position")
        // ?????? ?????? ??????
        this.searchHistoryList.removeAt(position)
        // ????????? ????????????
        SharedPrefManager.storeSearchHistoryList(this.searchHistoryList)
        // ????????? ?????? ?????? ?????????
        this.mySearchHistoryRecyclerViewAdapter.notifyDataSetChanged()
        // ui ??????
        handleSearchViewUi()
    }

    // ?????? ????????? ?????? ?????????
    override fun onSearchItemClicked(position: Int) {
        Log.d(TAG, "PhotoCollectionActivity - onSearchItemClicked() called / position: $position")

        var queryString = this.searchHistoryList[position].term
        searchPhotoApiCall(queryString)
        binding.topAppBar.title = queryString

        this.insertSearchTermHistory(searchTerm = queryString)

        this.binding.topAppBar.collapseActionView()
    }

    // ?????? ?????? API ??????
    private fun searchPhotoApiCall(query: String) {
        RetrofitManager.instance.searchPhotos(searchTerm = query, completion = { status, list ->
            when(status) {
                RESPONSE_STATUS.OKAY -> {
                    Log.d(TAG, "PhotoCollectionActivity - searchPhotoApiCall() called ?????? ?????? / list.size : ${list?.size}")
                    if(list != null) {
                        this.photoList.clear()
                        this.photoList = list
                        this.photoGridRecyclerViewAdapter.submitList(this.photoList)
                        this.photoGridRecyclerViewAdapter.notifyDataSetChanged()
                    }
                }
                RESPONSE_STATUS.NO_CONTENT -> {
                    Toast.makeText(this, "$query ??? ?????? ?????? ????????? ????????????.", Toast.LENGTH_SHORT).show()
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

    // ????????? ??????
    private fun insertSearchTermHistory(searchTerm: String) {
        Log.d(TAG, "PhotoCollectionActivity - insertSearchTermHistory() called")

        if(SharedPrefManager.checkSearchHistoryMode() == true) {
            // ?????? ????????? ??????
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

            // ??? ????????? ??????
            val newSearchData = SearchData(term = searchTerm, timestamp = Date().toSimpleString())
            this.searchHistoryList.add(newSearchData)

            // ?????? ????????? ????????????
            SharedPrefManager.storeSearchHistoryList(this.searchHistoryList)

            this.mySearchHistoryRecyclerViewAdapter.notifyDataSetChanged()
        }
    }
}