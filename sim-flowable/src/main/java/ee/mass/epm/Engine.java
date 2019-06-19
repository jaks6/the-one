package ee.mass.epm;

import ee.mass.epm.sim.SimulatedWorkQueue;
import ee.mass.epm.sim.message.EngineMessageContent;
import ee.mass.epm.sim.message.SimMessage;
import org.flowable.engine.*;
import org.flowable.engine.common.api.delegate.event.FlowableEvent;
import org.flowable.engine.common.api.delegate.event.FlowableEventListener;
import org.flowable.engine.common.api.delegate.event.FlowableEventType;
import org.flowable.engine.common.impl.runtime.Clock;
import org.flowable.engine.common.impl.history.HistoryLevel;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.*;

public class Engine implements SimulationApplicationEngine {


    public static final String LOCALHOST_VAR = "localhost";
    private int hostAddress;
    private ProcessEngine processEngine;
    private TaskService taskService;
    RepositoryService repositoryService;

    private SimulatedWorkQueue simulatedWorkQueue;
    private Queue<SimMessage> outgoingMessageQueue;
    private Set<FogMessageRequest> fogMessageRequests;
    RuntimeService runtimeService;


    Logger log = LoggerFactory.getLogger(this.getClass());


    public Engine(String uniqueID, Clock clock) {
        this.init(uniqueID, clock);
    }

    public Engine(String uniqueID) {
        this.init(uniqueID, null);
    }



    public void init(String uniqueID, Clock clock) {
        //TODO: verify db conf (in-memory needed?)
        ProcessEngineConfiguration cfg = new SimulatedProcessEngineConfiguration()
                .setJdbcUrl("jdbc:h2:mem:flowable"+uniqueID+";DB_CLOSE_DELAY=-1")
//                .setJdbcUsername("sa")
//                .setJdbcPassword("")
//                .setJdbcDriver("org.h2.Driver")
//                .setAsyncExecutorActivate(true)

                .setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE);

        if (clock != null){
            cfg.setClock(clock);
        }

        //TODO: consider ProcessEngineConfiguration.createStandaloneInMemProcessEngineConfiguration().buildProcessEngine();
        cfg.setHistory(HistoryLevel.NONE.getKey())
                .setCreateDiagramOnDeploy(false)
                .setEnableProcessDefinitionHistoryLevel(false);


        processEngine = cfg.buildProcessEngine();
        repositoryService = processEngine.getRepositoryService();
        taskService = processEngine.getTaskService();
        runtimeService = processEngine.getRuntimeService();

        simulatedWorkQueue = ((SimulatedProcessEngineConfiguration)cfg).simulatedWorkQueue;
        outgoingMessageQueue = ((SimulatedProcessEngineConfiguration)cfg).getOutgoingMessages();
        fogMessageRequests = ((SimulatedProcessEngineConfiguration)cfg).getFogMessageRequests();

    }

    @Override
    public Queue<SimMessage> getPendingOutgoingMessages() {
        return outgoingMessageQueue;
    }

    @Override
    public Set<FogMessageRequest> getFogMessageRequests() { return fogMessageRequests; }

    @Override
    public int getHostAddress() {
        return hostAddress;
    }

    @Override
    public void setHostAddress(int host){
        this.hostAddress = host;
    }

    @Override
    public ProcessInstance startProcessInstanceWithMessage(SimMessage msg) {
        Map<String, Object> processVariables = ((EngineMessageContent) msg.getContent()).variables;
        processVariables.put(LOCALHOST_VAR, hostAddress);
        return runtimeService.startProcessInstanceByMessage(msg.name, processVariables);
    }

    @Override
    public ProcessInstance startProcessInstance(String processKey) {
        return this.startProcessInstance(processKey, new HashMap<>());
    }

    @Override
    public ProcessInstance startProcessInstance(String processKey, Map<String, Object> processVariables) {
        processVariables.put(LOCALHOST_VAR, hostAddress);
        return runtimeService.startProcessInstanceByKey(processKey, processVariables);
    }

    public void event(String name){
        runtimeService.dispatchEvent(new FlowableEvent() {
            @Override
            public FlowableEventType getType() {
                return null;
            }
        });
    }

    @Override
    public void addEngineEventsListener(FlowableEventListener listener) {
        runtimeService.addEventListener(listener);
    }



    @Override
    public void message(String msgName, String executionId) {
        log.debug("received msgName = [" + msgName + "], destinationProcessInstanceId = [" + executionId + "]");
        runtimeService.messageEventReceived(msgName, executionId);
    }

    @Override
    public void message(String msgName, String executionId, Map<String, Object> processVariables) {
        log.debug("received msgName = [" + msgName + "], destinationProcessInstanceId = [" + executionId + "], processVariables = [" + processVariables + "]");
        if (doesExecutionExist(executionId))
            runtimeService.messageEventReceived(msgName, executionId, processVariables);
        else
            log.debug("A Message was dropped because the execution does not exist");
    }

    @Override
    public void message(SimMessage msg){
        EngineMessageContent msgContent = (EngineMessageContent) msg.getContent();

        Execution execution;
        if (msgContent.destinationProcessInstanceId != null){
            execution = getMessageExecutionForProcess(msg.name, msgContent.destinationProcessInstanceId);
        } else {
            execution =  getMessageListenerExecutionsByName(msg.name);
        }
        if (execution == null){
            log.warn("A message was dropped because the specified destinationExecutionId does not have a corresponding message subscription!");
        } else {
            runtimeService.messageEventReceived(msg.name, execution.getId(), msgContent.variables);
        }

//        if (doesExecutionExist(msgContent.destinationProcessInstanceId))
//            runtimeService.messageEventReceived(msg.name, msgContent.destinationProcessInstanceId, msgContent.variables);
//        else
//            log.debug("A Message was dropped because the execution does not exist");
    }

    private Execution getMessageListenerExecutionsByName(String name){
        return runtimeService.createExecutionQuery().messageEventSubscriptionName(name).singleResult();
    }

    private Execution getMessageExecutionForProcess(String name, String processId){
        return runtimeService.createExecutionQuery().messageEventSubscriptionName(name).rootProcessInstanceId(processId).singleResult();
    }

    private boolean doesExecutionExist(String executionId){
        return runtimeService.createExecutionQuery().executionId(executionId).count() > 0;
    }

    @Override
    public void signal(String name) {
        runtimeService.signalEventReceived(name);
    }

    @Override
    public void signal(String name, String executionId) { runtimeService.signalEventReceived(name, executionId);  }

    @Override
    public void signal(String name, Map<String, Object> processVariables) {
        runtimeService.signalEventReceived(name, processVariables);
    }

    @Override
    public void signal(String name, String executionId, Map<String, Object> processVariables) {
        runtimeService.signalEventReceived(name, executionId, processVariables);
    }


    /** Used to notify the original process that a send message task was succesfully received at the other end*/
    @Override
    public void notifyMessageTransferred(SimMessage msg) {
        if (msg.srcExecutionId == null) {
            log.warn("Tried to notify message transffered but srcExecutionId was null!");
            return;
        }
        if (runtimeService.createExecutionQuery().executionId(msg.srcExecutionId).count() > 0)
            runtimeService.trigger(msg.srcExecutionId);
        else
            log.debug("A Message transfer notification was dropped because the execution no longer exists");
    }


    @Override
    public void deploy(String name, String value){
        repositoryService.createDeployment().addString(name, value).deploy();
    }

    @Override
    public void deploy(String name, InputStream inputStream){
        repositoryService.createDeployment().addInputStream(name, inputStream).deploy();
    }

    public EngineStats getStats(){
        return EngineStats.create(this);
    }

    @Override
    public int getQueuedSimTasksSize(){
        return simulatedWorkQueue.getTimeToFinishJobs();
    }

    public List<ProcessInstance> getRunningInstances(){
        return runtimeService.createProcessInstanceQuery().list();
    }

    @Override
    public void activityCancelled(String executionId) {
        simulatedWorkQueue.cancelJob(executionId);
    }

    @Override
    public void cancelRunningInstances() {
        int count = 0;
        runtimeService.createProcessInstanceQuery().active().list()
                .forEach(i -> {
                    simulatedWorkQueue.removeJobs(i.getProcessInstanceId());
                    runtimeService.deleteProcessInstance(i.getProcessInstanceId(), "cancelled");
                });
    }

    @Override
    public void cancelRunningInstances(String reason) {
        int count = 0;
        runtimeService.createProcessInstanceQuery().active().list()
                .forEach(i -> {
                    simulatedWorkQueue.removeJobs(i.getProcessInstanceId());
                    runtimeService.deleteProcessInstance(i.getProcessInstanceId(), reason);
                });

    }

    /** Returns how much work was done during this update */
    @Override
    public int update() {

        ((SimulatedProcessEngineConfiguration)processEngine.getProcessEngineConfiguration()).doTimerUpdate();

        return simulatedWorkQueue.doWork();
    }

    public TaskService getTaskService() { return taskService; }

    public RepositoryService getRepositoryService() { return repositoryService; }
}
