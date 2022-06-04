package com.example.studyapi.recyclerview

import android.util.Log
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.studyapi.App
import com.example.studyapi.R
import com.example.studyapi.databinding.LayoutPhotoItemBinding
import com.example.studyapi.model.Photo
import com.example.studyapi.utils.Constants.TAG


class PhotoItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    var itemBinding = LayoutPhotoItemBinding.bind(itemView)

    // 뷰를 가져온다.
    private val photoItemView = itemBinding.photoImage
    private val photoCreateAtText = itemBinding.createdAtText
    private val photoLikesCountText = itemBinding.likesCountText

    // 데이터와 뷰를 묶는다
    fun bindWithView(photoItem: Photo) {
        Log.d(TAG, "PhotoItemViewHolder - bindWithView() called")

        photoCreateAtText.text = photoItem.createdAt
        photoLikesCountText.text = photoItem.likesCount.toString()

        // 이미지 설정한다.
        Glide.with(App.instance)
            .load(photoItem.thumbnail)
            .placeholder(R.drawable.ic_baseline_insert_photo_24)
            .into(photoItemView)
    }
}