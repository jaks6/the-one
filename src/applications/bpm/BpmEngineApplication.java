package applications.bpm;

import core.*;
import ee.mass.epm.Engine;
import ee.mass.epm.SimulationApplicationEngine;
import ee.mass.epm.sim.SimMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import report.BpmAppReporter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;

import static routing.util.EnergyModel.ENERGY_VALUE_ID;

public class BpmEngineApplication extends Application {

    public static final String PROCESSVAR_DELIMITER = ":";
    public static final String MESSAGE_PROPERTY_OPERATION = "operation";
    private static final int ENERGY_MODIFIER = 1;
    public static final String MESSAGE_PROPERTY_TYPE = "type";
    private HashMap<String, Object>[] AUTOSTARTED_PROCESS_VARS;
    private String[] AUTO_STARTED_PROCESSES  = new String[]{};
    private String[] AUTO_DEPLOYED_PROCESSES = new String[]{};

    private static final String SETTING_AUTOSTARTED_PROCESSES = "autoStartedProcessKeys";

    // Should be in the form var1=var1Value&var2=var2Value ... etc (html url params - like)
    private static final String SETTING_AUTOSTARTED_PROCESSES_VARS = "autoStartedProcessVars";
    private static final String SETTING_AUTODEPLOYED_PROCESSES = "autoDeployedProcesses";



    public static int appIdCounter = 0; //TODO fix this dirty mess! currently used as workaround to force unique h2 database ids

    /** Application ID */
    public static final String APP_ID = "ee.ut.cs.mc.BpmApplication";
    private static final int OPERATION_DEPLOY = 10;
    private static final int OPERATION_START_PROCESS_BY_KEY = 11;
    public static final int OPERATION_PROCESS_MESSAGE = 12;

    public static final String PROPERTY_PROCESS_MSG = "process_message";
    public static final String SIGNAL_NEARBY_DEVICE = "Device Nearby";
    public static final String SIGNAL_DISCONNECTED = "Disconnected";
    public static final String SIGNAL_NEW_DESTINATION = "New Destination";

    public static final String MSG_TYPE_BPM = "bpm_msg";
    private static final String SETTING_OVERRIDE_MSG_SIZE = "overrideMsgSize";

    public static int OVERRIDE_MSG_SIZE = -1;

    Logger log = LoggerFactory.getLogger(this.getClass());

    SimulationApplicationEngine engine;
    private boolean firstUpdate = true;
    DTNHost mHost;

    @Override
    public Message handle(Message msg, DTNHost host) {
        log.debug("Handling Message");
        String type = (String) msg.getProperty(MESSAGE_PROPERTY_TYPE);
        if (!type.equals(MSG_TYPE_BPM)) return msg;
        else {
            return handleBpmMessage(msg, host);
        }
    }

    private Message handleBpmMessage(Message msg, DTNHost host) {
        if (msg.getTo() != host) return null; // are we the recipient?

        int operation = (Integer) msg.getProperty(MESSAGE_PROPERTY_OPERATION);
        switch (operation){
            case OPERATION_DEPLOY:
                handleOperationDeploy(msg);
                break;
            case OPERATION_START_PROCESS_BY_KEY:
                handleOperationStartProcessByKey(msg);
                break;
            case OPERATION_PROCESS_MESSAGE:
                super.sendEventToListeners(BpmAppReporter.RECEIVED_MESSAGE, null, host);
                handleOperationMessage(msg);
                break;
        }


        return null; //dont forward

    }

    private void handleOperationMessage(Message msg) {
        SimMessage simMsg = (SimMessage) msg.getProperty(PROPERTY_PROCESS_MSG);
        simMsg.variables.put("last_msg_source_address", msg.getFrom().getAddress());

        log.debug("[BPH-"+ mHost + "] Received : "+ simMsg);

        if (simMsg.isForStartEvent){
            engine.startProcessInstanceWithMessage(simMsg);
        } else {
            engine.message(simMsg);
        }
    }

    private void handleOperationStartProcessByKey(Message msg) {
        startProcessInstance((String) msg.getProperty("processKey"));
    }

    private void handleOperationDeploy(Message msg) {
        String name = (String) msg.getProperty("resourceName");
        byte[] bytes = (byte[]) msg.getProperty("bytes");
        engine.deploy(name, new ByteArrayInputStream(bytes));
    }

    public void handleSignal(String signalName, HashMap<String, Object> processVars) {
        log.debug(String.format("[BPH-%s] Signal: %s ( %s)", mHost, signalName, processVars.toString()));

        engine.signal(signalName, processVars);
        sendEventToListeners("signalNearbyDevice", null, mHost);
    }


    /** This constructor defines the prototype Engine instance,
     *  based on which copies are made with  the copy constructor
     * @param s
     */
    public BpmEngineApplication(Settings s) {
        log.info("BpmApp Proto");

        if (s.contains(SETTING_OVERRIDE_MSG_SIZE)){
            OVERRIDE_MSG_SIZE = s.getInt(SETTING_OVERRIDE_MSG_SIZE);
        }

        if (s.contains(SETTING_AUTODEPLOYED_PROCESSES))
            AUTO_DEPLOYED_PROCESSES = s.getCsvSetting(SETTING_AUTODEPLOYED_PROCESSES);
        if (s.contains(SETTING_AUTOSTARTED_PROCESSES)){
            AUTO_STARTED_PROCESSES = s.getCsvSetting(SETTING_AUTOSTARTED_PROCESSES);
            AUTOSTARTED_PROCESS_VARS = new HashMap[AUTO_STARTED_PROCESSES.length];
        }
        if (s.contains(SETTING_AUTOSTARTED_PROCESSES_VARS)){

            String[] processes = s.getCsvSetting(SETTING_AUTOSTARTED_PROCESSES_VARS);
            for (int i = 0; i < processes.length; i++) {
                String[] processVars = processes[i].split("&");
                HashMap<String, Object>  varMap = new HashMap<>();

                for (String processVar : processVars) {

                    String[] split = processVar.split("=");
                    String varName = split[0].trim();
                    String varValue = s.valueFillString(split[1].trim());
                    varMap.put(varName, varValue);
                    AUTOSTARTED_PROCESS_VARS[i] = varMap;
                }
            }
        }

        super.setAppID(APP_ID);
    }

    /**
     * Copy-constructor: this is the main used constructor which initializes the engine
     * for each node in the simulation. Although this is a copy constructor, each copy
     * creates a new engine instance.
     *
     * @param e
     */
    public BpmEngineApplication(BpmEngineApplication e){
        super(e);
        this.AUTO_DEPLOYED_PROCESSES = e.AUTO_DEPLOYED_PROCESSES;
        this.AUTO_STARTED_PROCESSES = e.AUTO_STARTED_PROCESSES;
        this.AUTOSTARTED_PROCESS_VARS = e.AUTOSTARTED_PROCESS_VARS;
        this.engine = new Engine(String.valueOf(BpmEngineApplication.appIdCounter));
        BpmEngineApplication.appIdCounter++;
    }



    @Override
    public void update(DTNHost host) {
        if (firstUpdate){ bootstrap(host); }
        else { //TODO: this means that autostarted processes and their tasks are ignored before bootstrap has been finished
            //make engine work on running tasks
            int workDone = engine.update();

            updateEnergy(host, workDone);
        }

        //take some messages and send them out
        handleOutgoingMessages(host);
    }

    private void updateEnergy(DTNHost host, int workDone) {
        if (workDone > 0){
            if (host.getComBus().containsProperty(ENERGY_VALUE_ID)){
                host.getComBus().updateDouble(ENERGY_VALUE_ID, -workDone * ENERGY_MODIFIER);
            } else {
                log.warn("Missed workDone Energy update!!");
            }
        }
    }

    void bootstrap(DTNHost host) {
        //initialize the singleton connection listener, so that bpm engines can get signals about new connections
        NewConnectionListener.getInstance(); //TODO: can we move it to prototype initialization?
        firstUpdate = false;
        mHost = host;

        SimScenario.getInstance().addMovementListener(new BpmMovementListener(this));
        SimScenario.getInstance().addMessageListener(new BpmMessageListener(host, engine));
        this.engine.addEngineEventsListener(new BpmEventListener(mHost, this));

        deployProcesses(host);

        handleAutostartProcesses();



        log.info("[BPH-" + host + "] initialized engine: "+ this.engine);
    }

    public void handleAutostartProcesses() {
        for (int i = 0; i < AUTO_STARTED_PROCESSES.length; i++) {
            HashMap<String, Object> processVariables = AUTOSTARTED_PROCESS_VARS[i];
            if (processVariables != null){
                engine.startProcessInstance(AUTO_STARTED_PROCESSES[i], processVariables);
            } else {
                engine.startProcessInstance(AUTO_STARTED_PROCESSES[i]);
            }
        }
    }

    private void deployProcesses(DTNHost host) {
        // TODO avoid re-parsing XML each time, parse once, get BpmnModel Java object and deploy that
        try {
            for (String processFile : AUTO_DEPLOYED_PROCESSES) {
                engine.deploy(processFile, Util.getCachedProcess(processFile));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleOutgoingMessages(DTNHost host) {
        for(Iterator<SimMessage> i = engine.getPendingOutgoingMessages().iterator(); i.hasNext();) {

            SimMessage simMessage = i.next();

            Message m = messageFromBpm(host, simMessage);
            host.createNewMessage(m);

            i.remove();
            // Call listeners
            this.sendEventToListeners(BpmAppReporter.SENT_MESSAGE, null, host);
        }
    }

    private Message messageFromBpm(DTNHost host, SimMessage simMessage) {
        DTNHost target =  SimScenario.getInstance().getWorld().getNodeByAddress(simMessage.destinationAddress);

        int size = (OVERRIDE_MSG_SIZE != -1) ? OVERRIDE_MSG_SIZE : simMessage.size;

        Message m = new Message(host, target, simMessage.name +
                SimClock.getIntTime() + "-" + host.getAddress(),
                size);

        m.addProperty("operation", OPERATION_PROCESS_MESSAGE);
        m.addProperty("type", MSG_TYPE_BPM);
        m.addProperty(PROPERTY_PROCESS_MSG, simMessage);
        m.setAppID(APP_ID);

        log.debug("[BPH-" + host + "] Sent: "+ simMessage );
        return m;
    }

    public void startProcessInstance(String processKey){
        this.engine.startProcessInstance(processKey);
//        super.sendEventToListeners(BpmAppReporter., null, mHost);
    }



    public int getSimulatedTaskQueueSize() {
        return engine.getQueuedSimTasksSize();
    }


    public void cancelRunningInstances() {
        engine.cancelRunningInstances();
    }
    public void cancelRunningInstances(String reason) {
        engine.cancelRunningInstances(reason);
    }

    @Override
    public Application replicate() {
        return new BpmEngineApplication(this);
    }


    public DTNHost getHost() {
        return mHost;
    }



}


