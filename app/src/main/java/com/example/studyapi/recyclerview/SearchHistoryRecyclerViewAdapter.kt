package com.example.studyapi.recyclerview

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.studyapi.R
import com.example.studyapi.model.SearchData
import com.example.studyapi.utils.Constants.TAG

class SearchHistoryRecyclerViewAdapter(searchHistoryRecyclerViewInterface: ISearchHistoryRecyclerView)
                                : RecyclerView.Adapter<SearchItemViewHolder>() {

    private var searchHistoryList: ArrayList<SearchData> = ArrayList()

    private var iSearchHistoryRecyclerView : ISearchHistoryRecyclerView? = null

    init {
        Log.d(TAG, "SearchHistoryRecyclerViewAdapter - init() called")
        this.iSearchHistoryRecyclerView = searchHistoryRecyclerViewInterface
    }

    // 뷰홀더가 메모리에 올라갔을 때
    // 뷰 홀더와 레이아웃을 연결 시켜준다.
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchItemViewHolder {
        return SearchItemViewHolder(
            LayoutInflater
                .from(parent.context)
                .inflate(R.layout.layout_search_item, parent, false)
        , this.iSearchHistoryRecyclerView!!
        )
    }

    override fun getItemCount(): Int {
        return this.searchHistoryList.size
    }

    override fun onBindViewHolder(holder: SearchItemViewHolder, position: Int) {

        val dataItem = this.searchHistoryList[position]
        holder.bindViewHolder(dataItem)

    }

    // 외부에서 어댑터에 데이터 배열을 넣는다.
    fun submitList(searchHistoryList: ArrayList<SearchData>) {
        this.searchHistoryList = searchHistoryList
    }
}