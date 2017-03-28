package de.unibi.cebitec.bibigrid.util;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.AbstractMatcherFilter;
import ch.qos.logback.core.spi.FilterReply;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

public class ImportantInfoOutputFilter extends AbstractMatcherFilter<ILoggingEvent> {

    public static final Marker I = MarkerFactory.getMarker("IMPORTANT");
    @Override
    public FilterReply decide(ILoggingEvent event) {
        Marker evtMarker = event.getMarker();
        if (evtMarker == null) {
            return FilterReply.DENY;
        }
        if (!isStarted()) {
            return FilterReply.NEUTRAL;
        }
        if (evtMarker.contains(I)) {
            return FilterReply.ACCEPT;
        }
        
        return FilterReply.DENY;
    }
}
