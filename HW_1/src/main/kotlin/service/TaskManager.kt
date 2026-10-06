package service

import kotlinx.coroutines.delay
import model.Task
import model.TaskStatus

class TaskManager {

    private val tasks = mutableListOf<Task>()

    fun addTask(task: Task) {
        tasks.add(task)
    }

    fun getAllTasks(): List<Task> {
        return tasks
    }

    fun showAllTasks() {
        println("\nAll tasks:")

        for (task in tasks) {
            val userName = task.assignedUser?.name ?: "Unassigned"

            println(
                "${task.id}. ${task.title} | " +
                    "${task.status.displayName()} | " +
                    userName
            )
        }
    }

    fun getActiveTasks(): List<Task> {
        return tasks.filter {
            it.status !is TaskStatus.Done
        }
    }

    fun getTaskTitles(): List<String> {
        return tasks.map {
            it.title
        }
    }

    fun getTotalEstimatedHours(): Int {
        if (tasks.isEmpty()) {
            return 0
        }

        return tasks
            .map { it.estimatedHours }
            .reduce { total, hours -> total + hours }
    }

    fun getUniqueCategories(): Set<String> {
        return tasks
            .map { it.category }
            .toSet()
    }

    fun getTasksByUser(): Map<String, List<Task>> {
        return tasks.groupBy {
            it.assignedUser?.name ?: "Unassigned"
        }
    }

    fun processTasks(action: (Task) -> Unit) {
        for (task in tasks) {
            action(task)
        }
    }

    suspend fun loadTasksFromServer() {
        println("\nLoading tasks from server...")
        delay(1500)
        println("Tasks loaded successfully.")
    }
}
