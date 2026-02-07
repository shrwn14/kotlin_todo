# Kotlin Desktop Todo App

A modern, high-performance Todo application built with Compose Multiplatform and SQLite.

## ✨ Features

- **Real-time Search**: Find tasks instantly by title or description.
- **Smart Filtering**: Filter by All, Active, or Completed status.
- **Advanced Sorting**: Sort by creation date or title (A-Z/Z-A).
- **Infinite Scrolling**: Handles large numbers of tasks smoothly.
- **SQLite Persistence**: Data is stored locally in `~/.todo_app/todos.db`.
- **Modern UI**: Dark mode, gradients, micro-animations, and Snackbar feedback.

## 🚀 Getting Started

### Prerequisites

- **Java Development Kit (JDK) 17 or higher**
- **IntelliJ IDEA** (Recommended)

### Opening in IntelliJ IDEA

1. Open IntelliJ IDEA.
2. Select **Open** and navigate to this directory.
3. Wait for IntelliJ to import the Gradle project.
4. If prompted, trust the project.

### Running the Application

You can run the application directly from IntelliJ or via the terminal:

#### Using IntelliJ IDEA
- Open `src/main/kotlin/Main.kt`.
- Click the green "Run" icon next to the `fun main()` function.

#### Using Terminal
```bash
./gradlew run
```

## 🛠 Tech Stack

- **UI**: [Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/)
- **Language**: [Kotlin](https://kotlinlang.org/)
- **Database**: [SQLite](https://www.sqlite.org/) with JDBC
- **Concurrency**: [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html)
- **Styling**: Material 3 with Custom CSS-like brush gradients.

---


