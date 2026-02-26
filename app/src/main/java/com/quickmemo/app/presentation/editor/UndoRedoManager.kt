package com.quickmemo.app.presentation.editor

class UndoRedoManager(
    private val maxHistory: Int = 50,
) {
    private val undoStack = mutableListOf<String>()
    private val redoStack = mutableListOf<String>()
    private var isRestoring = false

    fun reset(initialSnapshot: String) {
        undoStack.clear()
        redoStack.clear()
        undoStack.add(initialSnapshot)
        isRestoring = false
    }

    fun save(snapshot: String) {
        if (isRestoring) return
        if (undoStack.lastOrNull() == snapshot) return

        undoStack.add(snapshot)
        if (undoStack.size > maxHistory) {
            undoStack.removeFirst()
        }
        redoStack.clear()
    }

    fun undo(): String? {
        if (undoStack.size <= 1) return null
        val current = undoStack.removeLast()
        redoStack.add(current)
        return undoStack.lastOrNull()
    }

    fun redo(): String? {
        if (redoStack.isEmpty()) return null
        val snapshot = redoStack.removeLast()
        undoStack.add(snapshot)
        return snapshot
    }

    fun canUndo(): Boolean = undoStack.size > 1

    fun canRedo(): Boolean = redoStack.isNotEmpty()

    fun setRestoring(value: Boolean) {
        isRestoring = value
    }
}
