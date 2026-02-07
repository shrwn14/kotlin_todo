package ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import database.DatabaseHelper
import database.TodoFilter
import database.TodoSort
import kotlinx.coroutines.launch
import model.Todo
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MainScreen(database: DatabaseHelper) {
    var todos by remember { mutableStateOf(listOf<Todo>()) }
    var newTitle by remember { mutableStateOf("") }
    var newDescription by remember { mutableStateOf("") }
    var editingTodo by remember { mutableStateOf<Todo?>(null) }
    var editTitle by remember { mutableStateOf("") }
    var editDescription by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var currentFilter by remember { mutableStateOf(TodoFilter.ALL) }
    var currentSort by remember { mutableStateOf(TodoSort.CREATED_DESC) }
    var currentPage by remember { mutableStateOf(0) }
    var hasMore by remember { mutableStateOf(true) }
    var showSortMenu by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val pageSize = 20
    
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // Load initial todos or when search/filter/sort changes
    LaunchedEffect(searchQuery, currentFilter, currentSort) {
        todos = database.getAllTodos(pageSize, 0, searchQuery, currentFilter, currentSort)
        currentPage = 0
        hasMore = todos.size >= pageSize
    }

    // Infinite scroll detection
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastVisibleIndex ->
                if (lastVisibleIndex != null && lastVisibleIndex >= todos.size - 3 && hasMore) {
                    // Load more todos
                    val nextPage = currentPage + 1
                    val newTodos = database.getAllTodos(pageSize, nextPage * pageSize, searchQuery, currentFilter, currentSort)
                    if (newTodos.isNotEmpty()) {
                        todos = todos + newTodos
                        currentPage = nextPage
                        hasMore = newTodos.size >= pageSize
                    } else {
                        hasMore = false
                    }
                }
            }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1a1a2e),
                        Color(0xFF16213e)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            // Header
            Text(
                text = "✨ My Tasks",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Text(
                text = "${todos.size} task${if (todos.size != 1) "s" else ""}",
                fontSize = 14.sp,
                color = Color(0xFFa8b2d1),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Search and Clear Completed Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search tasks...", color = Color(0xFFa8b2d1)) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF00d4ff)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color(0xFFa8b2d1))
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF00d4ff),
                        unfocusedBorderColor = Color(0xFF533483),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    singleLine = true
                )

                if (todos.any { it.completed }) {
                    TextButton(
                        onClick = {
                            scope.launch {
                                if (database.clearCompletedTodos()) {
                                    todos = database.getAllTodos(pageSize, 0, searchQuery, currentFilter, currentSort)
                                    currentPage = 0
                                    hasMore = todos.size >= pageSize
                                    scope.launch { snackbarHostState.showSnackbar("Cleared completed tasks") }
                                } else {
                                    scope.launch { snackbarHostState.showSnackbar("Failed to clear tasks") }
                                }
                            }
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFff6b6b))
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Clear Done", fontSize = 12.sp)
                    }
                }
            }

            // Filter Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TodoFilter.values().forEach { filter ->
                    FilterChip(
                        selected = currentFilter == filter,
                        onClick = { currentFilter = filter },
                        label = { Text(filter.name.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }) },
                        enabled = true,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF00d4ff),
                            selectedLabelColor = Color(0xFF1a1a2e),
                            containerColor = Color(0xFF16213e),
                            labelColor = Color(0xFFa8b2d1)
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = currentFilter == filter,
                            borderColor = Color(0xFF533483),
                            selectedBorderColor = Color(0xFF00d4ff)
                        )
                    )
                }

                Spacer(Modifier.weight(1f))

                // Sort Button
                Box {
                    IconButton(
                        onClick = { showSortMenu = true },
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Color(0xFF16213e))
                    ) {
                        Icon(Icons.Default.Sort, contentDescription = "Sort", tint = Color(0xFF00d4ff))
                    }

                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        TodoSort.values().forEach { sort ->
                            DropdownMenuItem(
                                text = { 
                                    Text(
                                        text = sort.name.lowercase().replace("_", " ").replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() },
                                        color = if (currentSort == sort) Color(0xFF00d4ff) else Color.White
                                    ) 
                                },
                                onClick = {
                                    currentSort = sort
                                    showSortMenu = false
                                },
                                leadingIcon = {
                                    if (currentSort == sort) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF00d4ff), modifier = Modifier.size(18.dp))
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // Add Todo Form
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
                    .shadow(8.dp, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF0f3460)
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "Add New Task",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text("Title", color = Color(0xFFa8b2d1)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00d4ff),
                            unfocusedBorderColor = Color(0xFF533483),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = Color(0xFF00d4ff)
                        ),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = newDescription,
                        onValueChange = { newDescription = it },
                        label = { Text("Description", color = Color(0xFFa8b2d1)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .padding(bottom = 16.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00d4ff),
                            unfocusedBorderColor = Color(0xFF533483),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = Color(0xFF00d4ff)
                        ),
                        maxLines = 3
                    )

                    Button(
                        onClick = {
                            if (newTitle.isNotBlank()) {
                                scope.launch {
                                    val id = database.insertTodo(newTitle, newDescription)
                                    if (id > 0) {
                                        // Update local state if it matches current filter
                                        if (currentFilter != TodoFilter.COMPLETED) {
                                            todos = database.getAllTodos(pageSize, 0, searchQuery, currentFilter, currentSort)
                                            currentPage = 0
                                            hasMore = todos.size >= pageSize
                                        }
                                        newTitle = ""
                                        newDescription = ""
                                        scope.launch { snackbarHostState.showSnackbar("Task added successfully") }
                                    } else {
                                        scope.launch { snackbarHostState.showSnackbar("Failed to add task") }
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00d4ff)
                        ),
                        enabled = newTitle.isNotBlank()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add",
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = "Add Task",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1a1a2e)
                        )
                    }
                }
            }

            // Todo List
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(
                    items = todos,
                    key = { _, todo -> todo.id }
                ) { _, todo ->
                    TodoItem(
                        todo = todo,
                        onToggleComplete = { completed ->
                            scope.launch {
                                database.toggleTodoComplete(todo.id, completed)
                                // If filtering, we might need to remove it from list
                                if (currentFilter == TodoFilter.ACTIVE && completed ||
                                    currentFilter == TodoFilter.COMPLETED && !completed) {
                                    todos = todos.filter { it.id != todo.id }
                                } else {
                                    todos = todos.map {
                                        if (it.id == todo.id) it.copy(completed = completed) else it
                                    }
                                }
                            }
                        },
                        onEdit = {
                            editingTodo = todo
                            editTitle = todo.title
                            editDescription = todo.description
                        },
                        onDelete = {
                            scope.launch {
                                if (database.deleteTodo(todo.id)) {
                                    todos = todos.filter { it.id != todo.id }
                                    snackbarHostState.showSnackbar("Task deleted")
                                } else {
                                    snackbarHostState.showSnackbar("Failed to delete task")
                                }
                            }
                        }
                    )
                }

                // Empty State
                if (todos.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 48.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = if (searchQuery.isNotEmpty()) Icons.Default.SearchOff else Icons.Default.TaskAlt,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = Color(0xFF1e3a5f)
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = if (searchQuery.isNotEmpty()) "No tasks match your search" else "No tasks found here",
                                color = Color(0xFFa8b2d1),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Loading indicator
                if (hasMore) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = Color(0xFF00d4ff),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            }
        }

        // Edit Dialog
        editingTodo?.let { todo ->
            AlertDialog(
                onDismissRequest = { editingTodo = null },
                title = {
                    Text(
                        text = "Edit Task",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column {
                        OutlinedTextField(
                            value = editTitle,
                            onValueChange = { editTitle = it },
                            label = { Text("Title", color = Color(0xFFa8b2d1)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF00d4ff),
                                unfocusedBorderColor = Color(0xFF533483),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = Color(0xFF00d4ff)
                            ),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = editDescription,
                            onValueChange = { editDescription = it },
                            label = { Text("Description", color = Color(0xFFa8b2d1)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF00d4ff),
                                unfocusedBorderColor = Color(0xFF533483),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = Color(0xFF00d4ff)
                            ),
                            maxLines = 3
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            scope.launch {
                                if (database.updateTodo(todo.id, editTitle, editDescription, todo.completed)) {
                                    todos = todos.map {
                                        if (it.id == todo.id) {
                                            it.copy(title = editTitle, description = editDescription)
                                        } else it
                                    }
                                    editingTodo = null
                                    scope.launch { snackbarHostState.showSnackbar("Task updated") }
                                } else {
                                    scope.launch { snackbarHostState.showSnackbar("Failed to update task") }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00d4ff)
                        )
                    ) {
                        Text("Save", color = Color(0xFF1a1a2e))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { editingTodo = null }) {
                        Text("Cancel", color = Color(0xFFa8b2d1))
                    }
                },
                containerColor = Color(0xFF0f3460)
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp)
        )
    }
}

@Composable
fun TodoItem(
    todo: Todo,
    onToggleComplete: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(12.dp))
            .animateContentSize(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (todo.completed) Color(0xFF1e3a5f) else Color(0xFF0f3460)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Checkbox
            Checkbox(
                checked = todo.completed,
                onCheckedChange = onToggleComplete,
                colors = CheckboxDefaults.colors(
                    checkedColor = Color(0xFF00d4ff),
                    uncheckedColor = Color(0xFF533483),
                    checkmarkColor = Color(0xFF1a1a2e)
                ),
                modifier = Modifier.padding(end = 12.dp)
            )

            // Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
                Text(
                    text = todo.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (todo.completed) Color(0xFF64748b) else Color.White,
                    textDecoration = if (todo.completed) TextDecoration.LineThrough else null,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                if (todo.description.isNotBlank()) {
                    Text(
                        text = todo.description,
                        fontSize = 14.sp,
                        color = if (todo.completed) Color(0xFF475569) else Color(0xFFa8b2d1),
                        textDecoration = if (todo.completed) TextDecoration.LineThrough else null,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                Text(
                    text = formatDate(todo.createdAt),
                    fontSize = 12.sp,
                    color = Color(0xFF64748b)
                )
            }

            // Actions
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = Color(0xFF00d4ff),
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color(0xFFff6b6b),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = {
                Text(
                    text = "Delete Task",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete this task?",
                    color = Color(0xFFa8b2d1)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFff6b6b)
                    )
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel", color = Color(0xFFa8b2d1))
                }
            },
            containerColor = Color(0xFF0f3460)
        )
    }
}

fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
