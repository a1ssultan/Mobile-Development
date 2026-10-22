package model

sealed class TaskStatus {
    data object Todo : TaskStatus()
    data object InProgress : TaskStatus()
    data object Done : TaskStatus()

    fun displayName(): String {
        return when (this) {
            Todo -> "TODO"
            InProgress -> "IN PROGRESS"
            Done -> "DONE"
        }
    }
}
