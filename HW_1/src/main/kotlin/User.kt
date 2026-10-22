import model.Task

interface Assignable {
    fun assignTask(task: Task)
}

open class User(
    val id: Int,
    val name: String
) : Assignable {

    protected val assignedTasks = mutableListOf<Task>()

    override fun assignTask(task: Task) {
        assignedTasks.add(task)
        task.assignedUser = this
    }

    open fun showRole() {
        println("$name is a regular user")
    }

    fun showTasks() {
        println("Tasks assigned to $name:")

        if (assignedTasks.isEmpty()) {
            println("No tasks")
            return
        }

        for (task in assignedTasks) {
            println("- ${task.title} [${task.status.displayName()}]")
        }
    }
}

class Developer(
    id: Int,
    name: String,
    val programmingLanguage: String
) : User(id, name) {

    override fun showRole() {
        println("$name is a Developer working with $programmingLanguage")
    }
}

class Manager(
    id: Int,
    name: String
) : User(id, name) {

    override fun showRole() {
        println("$name is a Manager")
    }
}
