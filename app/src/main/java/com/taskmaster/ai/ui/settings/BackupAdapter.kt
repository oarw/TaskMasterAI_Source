package com.taskmaster.ai.ui.settings

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
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 备份文件适配器
 */
class BackupAdapter(
    private val onRestoreClick: (File) -> Unit,
    private val onDeleteClick: (File) -> Unit
) : ListAdapter<File, BackupAdapter.BackupViewHolder>(BackupDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BackupViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_backup, parent, false)
        return BackupViewHolder(view)
    }

    override fun onBindViewHolder(holder: BackupViewHolder, position: Int) {
        val backupFile = getItem(position)
        holder.bind(backupFile, onRestoreClick, onDeleteClick)
    }

    class BackupViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvBackupName: TextView = itemView.findViewById(R.id.tv_backup_name)
        private val tvBackupDate: TextView = itemView.findViewById(R.id.tv_backup_date)
        private val tvBackupSize: TextView = itemView.findViewById(R.id.tv_backup_size)
        private val btnRestore: Button = itemView.findViewById(R.id.btn_restore)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btn_delete)

        fun bind(
            backupFile: File,
            onRestoreClick: (File) -> Unit,
            onDeleteClick: (File) -> Unit
        ) {
            tvBackupName.text = backupFile.name
            tvBackupDate.text = formatDate(Date(backupFile.lastModified()))
            tvBackupSize.text = formatFileSize(backupFile.length())
            
            btnRestore.setOnClickListener { onRestoreClick(backupFile) }
            btnDelete.setOnClickListener { onDeleteClick(backupFile) }
        }
        
        private fun formatDate(date: Date): String {
            val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            return format.format(date)
        }
        
        private fun formatFileSize(size: Long): String {
            val kb = size / 1024.0
            val mb = kb / 1024.0
            
            return when {
                mb >= 1.0 -> String.format("%.2f MB", mb)
                kb >= 1.0 -> String.format("%.2f KB", kb)
                else -> "$size 字节"
            }
        }
    }

    class BackupDiffCallback : DiffUtil.ItemCallback<File>() {
        override fun areItemsTheSame(oldItem: File, newItem: File): Boolean {
            return oldItem.absolutePath == newItem.absolutePath
        }

        override fun areContentsTheSame(oldItem: File, newItem: File): Boolean {
            return oldItem.lastModified() == newItem.lastModified() &&
                   oldItem.length() == newItem.length()
        }
    }
}
