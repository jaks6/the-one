package applications.bpm;

import core.Application;
import core.DTNHost;
import core.SimClock;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.flowable.engine.common.api.delegate.event.*;
import org.flowable.engine.delegate.event.FlowableActivityEvent;
import org.flowable.engine.delegate.event.FlowableCancelledEvent;

import static report.BpmAppReporter.*;
import static report.FogReport.FOG_ACTIVITY_STARTED;


class BpmEventListener implements FlowableEventListener {

    private final Application application;
    private final DTNHost host;

    public BpmEventListener(DTNHost host, BpmEngineApplication bpmEngineApplication) {
        this.host = host;
        this.application = bpmEngineApplication;
    }

    @Override
    public void onEvent(FlowableEvent event) {
        FlowableEventType type = event.getType();
        FlowableEngineEvent engineEvent = (FlowableEngineEvent) event;

        Pair<Double, String> pair = new ImmutablePair<>( SimClock.getTime(), engineEvent.getProcessInstanceId());
        if (type.equals(FlowableEngineEventType.JOB_EXECUTION_SUCCESS)) {

        } else if (type.equals(FlowableEngineEventType.PROCESS_STARTED)){

            application.sendEventToListeners(PROCESS_STARTED, pair, host);
        } else if (type.equals(FlowableEngineEventType.PROCESS_COMPLETED)){
            application.sendEventToListeners(PROCESS_COMPLETED, pair, host);
        } else if (type.equals(FlowableEngineEventType.PROCESS_CANCELLED)){
            String cause = (String) ((FlowableCancelledEvent) event).getCause();
            if (cause.equals("cancelled"))
                application.sendEventToListeners(PROCESS_CANCELLED, pair, host);

        }

        else if (type.equals(FlowableEngineEventType.ACTIVITY_STARTED)){
            application.sendEventToListeners(ACTIVITY_STARTED, null, host);
            String activityId = ((FlowableActivityEvent) event).getActivityId();
            if (activityId.equals("offloadTask") ||  activityId.equals("localProcessingTask")) {
                application.sendEventToListeners(FOG_ACTIVITY_STARTED, pair, host);
            }

        } else if (type.equals(FlowableEngineEventType.ACTIVITY_COMPLETED)){
            application.sendEventToListeners(ACTIVITY_COMPLETED, null, host);
        } else if (type.equals(FlowableEngineEventType.ACTIVITY_CANCELLED)){
            application.sendEventToListeners(ACTIVITY_CANCELLED, null, host);

        }

        else if (type.equals(FlowableEngineEventType.TASK_CREATED)){
            application.sendEventToListeners(TASK_CREATED, null, host);
        } else if (type.equals(FlowableEngineEventType.TASK_COMPLETED)){
            application.sendEventToListeners(TASK_COMPLETED, null, host);
        }


        else if (type.equals(FlowableEngineEventType.ENTITY_CREATED)){
            application.sendEventToListeners(ENTITY_CREATED, null, host);
        }        else if (type.equals(FlowableEngineEventType.ENTITY_DELETED)){
            application.sendEventToListeners(ENTITY_DELETED, null, host);
        }

        //Not using engine message event, instead using simulator means for this
//        else if (type.equals(FlowableEngineEventType.ACTIVITY_MESSAGE_RECEIVED)) {
//            application.sendEventToListeners(RECEIVED_MESSAGE, null, host);
//        }
//        else {
//            System.out.println("Event received: " + event.getType());
//        }


    }

    @Override
    public boolean isFailOnException() {
        return false;
    }

    @Override
    public boolean isFireOnTransactionLifecycleEvent() {
        return false;
    }

    @Override
    public String getOnTransaction() {
        return null;
    }
}
