package week_4_5_6_7;


/*
Features supported in this LLD
    Create a task
    Schedule it to run at a given time
    Support one-time and recurring tasks
    Execute tasks using a background scheduler thread
    Clean separation of responsibilities:
    Task
    Schedule
    TaskScheduler
    TaskExecutor
*/

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class TaskSchedulerDemo {
    public static void main(String[] args) throws InterruptedException {
        TaskScheduler scheduler = new TaskScheduler();

        // One time task
        scheduler.scheduleTask(
                new PrintTask("Send Welcome Email", "Welcome email sent"),
                new OneTimeSchedule(LocalDateTime.now().plusSeconds(5))
        );

        // Recurring task
        scheduler.scheduleTask(
                new PrintTask("Heartbeat Task", "Heartbeat running"),
                new RecurringSchedule(LocalDateTime.now().plusSeconds(2), Duration.ofSeconds(3))
        );

        // Another recurring task
        scheduler.scheduleTask(
                new PrintTask("Data Sync Task", "Syncing data to DB"),
                new RecurringSchedule(LocalDateTime.now().plusSeconds(4), Duration.ofSeconds(5))
        );

        Thread.sleep(20000);
        scheduler.shutdown();
    }
}

enum TaskStatus {
    SCHEDULED,
    RUNNING,
    COMPLETED,
    CANCELLED
}

/*
 * Task is the actual executable unit.
 * Any new task type can implement this interface.
 */
interface Task {
    String getName();
    void execute();
}

/*
 * Sample task implementation
 */
class PrintTask implements Task {
    private final String name;
    private final String message;

    public PrintTask(String name, String message) {
        this.name = name;
        this.message = message;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void execute() {
        System.out.println(message + " at " + LocalDateTime.now());
    }
}

/*
 * Schedule tells when the task should run
 */
interface Schedule {
    LocalDateTime getNextExecutionTime();
    void updateNextExecutionTime();
    boolean isRecurring();
}

class OneTimeSchedule implements Schedule {
    private final LocalDateTime executionTime;
    private boolean executed;

    public OneTimeSchedule(LocalDateTime executionTime) {
        this.executionTime = executionTime;
        this.executed = false;
    }

    @Override
    public LocalDateTime getNextExecutionTime() {
        return executed ? null : executionTime;
    }

    @Override
    public void updateNextExecutionTime() {
        executed = true;
    }

    @Override
    public boolean isRecurring() {
        return false;
    }
}

class RecurringSchedule implements Schedule {
    private LocalDateTime nextExecutionTime;
    private final Duration interval;

    public RecurringSchedule(LocalDateTime startTime, Duration interval) {
        this.nextExecutionTime = startTime;
        this.interval = interval;
    }

    @Override
    public LocalDateTime getNextExecutionTime() {
        return nextExecutionTime;
    }

    @Override
    public void updateNextExecutionTime() {
        nextExecutionTime = nextExecutionTime.plus(interval);
    }

    @Override
    public boolean isRecurring() {
        return true;
    }
}

/*
 * ScheduledTask = wrapper around
 * 1. actual Task
 * 2. schedule info
 * 3. scheduler metadata like id/status
 */

class ScheduledTask {
    private final int id;
    private final Task task;
    private final Schedule schedule;
    private TaskStatus status;

    public ScheduledTask(int id, Task task, Schedule schedule) {
        this.id = id;
        this.task = task;
        this.schedule = schedule;
        this.status = TaskStatus.SCHEDULED;
    }

    public int getId() {
        return id;
    }

    public Task getTask() {
        return task;
    }

    public Schedule getSchedule() {
        return schedule;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public LocalDateTime getNextExecutionTime() {
        return schedule.getNextExecutionTime();
    }
}

class TaskExecutor {
    public void execute(ScheduledTask scheduledTask) {
        try {
            scheduledTask.setStatus(TaskStatus.RUNNING);
            System.out.println("Executing task: " + scheduledTask.getTask().getName());
            scheduledTask.getTask().execute();
        } catch (Exception e) {
            System.out.println("Task failed: " + scheduledTask.getTask().getName());
        }
    }
}

class TaskScheduler {
    private final AtomicInteger taskIdGenerator = new AtomicInteger(1);
    private final Map<Integer, ScheduledTask> taskMap = new HashMap<>();
    private final PriorityQueue<ScheduledTask> taskQueue;
    private final TaskExecutor taskExecutor;
    private final Thread schedulerThread;
    private volatile boolean isRunning;

    public TaskScheduler() {
        this.taskQueue = new PriorityQueue<>(Comparator.comparing(ScheduledTask::getNextExecutionTime));
        this.taskExecutor = new TaskExecutor();
        this.isRunning = true;

        this.schedulerThread = new Thread(this::startSchedulerLoop);
        this.schedulerThread.start();
    }

    public int scheduleTask(Task task, Schedule schedule) {
        int taskId = taskIdGenerator.getAndIncrement();
        ScheduledTask scheduledTask = new ScheduledTask(taskId, task, schedule);

        taskMap.put(taskId, scheduledTask);
        taskQueue.offer(scheduledTask);

        System.out.println("Task scheduled: " + task.getName() + " at " + scheduledTask.getNextExecutionTime());
        return taskId;
    }

    public void cancelTask(int taskId) {
        ScheduledTask scheduledTask = taskMap.get(taskId);
        if (scheduledTask == null) {
            System.out.println("Task not found");
            return;
        }

        scheduledTask.setStatus(TaskStatus.CANCELLED);
        taskQueue.remove(scheduledTask);
        System.out.println("Task cancelled: " + scheduledTask.getTask().getName());
    }

    private void startSchedulerLoop() {
        while (isRunning) {
            try {
                ScheduledTask nextTask = taskQueue.peek();

                if (nextTask == null) {
                    Thread.sleep(500);
                    continue;
                }

                if (nextTask.getStatus() == TaskStatus.CANCELLED) {
                    taskQueue.poll();
                    continue;
                }

                LocalDateTime now = LocalDateTime.now();
                LocalDateTime nextExecutionTime = nextTask.getNextExecutionTime();

                if (nextExecutionTime == null) {
                    taskQueue.poll();
                    continue;
                }

                if (!nextExecutionTime.isAfter(now)) {
                    taskQueue.poll();
                    runTask(nextTask);
                } else {
                    Thread.sleep(500);
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void runTask(ScheduledTask scheduledTask) {
        if (scheduledTask.getStatus() == TaskStatus.CANCELLED) {
            return;
        }

        taskExecutor.execute(scheduledTask);

        if (scheduledTask.getSchedule().isRecurring() &&
                scheduledTask.getStatus() != TaskStatus.CANCELLED) {

            scheduledTask.getSchedule().updateNextExecutionTime();
            scheduledTask.setStatus(TaskStatus.SCHEDULED);
            taskQueue.offer(scheduledTask);
        } else {
            scheduledTask.getSchedule().updateNextExecutionTime();
            scheduledTask.setStatus(TaskStatus.COMPLETED);
        }
    }

    public void shutdown() {
        isRunning = false;
        schedulerThread.interrupt();
        System.out.println("Scheduler stopped");
    }
}