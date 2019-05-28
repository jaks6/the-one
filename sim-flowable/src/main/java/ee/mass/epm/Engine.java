package ee.mass.epm;

import ee.mass.epm.sim.SimMessage;
import ee.mass.epm.sim.SimulatedWorkQueue;
import org.flowable.engine.*;
import org.flowable.engine.common.api.delegate.event.FlowableEvent;
import org.flowable.engine.common.api.delegate.event.FlowableEventListener;
import org.flowable.engine.common.api.delegate.event.FlowableEventType;
import org.flowable.engine.common.impl.history.HistoryLevel;
import org.flowable.engine.runtime.ProcessInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public class Engine implements SimulationApplicationEngine {


    private ProcessEngine processEngine;
    private TaskService taskService;
    RepositoryService repositoryService;

    private SimulatedWorkQueue simulatedWorkQueue;
    private Queue<SimMessage> outgoingMessageQueue;
    private Set<FogMessageRequest> fogMessageRequests;
    RuntimeService runtimeService;


    Logger log = LoggerFactory.getLogger(this.getClass());


    public Engine(String uniqueID) {
        this.init(uniqueID);
    }



    public void init(String uniqueID) {
        //TODO: verify db conf (in-memory needed?)
        ProcessEngineConfiguration cfg = new SimulatedProcessEngineConfiguration()
                .setJdbcUrl("jdbc:h2:mem:flowable"+uniqueID+";DB_CLOSE_DELAY=-1")
//                .setJdbcUsername("sa")
//                .setJdbcPassword("")
//                .setJdbcDriver("org.h2.Driver")
                .setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE);

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
    public ProcessInstance startProcessInstanceWithMessage(SimMessage msg) {
        return runtimeService.startProcessInstanceByMessage(msg.name, msg.variables);
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
    public ProcessInstance startProcessInstance(String processKey) {
        return runtimeService.startProcessInstanceByKey(processKey);
    }

    @Override
    public ProcessInstance startProcessInstance(String processKey, Map<String, Object> processVariables) {
        return runtimeService.startProcessInstanceByKey(processKey, processVariables);
    }

    @Override
    public void message(String msgName, String executionId) {
        log.debug("received msgName = [" + msgName + "], destinationExecutionid = [" + executionId + "]");
        runtimeService.messageEventReceived(msgName, executionId);
    }

    @Override
    public void message(String msgName, String executionId, Map<String, Object> processVariables) {
        log.debug("received msgName = [" + msgName + "], destinationExecutionid = [" + executionId + "], processVariables = [" + processVariables + "]");
        if (doesExecutionExist(executionId))
            runtimeService.messageEventReceived(msgName, executionId, processVariables);
        else
            log.debug("A Message was dropped because the execution does not exist");
    }

    @Override
    public void message(SimMessage msg){
        if (doesExecutionExist(msg.destinationExecutionid))
            runtimeService.messageEventReceived(msg.name, msg.destinationExecutionid, msg.variables);
        else
            log.debug("A Message was dropped because the execution does not exist");
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
    public void notifyMessageTransferred(SimMessage m) {
        if (runtimeService.createExecutionQuery().executionId(m.srcExecutionId).count() > 0)
            runtimeService.trigger(m.srcExecutionId);
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

    @Override
    public void deploy(String definitionResource) {
        if (repositoryService == null){
            log.error("repositoryService null!");
            return;
        }

        repositoryService.createDeployment().addClasspathResource(definitionResource).deploy();
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
        return simulatedWorkQueue.doWork();
    }

    public TaskService getTaskService() { return taskService; }

    public RepositoryService getRepositoryService() { return repositoryService; }
}
