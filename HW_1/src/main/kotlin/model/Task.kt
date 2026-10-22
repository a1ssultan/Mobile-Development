package model

import User

data class Task(
    val id: Int,
    val title: String,
    val category: String,
    val estimatedHours: Int,
    var status: TaskStatus,
    var assignedUser: User? = null
)
