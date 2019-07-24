package ee.mass.epm.sim;

import core.Coord;
import ee.mass.epm.SimulatedProcessEngineConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;
import org.flowable.engine.impl.context.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LocationSignalExecutionListener implements ExecutionListener {

    /**
     *
     * Valid formats for coordinate are (whitespace is ignored):
     * 1) (123.0, 232.0) - this format is toString() of core.Coord of ONE sim
     * 2) 123.0, 232.0
     * 3) (123.0; 232.0)
     * 4) 123.0; 232.0
     */
    Expression coordinate;
    Logger log = LoggerFactory.getLogger(this.getClass());


    @Override
    public void notify(DelegateExecution execution) {
        SimulatedProcessEngineConfiguration engine = (SimulatedProcessEngineConfiguration) Context.getProcessEngineConfiguration();

        String coordString = (String) coordinate.getValue(execution);
        coordString = coordString.replaceAll("\\(","").replaceAll("\\)","");
        String[] strings = StringUtils.stripAll(coordString.split(","));

        if (strings.length != 2){
            strings = StringUtils.stripAll(coordString.split(";"));
        }

        if (strings.length != 2){
            log.error("Failed to parse Coord from field expression!");
        }

        Coord coordinate = new Coord(Double.valueOf(strings[0]), Double.valueOf(strings[1] ));

        engine.getLocationSignalSubscriptions().add(
                new LocationSignalSubscription(execution.getId(), coordinate));
    }
}
