package ee.mass.epm.sim;

public class JobHandle {
    private final int size;
    private final String executionId;
    private int workDone;

    public JobHandle(String id, int size) {
        this.executionId = id;
        this.workDone = 0;
        this.size = size;
    }

    public boolean isFinished() {
        return workDone >= size;
    }

    public void work() {
        workDone++;
    }

    public int getWorkDone() {
        return workDone;
    }

    public int getSize() {
        return size;
    }

    public String getExecutionId() {
        return executionId;
    }

    @Override
    public String toString() {
        return String.format("JobHandle %s [%s/%s]", executionId, workDone, size);
    }
}
