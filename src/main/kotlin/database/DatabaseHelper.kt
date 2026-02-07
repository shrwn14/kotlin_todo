package database

import model.Todo
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet

class DatabaseHelper {
    private var connection: Connection? = null

    init {
        connect()
        createTable()
    }

    private fun connect() {
        try {
            // Create database in user's home directory
            val dbPath = System.getProperty("user.home") + "/.todo_app/todos.db"
            val dbDir = System.getProperty("user.home") + "/.todo_app"
            
            // Create directory if it doesn't exist
            java.io.File(dbDir).mkdirs()
            
            connection = DriverManager.getConnection("jdbc:sqlite:$dbPath")
            println("Connected to SQLite database at: $dbPath")
        } catch (e: Exception) {
            println("Database connection error: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun createTable() {
        val sql = """
            CREATE TABLE IF NOT EXISTS todos (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT NOT NULL,
                description TEXT NOT NULL,
                completed INTEGER NOT NULL DEFAULT 0,
                created_at INTEGER NOT NULL
            )
        """.trimIndent()

        try {
            connection?.createStatement()?.execute(sql)
            println("Todos table created or already exists")
        } catch (e: Exception) {
            println("Error creating table: ${e.message}")
            e.printStackTrace()
        }
    }

    fun insertTodo(title: String, description: String): Long {
        val sql = "INSERT INTO todos (title, description, completed, created_at) VALUES (?, ?, 0, ?)"
        
        return try {
            val stmt = connection?.prepareStatement(sql)
            stmt?.setString(1, title)
            stmt?.setString(2, description)
            stmt?.setLong(3, System.currentTimeMillis())
            stmt?.executeUpdate()
            
            // Get the last inserted ID
            val rs = connection?.createStatement()?.executeQuery("SELECT last_insert_rowid()")
            if (rs?.next() == true) {
                rs.getLong(1)
            } else {
                -1
            }
        } catch (e: Exception) {
            println("Error inserting todo: ${e.message}")
            e.printStackTrace()
            -1
        }
    }

    fun getAllTodos(
        limit: Int = 20, 
        offset: Int = 0, 
        searchQuery: String = "", 
        filter: TodoFilter = TodoFilter.ALL,
        sortBy: TodoSort = TodoSort.CREATED_DESC
    ): List<Todo> {
        val todos = mutableListOf<Todo>()
        val conditions = mutableListOf<String>()
        val params = mutableListOf<Any>()

        if (searchQuery.isNotBlank()) {
            conditions.add("(title LIKE ? OR description LIKE ?)")
            params.add("%$searchQuery%")
            params.add("%$searchQuery%")
        }

        when (filter) {
            TodoFilter.ACTIVE -> conditions.add("completed = 0")
            TodoFilter.COMPLETED -> conditions.add("completed = 1")
            TodoFilter.ALL -> {}
        }

        val whereClause = if (conditions.isNotEmpty()) {
            "WHERE " + conditions.joinToString(" AND ")
        } else {
            ""
        }

        val orderBy = when (sortBy) {
            TodoSort.CREATED_DESC -> "created_at DESC"
            TodoSort.CREATED_ASC -> "created_at ASC"
            TodoSort.TITLE_ASC -> "title COLLATE NOCASE ASC"
            TodoSort.TITLE_DESC -> "title COLLATE NOCASE DESC"
        }

        val sql = "SELECT * FROM todos $whereClause ORDER BY $orderBy LIMIT ? OFFSET ?"
        
        try {
            val stmt = connection?.prepareStatement(sql)
            var paramIndex = 1
            for (param in params) {
                when (param) {
                    is String -> stmt?.setString(paramIndex++, param)
                    is Int -> stmt?.setInt(paramIndex++, param)
                    is Long -> stmt?.setLong(paramIndex++, param)
                }
            }
            stmt?.setInt(paramIndex++, limit)
            stmt?.setInt(paramIndex++, offset)
            
            val rs = stmt?.executeQuery()
            
            while (rs?.next() == true) {
                todos.add(rs.toTodo())
            }
        } catch (e: Exception) {
            println("Error getting todos: ${e.message}")
            e.printStackTrace()
        }
        
        return todos
    }

    fun clearCompletedTodos(): Boolean {
        val sql = "DELETE FROM todos WHERE completed = 1"
        return try {
            val stmt = connection?.prepareStatement(sql)
            val rowsAffected = stmt?.executeUpdate() ?: 0
            rowsAffected > 0
        } catch (e: Exception) {
            println("Error clearing completed todos: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    fun updateTodo(id: Long, title: String, description: String, completed: Boolean): Boolean {
        val sql = "UPDATE todos SET title = ?, description = ?, completed = ? WHERE id = ?"
        
        return try {
            val stmt = connection?.prepareStatement(sql)
            stmt?.setString(1, title)
            stmt?.setString(2, description)
            stmt?.setInt(3, if (completed) 1 else 0)
            stmt?.setLong(4, id)
            val rowsAffected = stmt?.executeUpdate() ?: 0
            rowsAffected > 0
        } catch (e: Exception) {
            println("Error updating todo: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    fun toggleTodoComplete(id: Long, completed: Boolean): Boolean {
        val sql = "UPDATE todos SET completed = ? WHERE id = ?"
        
        return try {
            val stmt = connection?.prepareStatement(sql)
            stmt?.setInt(1, if (completed) 1 else 0)
            stmt?.setLong(2, id)
            val rowsAffected = stmt?.executeUpdate() ?: 0
            rowsAffected > 0
        } catch (e: Exception) {
            println("Error toggling todo: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    fun deleteTodo(id: Long): Boolean {
        val sql = "DELETE FROM todos WHERE id = ?"
        
        return try {
            val stmt = connection?.prepareStatement(sql)
            stmt?.setLong(1, id)
            val rowsAffected = stmt?.executeUpdate() ?: 0
            rowsAffected > 0
        } catch (e: Exception) {
            println("Error deleting todo: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    fun getTodoCount(): Int {
        val sql = "SELECT COUNT(*) FROM todos"
        
        return try {
            val rs = connection?.createStatement()?.executeQuery(sql)
            if (rs?.next() == true) {
                rs.getInt(1)
            } else {
                0
            }
        } catch (e: Exception) {
            println("Error getting todo count: ${e.message}")
            0
        }
    }

    private fun ResultSet.toTodo(): Todo {
        return Todo(
            id = getLong("id"),
            title = getString("title"),
            description = getString("description"),
            completed = getInt("completed") == 1,
            createdAt = getLong("created_at")
        )
    }

    fun close() {
        try {
            connection?.close()
            println("Database connection closed")
        } catch (e: Exception) {
            println("Error closing database: ${e.message}")
        }
    }
}

enum class TodoFilter {
    ALL, ACTIVE, COMPLETED
}

enum class TodoSort {
    CREATED_DESC, CREATED_ASC, TITLE_ASC, TITLE_DESC
}
