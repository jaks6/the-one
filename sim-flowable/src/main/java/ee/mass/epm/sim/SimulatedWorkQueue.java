package ee.mass.epm.sim;

import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.runtime.Execution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class SimulatedWorkQueue {

    private final int CONCURRENT_JOBS = 1;

    private final ProcessEngineConfiguration processEngineConfiguration;
    private final LinkedList<JobHandle> jobQueue;
    Queue<JobHandle> activeJobs = new LinkedList<>();

    Deque<JobHandle> registeredTasks;

    Logger log = LoggerFactory.getLogger(this.getClass());



    public SimulatedWorkQueue(ProcessEngineConfiguration engineConf) {
        this.processEngineConfiguration = engineConf;
        this.registeredTasks = new LinkedList<>();
        this.jobQueue = new LinkedList<>();
    }

    /** Estimate how many doWork() calls it will take to finish all currently present jobs */
    public int getTimeToFinishJobs(){
        return registeredTasks.stream().mapToInt(job -> job.getSize()).sum() +
                activeJobs.stream().mapToInt(job -> job.getSize() - job.getWorkDone()).sum();
    }

    public int doWork(){
        int workDone = 0;
        // is there work left?
        while ( CONCURRENT_JOBS > activeJobs.size() &&
                registeredTasks.size() > 0){
            activeJobs.add(registeredTasks.removeFirst());
        }

        Iterator<JobHandle> i = activeJobs.iterator();
        while (i.hasNext()) {
            JobHandle job = i.next();
            job.work();
            workDone++;
//            System.out.println("Did work on " + job);

            if (job.isFinished()){
//                System.out.println("Finishing " + job);
                i.remove();

                // send trigger so that execution can continue
                processEngineConfiguration.getRuntimeService().trigger(job.getExecutionId());
            }
        }
        return workDone;
    }

    public JobHandle addJob(String id, int size) {
        JobHandle jobHandle = new JobHandle(id, size);
        registeredTasks.add(jobHandle);
        return jobHandle;
    }

    public void removeJobs(String processInstanceId) {
        List<Execution> list = processEngineConfiguration.getRuntimeService().createExecutionQuery()
                .processInstanceId(processInstanceId).onlyChildExecutions().list();


        for (Execution execution : list) {
            JobHandle job;
            //Go through registeredTask list
            for( Iterator<JobHandle> i = registeredTasks.iterator(); i.hasNext();) {
                job = i.next();

                if (job.getExecutionId().equals(execution.getId())){
//                    System.out.println("Removed job of processInstanceId = [" + processInstanceId + "]");
                    i.remove();
                }

            }
            //Go through activeJobs list
            for( Iterator<JobHandle> j = activeJobs.iterator(); j.hasNext();) {
                job = j.next();
                if (job.getExecutionId().equals(execution.getId())){
//                    System.out.println("Removed job of processInstanceId = [" + processInstanceId + "]");
                    j.remove();
                }
            }
        }

    }
}
