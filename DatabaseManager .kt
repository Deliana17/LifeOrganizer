package com.dataapk.lifeorganizer

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

object DatabaseManager {
    private const val TAG = "DatabaseManager"
    private const val DATABASE_NAME = "LifeOrganizer.db"
    private const val DATABASE_VERSION = 1
    private const val TABLE_TASKS = "tasks"
    
    // Kolom tabel tasks
    private const val KEY_ID = "id"
    private const val KEY_TITLE = "title"
    private const val KEY_DESCRIPTION = "description"
    private const val KEY_DUE_DATE = "due_date"
    private const val KEY_PRIORITY = "priority"
    private const val KEY_COMPLETED = "completed"
    
    private var databaseHelper: DatabaseHelper? = null
    
    fun initialize(context: Context) {
        databaseHelper = DatabaseHelper(context)
    }
    
    private class DatabaseHelper(context: Context) : 
        SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
        
        override fun onCreate(db: SQLiteDatabase) {
            val CREATE_TASKS_TABLE = """
                CREATE TABLE $TABLE_TASKS (
                    $KEY_ID TEXT PRIMARY KEY,
                    $KEY_TITLE TEXT NOT NULL,
                    $KEY_DESCRIPTION TEXT,
                    $KEY_DUE_DATE INTEGER,
                    $KEY_PRIORITY INTEGER,
                    $KEY_COMPLETED INTEGER DEFAULT 0
                )
            """.trimIndent()
            db.execSQL(CREATE_TASKS_TABLE)
        }
        
        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            db.execSQL("DROP TABLE IF EXISTS $TABLE_TASKS")
            onCreate(db)
        }
    }
    
    fun syncTask(task: Task) {
        val db = databaseHelper?.writableDatabase ?: return
        
        val values = ContentValues().apply {
            put(KEY_ID, task.id)
            put(KEY_TITLE, task.title)
            put(KEY_DESCRIPTION, task.description)
            put(KEY_DUE_DATE, task.dueDate?.time)
            put(KEY_PRIORITY, task.priority)
            put(KEY_COMPLETED, if (task.completed) 1 else 0)
        }
        
        try {
            val rowsAffected = db.update(TABLE_TASKS, values, "$KEY_ID = ?", arrayOf(task.id))
            if (rowsAffected == 0) {
                db.insert(TABLE_TASKS, null, values)
            }
            Log.d(TAG, "Task synced: ${task.title}")
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing task", e)
        }
    }
    
    fun deleteTask(taskId: String) {
        val db = databaseHelper?.writableDatabase ?: return
        
        try {
            db.delete(TABLE_TASKS, "$KEY_ID = ?", arrayOf(taskId))
            Log.d(TAG, "Task deleted: $taskId")
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting task", e)
        }
    }
    
    fun getAllTasks(): List<Task> {
        val db = databaseHelper?.readableDatabase ?: return emptyList()
        val tasks = mutableListOf<Task>()
        
        val cursor = db.query(
            TABLE_TASKS,
            arrayOf(KEY_ID, KEY_TITLE, KEY_DESCRIPTION, KEY_DUE_DATE, KEY_PRIORITY, KEY_COMPLETED),
            null, null, null, null, null
        )
        
        try {
            while (cursor.moveToNext()) {
                val task = Task(
                    id = cursor.getString(0),
                    title = cursor.getString(1),
                    description = cursor.getString(2),
                    dueDate = if (cursor.isNull(3)) null else java.util.Date(cursor.getLong(3)),
                    priority = cursor.getInt(4),
                    completed = cursor.getInt(5) == 1
                )
                tasks.add(task)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting tasks", e)
        } finally {
            cursor.close()
        }
        
        return tasks
    }
    
    fun syncAllTasks(tasks: List<Task>) {
        val db = databaseHelper?.writableDatabase ?: return
        
        try {
            db.beginTransaction()
            tasks.forEach { task ->
                syncTask(task)
            }
            db.setTransactionSuccessful()
            Log.d(TAG, "Synced ${tasks.size} tasks")
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing all tasks", e)
        } finally {
            db.endTransaction()
        }
    }
    
    fun isDatabaseAvailable(): Boolean {
        return databaseHelper != null
    }
}
