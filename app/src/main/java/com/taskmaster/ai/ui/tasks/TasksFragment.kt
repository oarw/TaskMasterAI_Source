package com.taskmaster.ai.ui.tasks

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.taskmaster.ai.TaskMasterApplication
import com.taskmaster.ai.data.repository.CategoryRepository
import com.taskmaster.ai.data.repository.TaskRepository
import com.taskmaster.ai.databinding.FragmentTasksBinding

/**
 * 任务列表Fragment
 */
class TasksFragment : Fragment() {

    private var _binding: FragmentTasksBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: TasksViewModel by viewModels {
        val application = requireActivity().application as TaskMasterApplication
        val database = application.database
        TasksViewModel.TasksViewModelFactory(
            TaskRepository(database.taskDao()),
            CategoryRepository(database.categoryDao())
        )
    }
    
    private lateinit var tasksAdapter: TasksAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTasksBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupTabLayout()
        setupFab()
        observeViewModel()
    }
    
    private fun setupRecyclerView() {
        tasksAdapter = TasksAdapter(
            onTaskClick = { task ->
                val action = TasksFragmentDirections.actionTasksFragmentToTaskDetailFragment(task.id)
                findNavController().navigate(action)
            },
            onTaskCompleteClick = { task ->
                viewModel.toggleTaskCompleted(task)
            }
        )
        
        binding.tasksRecyclerView.apply {
            adapter = tasksAdapter
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(true)
        }
    }
    
    private fun setupTabLayout() {
        binding.tabLayout.addOnTabSelectedListener(object : com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> viewModel.setFilter(TasksViewModel.FILTER_ACTIVE)
                    1 -> viewModel.setFilter(TasksViewModel.FILTER_COMPLETED)
                }
            }
            
            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
            
            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
        })
    }
    
    private fun setupFab() {
        binding.fabAddTask.setOnClickListener {
            val action = TasksFragmentDirections.actionTasksFragmentToAddEditTaskFragment()
            findNavController().navigate(action)
        }
    }
    
    private fun observeViewModel() {
        viewModel.currentFilter.observe(viewLifecycleOwner) { filter ->
            when (filter) {
                TasksViewModel.FILTER_ALL -> viewModel.allTasks.observe(viewLifecycleOwner) { tasks ->
                    tasksAdapter.submitList(tasks)
                    updateEmptyView(tasks.isEmpty())
                }
                TasksViewModel.FILTER_ACTIVE -> viewModel.activeTasks.observe(viewLifecycleOwner) { tasks ->
                    tasksAdapter.submitList(tasks)
                    updateEmptyView(tasks.isEmpty())
                }
                TasksViewModel.FILTER_COMPLETED -> viewModel.completedTasks.observe(viewLifecycleOwner) { tasks ->
                    tasksAdapter.submitList(tasks)
                    updateEmptyView(tasks.isEmpty())
                }
            }
        }
        
        // 默认显示活跃任务
        viewModel.setFilter(TasksViewModel.FILTER_ACTIVE)
    }
    
    private fun updateEmptyView(isEmpty: Boolean) {
        binding.emptyView.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.tasksRecyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
