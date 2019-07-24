package ee.mass.epm;

import ee.mass.epm.sim.SimulatedWorkQueue;
import ee.mass.epm.sim.message.SimMessage;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.engine.impl.bpmn.parser.handler.ServiceTaskParseHandler;
import org.flowable.engine.impl.cfg.StandaloneProcessEngineConfiguration;
import org.flowable.engine.parse.BpmnParseHandler;
import org.flowable.job.service.impl.asyncexecutor.AcquiredTimerJobEntities;
import org.flowable.job.service.impl.cmd.AcquireTimerJobsCmd;
import org.flowable.job.service.impl.persistence.entity.JobEntity;
import org.flowable.job.service.impl.persistence.entity.TimerJobEntity;

import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

public class SimulatedProcessEngineConfiguration extends StandaloneProcessEngineConfiguration {

    public SimulatedProcessEngineConfiguration() {
        super();
        simulatedWorkQueue =  new SimulatedWorkQueue(this);

        outgoingMessages = new ConcurrentLinkedQueue<>();
        fogMessageRequests = new HashSet<>();

        setAsyncExecutor(new OneAsyncExecutor());

    }

    void doTimerUpdate() {
        final AcquiredTimerJobEntities acquiredJobs = commandExecutor.execute(
                new AcquireTimerJobsCmd(asyncExecutor));

        commandExecutor.execute((Command<Void>) commandContext -> {

            for (TimerJobEntity job : acquiredJobs.getJobs()) {

                JobEntity jobEntity = asyncExecutor.getJobServiceConfiguration().getJobManager().moveTimerJobToExecutableJob(job);
                asyncExecutor.getJobServiceConfiguration().getJobManager().execute(jobEntity);
            }
            return null;
        });

    }

    @Override
    public List<BpmnParseHandler> getDefaultBpmnParseHandlers() {
        List<BpmnParseHandler> defaultBpmnParseHandlers = super.getDefaultBpmnParseHandlers();
        for (int i = 0; i < defaultBpmnParseHandlers.size(); i++) {
            if (defaultBpmnParseHandlers.get(i).getClass().equals(ServiceTaskParseHandler.class)){
                defaultBpmnParseHandlers.set(i, new StepONEServiceTaskParseHandler());
                break;
            }
        }
        return defaultBpmnParseHandlers;
    }

    protected SimulatedWorkQueue simulatedWorkQueue;
    private Queue<SimMessage> outgoingMessages;



    private Set<FogMessageRequest> fogMessageRequests;

    public SimulatedWorkQueue getSimulatedWorkQueue() {
        return simulatedWorkQueue;
    }

    public Queue<SimMessage> getOutgoingMessages() {
        return outgoingMessages;
    }

    public Set<FogMessageRequest> getFogMessageRequests() { return fogMessageRequests; }
}
