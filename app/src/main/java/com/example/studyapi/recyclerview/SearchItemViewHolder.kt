package com.example.studyapi.recyclerview

import android.util.Log
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.example.studyapi.App
import com.example.studyapi.databinding.LayoutSearchItemBinding
import com.example.studyapi.model.SearchData
import com.example.studyapi.utils.Constants.TAG

class SearchItemViewHolder(itemView: View,
                           searchRecyclerViewInterface: ISearchHistoryRecyclerView)
                        : RecyclerView.ViewHolder(itemView),
                            View.OnClickListener
{
    var itemBinding = LayoutSearchItemBinding.bind(itemView)

    private lateinit var mySearchHistoryRecyclerViewInterface: ISearchHistoryRecyclerView

    // 뷰 가져오기
    private val searchItemTextView = itemBinding.searchTermText
    private val whenSearchedTextView = itemBinding.whenSearchedTextView
    private val deleteSearchBtn = itemBinding.deleteSearchBtn
    private val constraintSearchItem = itemBinding.constraintSearchItem

    init {
        Log.d(TAG, "SearchItemViewHolder - init() called")
        // 리스터 연결
        deleteSearchBtn.setOnClickListener(this)
        constraintSearchItem.setOnClickListener(this)
        mySearchHistoryRecyclerViewInterface = searchRecyclerViewInterface
    }

    // 데이터와 뷰를 묶는다.
    fun bindViewHolder(searchItem: SearchData) {
        Log.d(TAG, "SearchItemViewHolder - bindViewHolder() called")
        whenSearchedTextView.text = searchItem.timestamp
        searchItemTextView.text = searchItem.term
    }

    override fun onClick(view: View?) {
        Log.d(TAG, "SearchItemViewHolder - onClick() called")
        when(view) {
            deleteSearchBtn -> {
                Log.d(TAG, "SearchItemViewHolder - 검색 삭제 버튼 클릭")
                this.mySearchHistoryRecyclerViewInterface.onSearchItemDeleteClicked(adapterPosition)
            }
            constraintSearchItem -> {
                Log.d(TAG, "SearchItemViewHolder - 검색 아이템 클릭")
                this.mySearchHistoryRecyclerViewInterface.onSearchItemClicked(adapterPosition)
            }
        }
    }
}