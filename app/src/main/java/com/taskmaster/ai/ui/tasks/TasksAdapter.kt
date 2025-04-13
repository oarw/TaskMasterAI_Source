package com.taskmaster.ai.ui.tasks

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.taskmaster.ai.data.Task
import com.taskmaster.ai.databinding.ItemTaskBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 任务列表适配器
 */
class TasksAdapter(
    private val onTaskClick: (Task) -> Unit,
    private val onTaskCompleteClick: (Task) -> Unit
) : ListAdapter<Task, TasksAdapter.TaskViewHolder>(TaskDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding = ItemTaskBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = getItem(position)
        holder.bind(task)
    }

    inner class TaskViewHolder(private val binding: ItemTaskBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onTaskClick(getItem(position))
                }
            }

            binding.checkboxComplete.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onTaskCompleteClick(getItem(position))
                }
            }
        }

        fun bind(task: Task) {
            binding.apply {
                tvTaskTitle.text = task.title
                checkboxComplete.isChecked = task.isCompleted
                
                // 设置优先级标识
                when (task.priority) {
                    Task.PRIORITY_HIGH -> priorityIndicator.setBackgroundResource(android.R.color.holo_red_light)
                    Task.PRIORITY_NORMAL -> priorityIndicator.setBackgroundResource(android.R.color.holo_orange_light)
                    Task.PRIORITY_LOW -> priorityIndicator.setBackgroundResource(android.R.color.holo_green_light)
                }
                
                // 设置截止日期
                if (task.dueDate != null) {
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    tvDueDate.text = dateFormat.format(task.dueDate)
                } else {
                    tvDueDate.text = "无截止日期"
                }
            }
        }
    }

    class TaskDiffCallback : DiffUtil.ItemCallback<Task>() {
        override fun areItemsTheSame(oldItem: Task, newItem: Task): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Task, newItem: Task): Boolean {
            return oldItem == newItem
        }
    }
}
