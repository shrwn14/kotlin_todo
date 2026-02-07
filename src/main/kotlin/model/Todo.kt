package model

data class Todo(
    val id: Long = 0,
    val title: String,
    val description: String,
    val completed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
