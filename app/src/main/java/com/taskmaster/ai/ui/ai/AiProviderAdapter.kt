package com.taskmaster.ai.ui.ai

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.taskmaster.ai.R
import com.taskmaster.ai.data.AiProvider

/**
 * AI提供商适配器
 */
class AiProviderAdapter(
    private val onEditClick: (AiProvider) -> Unit,
    private val onDeleteClick: (AiProvider) -> Unit,
    private val onSetDefaultClick: (AiProvider) -> Unit
) : ListAdapter<AiProvider, AiProviderAdapter.AiProviderViewHolder>(AiProviderDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AiProviderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ai_provider, parent, false)
        return AiProviderViewHolder(view)
    }

    override fun onBindViewHolder(holder: AiProviderViewHolder, position: Int) {
        val provider = getItem(position)
        holder.bind(provider, onEditClick, onDeleteClick, onSetDefaultClick)
    }

    class AiProviderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tv_provider_name)
        private val tvUrl: TextView = itemView.findViewById(R.id.tv_provider_url)
        private val tvDefaultBadge: TextView = itemView.findViewById(R.id.tv_default_badge)
        private val btnEdit: ImageButton = itemView.findViewById(R.id.btn_edit)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btn_delete)
        private val btnSetDefault: Button = itemView.findViewById(R.id.btn_set_default)

        fun bind(
            provider: AiProvider,
            onEditClick: (AiProvider) -> Unit,
            onDeleteClick: (AiProvider) -> Unit,
            onSetDefaultClick: (AiProvider) -> Unit
        ) {
            tvName.text = provider.name
            tvUrl.text = provider.apiUrl
            
            // 显示或隐藏默认标记
            tvDefaultBadge.visibility = if (provider.isDefault) View.VISIBLE else View.GONE
            
            // 设置默认按钮状态
            btnSetDefault.visibility = if (provider.isDefault) View.GONE else View.VISIBLE
            
            // 设置点击事件
            btnEdit.setOnClickListener { onEditClick(provider) }
            btnDelete.setOnClickListener { onDeleteClick(provider) }
            btnSetDefault.setOnClickListener { onSetDefaultClick(provider) }
        }
    }

    class AiProviderDiffCallback : DiffUtil.ItemCallback<AiProvider>() {
        override fun areItemsTheSame(oldItem: AiProvider, newItem: AiProvider): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: AiProvider, newItem: AiProvider): Boolean {
            return oldItem == newItem
        }
    }
}
