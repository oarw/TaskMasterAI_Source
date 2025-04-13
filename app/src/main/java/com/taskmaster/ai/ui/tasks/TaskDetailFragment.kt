package com.taskmaster.ai.ui.tasks

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.taskmaster.ai.TaskMasterApplication
import com.taskmaster.ai.data.Task
import com.taskmaster.ai.data.repository.TaskRepository
import com.taskmaster.ai.databinding.FragmentTaskDetailBinding
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * 任务详情Fragment
 */
class TaskDetailFragment : Fragment() {

    private var _binding: FragmentTaskDetailBinding? = null
    private val binding get() = _binding!!
    
    private val args: TaskDetailFragmentArgs by navArgs()
    
    private val viewModel: TaskDetailViewModel by viewModels {
        val application = requireActivity().application as TaskMasterApplication
        val database = application.database
        TaskDetailViewModel.TaskDetailViewModelFactory(
            TaskRepository(database.taskDao()),
            args.taskId
        )
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTaskDetailBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupFab()
        setupCompleteButton()
        observeViewModel()
    }
    
    private fun setupFab() {
        binding.fabEditTask.setOnClickListener {
            viewModel.task.value?.let { task ->
                val action = TaskDetailFragmentDirections.actionTaskDetailFragmentToAddEditTaskFragment(task.id)
                findNavController().navigate(action)
            }
        }
    }
    
    private fun setupCompleteButton() {
        binding.btnCompleteTask.setOnClickListener {
            viewModel.task.value?.let { task ->
                viewModel.toggleTaskCompleted(task)
                updateCompleteButtonText(task.isCompleted)
            }
        }
    }
    
    private fun observeViewModel() {
        viewModel.task.observe(viewLifecycleOwner) { task ->
            bindTaskToUi(task)
        }
    }
    
    private fun bindTaskToUi(task: Task) {
        binding.apply {
            taskTitle.text = task.title
            taskDescription.text = task.description.ifEmpty { "无描述" }
            
            // 设置优先级
            val priorityText = when (task.priority) {
                Task.PRIORITY_HIGH -> "高"
                Task.PRIORITY_NORMAL -> "中"
                Task.PRIORITY_LOW -> "低"
                else -> "中"
            }
            taskPriority.text = priorityText
            
            // 设置截止日期
            if (task.dueDate != null) {
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                taskDueDate.text = dateFormat.format(task.dueDate)
            } else {
                taskDueDate.text = "无截止日期"
            }
            
            // 设置分类
            // 注意：这里需要通过CategoryRepository获取分类名称
            // 简化起见，暂时显示分类ID
            taskCategory.text = if (task.categoryId > 0) "分类ID: ${task.categoryId}" else "无分类"
            
            // 更新完成按钮文本
            updateCompleteButtonText(task.isCompleted)
        }
    }
    
    private fun updateCompleteButtonText(isCompleted: Boolean) {
        binding.btnCompleteTask.text = if (isCompleted) "标记为未完成" else "标记为已完成"
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
