package de.unibi.cebitec.bibigrid.core.util;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.AbstractMatcherFilter;
import ch.qos.logback.core.spi.FilterReply;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

public class VerboseOutputFilter extends AbstractMatcherFilter<ILoggingEvent> {
    public static boolean SHOW_VERBOSE = false;
    public static final Marker V = MarkerFactory.getMarker("VERBOSE");

    @Override
    public FilterReply decide(ILoggingEvent event) {
        Marker evtMarker = event.getMarker();
        if (evtMarker == null) {
            return FilterReply.NEUTRAL;
        }
        if (!isStarted()) {
            return FilterReply.NEUTRAL;
        }
        if (evtMarker.contains(V) && !SHOW_VERBOSE) {
            return FilterReply.DENY;
        }
        if (evtMarker.contains(ImportantInfoOutputFilter.I)) {
            return FilterReply.DENY;
        }
        return FilterReply.NEUTRAL;
    }
}
