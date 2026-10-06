import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import model.Task
import model.TaskStatus
import service.TaskManager

fun main() = runBlocking {
    println("=== Kotlin Task Manager ===")

    val taskManager = TaskManager()

    val developer = Developer(
        id = 1,
        name = "Aisultan",
        programmingLanguage = "Kotlin"
    )

    val manager = Manager(
        id = 2,
        name = "Anna"
    )

    val users: List<User> = listOf(developer, manager)

    println("\nUsers:")
    for (user in users) {
        user.showRole()
    }

    val task1 = Task(
        id = 1,
        title = "Create login API",
        category = "Backend",
        estimatedHours = 5,
        status = TaskStatus.Todo
    )

    val task2 = Task(
        id = 2,
        title = "Fix database bug",
        category = "Backend",
        estimatedHours = 3,
        status = TaskStatus.InProgress
    )

    val task3 = Task(
        id = 3,
        title = "Prepare project report",
        category = "Documentation",
        estimatedHours = 2,
        status = TaskStatus.Done
    )

    val task4 = Task(
        id = 4,
        title = "Review pull request",
        category = "Code Review",
        estimatedHours = 1,
        status = TaskStatus.Todo
    )

    taskManager.addTask(task1)
    taskManager.addTask(task2)
    taskManager.addTask(task3)
    taskManager.addTask(task4)

    developer.assignTask(task1)
    developer.assignTask(task2)
    developer.assignTask(task4)
    manager.assignTask(task3)

    taskManager.showAllTasks()

    println("\nActive tasks:")
    val activeTasks = taskManager.getActiveTasks()
    activeTasks.forEach {
        println("- ${it.title}")
    }

    println("\nTask titles:")
    val titles = taskManager.getTaskTitles()
    println(titles)

    val totalHours = taskManager.getTotalEstimatedHours()
    println("\nTotal estimated hours: $totalHours")

    val categories = taskManager.getUniqueCategories()
    println("\nUnique categories:")
    for (category in categories) {
        println("- $category")
    }

    val tasksByUser = taskManager.getTasksByUser()
    println("\nTasks grouped by user:")
    for ((userName, tasks) in tasksByUser) {
        println("$userName:")
        for (task in tasks) {
            println("  - ${task.title}")
        }
    }

    println("\nUsing higher-order function:")
    taskManager.processTasks { task ->
        if (task.estimatedHours >= 3) {
            println("${task.title} is a large task")
        } else {
            println("${task.title} is a small task")
        }
    }

    println("\nDeveloper tasks:")
    developer.showTasks()

    println("\nCoroutine example:")
    val job = launch {
        taskManager.loadTasksFromServer()
    }

    println("Application continues working...")
    job.join()

    println("\nProgram finished.")
}
