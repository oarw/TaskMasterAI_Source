package com.taskmaster.ai.ui.ai

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.taskmaster.ai.R
import com.taskmaster.ai.TaskMasterApplication
import com.taskmaster.ai.data.AiProvider
import com.taskmaster.ai.data.ai.AiServiceClient
import com.taskmaster.ai.data.repository.AiProviderRepository
import com.taskmaster.ai.data.repository.TaskRepository
import com.taskmaster.ai.databinding.FragmentAiTaskPlanningBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * AI任务规划Fragment
 */
class AiTaskPlanningFragment : Fragment() {

    private var _binding: FragmentAiTaskPlanningBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: AiTaskPlanningViewModel by viewModels {
        val application = requireActivity().application as TaskMasterApplication
        val database = application.database
        AiTaskPlanningViewModel.AiTaskPlanningViewModelFactory(
            AiProviderRepository(database.aiProviderDao()),
            TaskRepository(database.taskDao()),
            AiServiceClient()
        )
    }
    
    private lateinit var subtasksAdapter: SubtasksAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAiTaskPlanningBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupAiProviderSpinner()
        setupDueDateButton()
        setupActionButtons()
        setupSubtasksRecyclerView()
        observeViewModel()
    }
    
    private fun setupAiProviderSpinner() {
        viewModel.aiProviders.observe(viewLifecycleOwner) { providers ->
            if (providers.isEmpty()) {
                // 如果没有AI提供商，显示提示
                Toast.makeText(requireContext(), "请先添加AI提供商", Toast.LENGTH_LONG).show()
                return@observe
            }
            
            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                providers.map { it.name }
            )
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerAiProvider.adapter = adapter
            
            // 设置默认选中项
            viewModel.defaultAiProvider.value?.let { defaultProvider ->
                val position = providers.indexOfFirst { it.id == defaultProvider.id }
                if (position >= 0) {
                    binding.spinnerAiProvider.setSelection(position)
                }
            }
            
            binding.spinnerAiProvider.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    viewModel.setSelectedAiProvider(providers[position])
                }
                
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        }
    }
    
    private fun setupDueDateButton() {
        binding.btnDueDate.setOnClickListener {
            showDatePicker()
        }
    }
    
    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        
        // 如果已有日期，使用已有日期
        viewModel.taskDueDate.value?.let {
            calendar.time = it
        }
        
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        
        DatePickerDialog(requireContext(), { _, selectedYear, selectedMonth, selectedDay ->
            calendar.set(selectedYear, selectedMonth, selectedDay)
            val selectedDate = calendar.time
            viewModel.setTaskDueDate(selectedDate)
            
            // 更新按钮文本
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            binding.btnDueDate.text = dateFormat.format(selectedDate)
        }, year, month, day).show()
    }
    
    private fun setupActionButtons() {
        // 任务分解按钮
        binding.btnDecomposeTask.setOnClickListener {
            val title = binding.etTaskTitle.text.toString().trim()
            val description = binding.etTaskDescription.text.toString().trim()
            
            if (title.isEmpty()) {
                Toast.makeText(requireContext(), "请输入任务标题", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            viewModel.setTaskTitle(title)
            viewModel.setTaskDescription(description)
            viewModel.decomposeTask()
        }
        
        // 时间估算按钮
        binding.btnEstimateTime.setOnClickListener {
            val title = binding.etTaskTitle.text.toString().trim()
            val description = binding.etTaskDescription.text.toString().trim()
            
            if (title.isEmpty()) {
                Toast.makeText(requireContext(), "请输入任务标题", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            viewModel.setTaskTitle(title)
            viewModel.setTaskDescription(description)
            viewModel.estimateTime()
        }
        
        // 优先级建议按钮
        binding.btnSuggestPriority.setOnClickListener {
            val title = binding.etTaskTitle.text.toString().trim()
            val description = binding.etTaskDescription.text.toString().trim()
            
            if (title.isEmpty()) {
                Toast.makeText(requireContext(), "请输入任务标题", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            viewModel.setTaskTitle(title)
            viewModel.setTaskDescription(description)
            viewModel.suggestPriority()
        }
        
        // 创建任务按钮
        binding.btnCreateTasks.setOnClickListener {
            val mainTaskId = viewModel.createMainTask()
            
            if (mainTaskId != null) {
                viewModel.createSubtasks(mainTaskId)
                Toast.makeText(requireContext(), "任务创建成功", Toast.LENGTH_SHORT).show()
                
                // 导航回任务列表
                findNavController().navigateUp()
            } else {
                Toast.makeText(requireContext(), "任务创建失败", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun setupSubtasksRecyclerView() {
        subtasksAdapter = SubtasksAdapter()
        binding.rvSubtasks.apply {
            adapter = subtasksAdapter
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(true)
        }
    }
    
    private fun observeViewModel() {
        // 观察加载状态
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            
            // 禁用/启用按钮
            binding.btnDecomposeTask.isEnabled = !isLoading
            binding.btnEstimateTime.isEnabled = !isLoading
            binding.btnSuggestPriority.isEnabled = !isLoading
            binding.btnCreateTasks.isEnabled = !isLoading
        }
        
        // 观察错误消息
        viewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            if (message.isNotEmpty()) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
            }
        }
        
        // 观察子任务列表
        viewModel.subtasks.observe(viewLifecycleOwner) { subtasks ->
            if (subtasks.isNotEmpty()) {
                binding.cardSubtasks.visibility = View.VISIBLE
                subtasksAdapter.submitList(subtasks)
                updateCreateTasksButtonVisibility()
            }
        }
        
        // 观察估算时间
        viewModel.estimatedTime.observe(viewLifecycleOwner) { minutes ->
            if (minutes > 0) {
                binding.cardTimeEstimation.visibility = View.VISIBLE
                
                val hours = minutes / 60
                val remainingMinutes = minutes % 60
                
                val timeText = if (hours > 0) {
                    "预计完成时间：${hours}小时${remainingMinutes}分钟"
                } else {
                    "预计完成时间：${minutes}分钟"
                }
                
                binding.tvEstimatedTime.text = timeText
                updateCreateTasksButtonVisibility()
            }
        }
        
        // 观察建议优先级
        viewModel.suggestedPriority.observe(viewLifecycleOwner) { priority ->
            binding.cardPrioritySuggestion.visibility = View.VISIBLE
            
            val priorityText = when (priority) {
                0 -> "低"
                1 -> "中"
                2 -> "高"
                else -> "中"
            }
            
            binding.tvSuggestedPriority.text = "建议优先级：$priorityText"
            updateCreateTasksButtonVisibility()
        }
        
        // 观察优先级解释
        viewModel.priorityExplanation.observe(viewLifecycleOwner) { explanation ->
            if (explanation.isNotEmpty()) {
                binding.tvPriorityExplanation.text = "解释：$explanation"
            }
        }
    }
    
    private fun updateCreateTasksButtonVisibility() {
        // 当有子任务或者有估算时间或者有建议优先级时，显示创建任务按钮
        val hasSubtasks = viewModel.subtasks.value?.isNotEmpty() == true
        val hasEstimatedTime = viewModel.estimatedTime.value != null
        val hasSuggestedPriority = viewModel.suggestedPriority.value != null
        
        binding.btnCreateTasks.visibility = 
            if (hasSubtasks || hasEstimatedTime || hasSuggestedPriority) View.VISIBLE else View.GONE
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    /**
     * 子任务适配器
     */
    private class SubtasksAdapter : androidx.recyclerview.widget.ListAdapter<
            AiServiceClient.SubTask,
            SubtasksAdapter.SubtaskViewHolder
            >(SubtaskDiffCallback()) {
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubtaskViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            val view = inflater.inflate(android.R.layout.simple_list_item_2, parent, false)
            return SubtaskViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: SubtaskViewHolder, position: Int) {
            val subtask = getItem(position)
            holder.bind(subtask)
        }
        
        class SubtaskViewHolder(itemView: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView) {
            private val text1 = itemView.findViewById<android.widget.TextView>(android.R.id.text1)
            private val text2 = itemView.findViewById<android.widget.TextView>(android.R.id.text2)
            
            fun bind(subtask: AiServiceClient.SubTask) {
                text1.text = subtask.title
                
                val hours = subtask.estimatedMinutes / 60
                val minutes = subtask.estimatedMinutes % 60
                
                val timeText = if (hours > 0) {
                    "预计：${hours}小时${minutes}分钟 - ${subtask.description}"
                } else {
                    "预计：${minutes}分钟 - ${subtask.description}"
                }
                
                text2.text = timeText
            }
        }
        
        class SubtaskDiffCallback : androidx.recyclerview.widget.DiffUtil.ItemCallback<AiServiceClient.SubTask>() {
            override fun areItemsTheSame(oldItem: AiServiceClient.SubTask, newItem: AiServiceClient.SubTask): Boolean {
                return oldItem.title == newItem.title
            }
            
            override fun areContentsTheSame(oldItem: AiServiceClient.SubTask, newItem: AiServiceClient.SubTask): Boolean {
                return oldItem == newItem
            }
        }
    }
}
