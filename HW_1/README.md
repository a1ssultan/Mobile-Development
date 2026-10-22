# Kotlin Task Manager

A small console Task Manager application written in Kotlin/JVM.

The project demonstrates Kotlin fundamentals, object-oriented programming, collections, higher-order functions, lambdas, sealed classes, and coroutines.

## How to run

### IntelliJ IDEA

1. Open IntelliJ IDEA.
2. Choose **Open** and select the `HW_1` folder.
3. Wait until Gradle finishes syncing.
4. Open `src/main/kotlin/Main.kt`.
5. Run the `main()` function.

### Terminal

From the `HW_1` folder, run:

```bash
./gradlew run
```

## Main features

The application creates users and tasks, assigns tasks to users, filters active tasks, calculates total estimated time, groups tasks by users, uses higher-order functions, and demonstrates a simple asynchronous operation with Kotlin coroutines.

## Homework requirements

- Variables and data types: user and task properties such as `name`, `title`, and `estimatedHours`.
- Conditions: checking task size and task status.
- Loops: displaying users, tasks, and categories.
- List: users and tasks.
- Set: unique task categories.
- Map: tasks grouped by user.
- `map`: converting tasks to task titles and estimated hours.
- `filter`: getting only active tasks.
- `reduce`: calculating total estimated hours.
- Functions: methods inside `TaskManager` and `User`.
- Higher-order function: `processTasks(action: (Task) -> Unit)`.
- Lambda: passed to `processTasks` and collection operations.
- Classes and objects: `User`, `Developer`, `Manager`, `TaskManager`, and `Task`.
- Inheritance: `Developer` and `Manager` inherit from `User`.
- Interface: `Assignable`.
- Polymorphism: `List<User>` contains both `Developer` and `Manager`, and `showRole()` behaves differently.
- Data class: `Task`.
- Sealed class: `TaskStatus`.
- Suspend function: `loadTasksFromServer()`.
- Coroutine: `runBlocking`, `launch`, `delay`, and `join`.

## Author

Aisultan Khalelov