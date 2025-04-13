package com.taskmaster.ai.ui.tasks

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.taskmaster.ai.R
import com.taskmaster.ai.TaskMasterApplication
import com.taskmaster.ai.data.Category
import com.taskmaster.ai.data.Task
import com.taskmaster.ai.data.repository.CategoryRepository
import com.taskmaster.ai.data.repository.TaskRepository
import com.taskmaster.ai.databinding.FragmentAddEditTaskBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * 添加/编辑任务Fragment
 */
class AddEditTaskFragment : Fragment() {

    private var _binding: FragmentAddEditTaskBinding? = null
    private val binding get() = _binding!!
    
    private val args: AddEditTaskFragmentArgs by navArgs()
    
    private val viewModel: AddEditTaskViewModel by viewModels {
        val application = requireActivity().application as TaskMasterApplication
        val database = application.database
        AddEditTaskViewModel.AddEditTaskViewModelFactory(
            TaskRepository(database.taskDao()),
            CategoryRepository(database.categoryDao()),
            args.taskId
        )
    }
    
    private var selectedDate: Date? = null
    private var categories: List<Category> = emptyList()
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddEditTaskBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupPriorityRadioGroup()
        setupDueDateButton()
        setupCategorySpinner()
        setupSaveButton()
        observeViewModel()
    }
    
    private fun setupPriorityRadioGroup() {
        binding.rgPriority.setOnCheckedChangeListener { _, checkedId ->
            val priority = when (checkedId) {
                R.id.rb_priority_low -> Task.PRIORITY_LOW
                R.id.rb_priority_medium -> Task.PRIORITY_NORMAL
                R.id.rb_priority_high -> Task.PRIORITY_HIGH
                else -> Task.PRIORITY_NORMAL
            }
            viewModel.setTaskPriority(priority)
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
        selectedDate?.let {
            calendar.time = it
        }
        
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        
        DatePickerDialog(requireContext(), { _, selectedYear, selectedMonth, selectedDay ->
            calendar.set(selectedYear, selectedMonth, selectedDay)
            selectedDate = calendar.time
            viewModel.setTaskDueDate(selectedDate)
            
            // 更新按钮文本
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            binding.btnDueDate.text = dateFormat.format(selectedDate!!)
        }, year, month, day).show()
    }
    
    private fun setupCategorySpinner() {
        viewModel.categories.observe(viewLifecycleOwner) { categoryList ->
            categories = categoryList
            
            // 创建分类名称列表，添加"无分类"选项
            val categoryNames = mutableListOf("无分类")
            categoryNames.addAll(categoryList.map { it.name })
            
            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                categoryNames
            )
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerCategory.adapter = adapter
            
            // 设置选中项
            viewModel.taskCategoryId.value?.let { categoryId ->
                val position = if (categoryId == 0L) {
                    0 // "无分类"
                } else {
                    categoryList.indexOfFirst { it.id == categoryId }.let {
                        if (it >= 0) it + 1 else 0 // +1 是因为第一项是"无分类"
                    }
                }
                binding.spinnerCategory.setSelection(position)
            }
            
            // 设置选择监听器
            binding.spinnerCategory.setOnItemSelectedListener { position ->
                val categoryId = if (position == 0) {
                    0L // "无分类"
                } else {
                    categories[position - 1].id // -1 是因为第一项是"无分类"
                }
                viewModel.setTaskCategory(categoryId)
            }
        }
    }
    
    private fun setupSaveButton() {
        binding.btnSaveTask.setOnClickListener {
            val title = binding.etTaskTitle.text.toString().trim()
            if (title.isEmpty()) {
                Toast.makeText(requireContext(), "请输入任务标题", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            viewModel.setTaskTitle(title)
            viewModel.setTaskDescription(binding.etTaskDescription.text.toString().trim())
            viewModel.saveTask()
            
            findNavController().navigateUp()
        }
    }
    
    private fun observeViewModel() {
        // 如果是编辑模式，加载现有任务数据
        if (args.taskId != -1L) {
            viewModel.task?.observe(viewLifecycleOwner) { task ->
                task?.let { bindTaskToUi(it) }
            }
        }
    }
    
    private fun bindTaskToUi(task: Task) {
        binding.apply {
            etTaskTitle.setText(task.title)
            etTaskDescription.setText(task.description)
            
            // 设置优先级
            when (task.priority) {
                Task.PRIORITY_LOW -> rbPriorityLow.isChecked = true
                Task.PRIORITY_NORMAL -> rbPriorityMedium.isChecked = true
                Task.PRIORITY_HIGH -> rbPriorityHigh.isChecked = true
            }
            
            // 设置截止日期
            task.dueDate?.let { date ->
                selectedDate = date
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                btnDueDate.text = dateFormat.format(date)
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    // 扩展函数，简化Spinner的选择监听
    private fun android.widget.Spinner.setOnItemSelectedListener(onItemSelected: (position: Int) -> Unit) {
        this.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                onItemSelected(position)
            }
            
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }
    }
}
