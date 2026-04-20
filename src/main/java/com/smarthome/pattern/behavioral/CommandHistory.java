package com.smarthome.pattern.behavioral;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * История команд для undo/redo.
 */
public class CommandHistory {

    private final Deque<DeviceCommand> undoStack = new ArrayDeque<>();
    private final Deque<DeviceCommand> redoStack = new ArrayDeque<>();
    private static final int MAX_HISTORY = 50;

    /** Выполнить команду и добавить в историю */
    public void executeCommand(DeviceCommand command) {
        command.execute();
        undoStack.push(command);
        redoStack.clear(); // После новой команды redo сбрасывается

        // Ограничиваем размер истории
        if (undoStack.size() > MAX_HISTORY) {
            // Удаляем самую старую команду (с конца)
            ((ArrayDeque<DeviceCommand>) undoStack).removeLast();
        }
    }

    /** Отменить последнюю команду */
    public boolean undo() {
        if (undoStack.isEmpty()) return false;
        DeviceCommand command = undoStack.pop();
        command.undo();
        redoStack.push(command);
        return true;
    }

    /** Повторить отменённую команду */
    public boolean redo() {
        if (redoStack.isEmpty()) return false;
        DeviceCommand command = redoStack.pop();
        command.execute();
        undoStack.push(command);
        return true;
    }

    public boolean canUndo() { return !undoStack.isEmpty(); }
    public boolean canRedo() { return !redoStack.isEmpty(); }

    /** Описание последней команды для отмены */
    public String getUndoDescription() {
        return undoStack.isEmpty() ? "" : undoStack.peek().getDescription();
    }

    /** Описание команды для повтора */
    public String getRedoDescription() {
        return redoStack.isEmpty() ? "" : redoStack.peek().getDescription();
    }

    public void clear() {
        undoStack.clear();
        redoStack.clear();
    }
}
