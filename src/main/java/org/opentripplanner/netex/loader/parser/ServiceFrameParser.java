package org.opentripplanner.netex.loader.parser;

import org.opentripplanner.netex.loader.NetexEntityDataIndex;
import org.opentripplanner.netex.loader.util.ReadOnlyHierarchicalMapById;
import org.opentripplanner.netex.loader.util.ReadOnlyHierarchicalVersionMapById;
import org.opentripplanner.util.OTPFeature;
import org.rutebanken.netex.model.DestinationDisplay;
import org.rutebanken.netex.model.DestinationDisplaysInFrame_RelStructure;
import org.rutebanken.netex.model.FlexibleLine;
import org.rutebanken.netex.model.FlexibleStopAssignment;
import org.rutebanken.netex.model.FlexibleStopPlace;
import org.rutebanken.netex.model.GroupOfLines;
import org.rutebanken.netex.model.GroupsOfLinesInFrame_RelStructure;
import org.rutebanken.netex.model.JourneyPattern;
import org.rutebanken.netex.model.JourneyPatternsInFrame_RelStructure;
import org.rutebanken.netex.model.Line;
import org.rutebanken.netex.model.LinesInFrame_RelStructure;
import org.rutebanken.netex.model.Network;
import org.rutebanken.netex.model.NetworksInFrame_RelStructure;
import org.rutebanken.netex.model.PassengerStopAssignment;
import org.rutebanken.netex.model.Quay;
import org.rutebanken.netex.model.Route;
import org.rutebanken.netex.model.RoutesInFrame_RelStructure;
import org.rutebanken.netex.model.ServiceLink;
import org.rutebanken.netex.model.ServiceLinksInFrame_RelStructure;
import org.rutebanken.netex.model.Service_VersionFrameStructure;
import org.rutebanken.netex.model.StopAssignmentsInFrame_RelStructure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBElement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

class ServiceFrameParser extends NetexParser<Service_VersionFrameStructure> {

    private static final Logger LOG = LoggerFactory.getLogger(ServiceFrameParser.class);

    private final ReadOnlyHierarchicalVersionMapById<Quay> quayById;

    private final ReadOnlyHierarchicalMapById<FlexibleStopPlace> flexibleStopPlaceById;

    private final Collection<Network> networks = new ArrayList<>();

    private final Collection<GroupOfLines> groupOfLines = new ArrayList<>();

    private final Collection<Route> routes = new ArrayList<>();

    private final Collection<FlexibleLine> flexibleLines = new ArrayList<>();

    private final Collection<Line> lines = new ArrayList<>();

    private final Map<String, String> networkIdByGroupOfLineId = new HashMap<>();

    private final Collection<JourneyPattern> journeyPatterns = new ArrayList<>();

    private final Collection<DestinationDisplay> destinationDisplays = new ArrayList<>();

    private final Map<String, String> quayIdByStopPointRef = new HashMap<>();

    private final Map<String, String> flexibleStopPlaceByStopPointRef = new HashMap<>();

    private final Collection<ServiceLink> serviceLinks = new ArrayList<>();

    private final NoticeParser noticeParser = new NoticeParser();

    ServiceFrameParser(
        ReadOnlyHierarchicalVersionMapById<Quay> quayById,
        ReadOnlyHierarchicalMapById<FlexibleStopPlace> flexibleStopPlaceById
    ) {
        this.quayById = quayById;
        this.flexibleStopPlaceById = flexibleStopPlaceById;
    }

    @Override
    void parse(Service_VersionFrameStructure frame) {
        parseStopAssignments(frame.getStopAssignments());
        parseRoutes(frame.getRoutes());
        parseNetwork(frame.getNetwork());
        parseAdditionalNetworks(frame.getAdditionalNetworks());
        noticeParser.parseNotices(frame.getNotices());
        noticeParser.parseNoticeAssignments(frame.getNoticeAssignments());
        parseLines(frame.getLines());
        parseJourneyPatterns(frame.getJourneyPatterns());
        parseDestinationDisplays(frame.getDestinationDisplays());
        parseServiceLinks(frame.getServiceLinks());

        // Keep list sorted alphabetically
        warnOnMissingMapping(LOG, frame.getAdditionalNetworks());
        warnOnMissingMapping(LOG, frame.getCommonSections());
        warnOnMissingMapping(LOG, frame.getConnections());
        warnOnMissingMapping(LOG, frame.getDirections());
        warnOnMissingMapping(LOG, frame.getDisplayAssignments());
        warnOnMissingMapping(LOG, frame.getFlexibleLinkProperties());
        warnOnMissingMapping(LOG, frame.getFlexiblePointProperties());
        warnOnMissingMapping(LOG, frame.getGeneralSections());
        warnOnMissingMapping(LOG, frame.getGroupsOfLines());
        warnOnMissingMapping(LOG, frame.getGroupsOfLinks());
        warnOnMissingMapping(LOG, frame.getGroupsOfPoints());
        warnOnMissingMapping(LOG, frame.getLineNetworks());
        warnOnMissingMapping(LOG, frame.getLogicalDisplays());
        warnOnMissingMapping(LOG, frame.getPassengerInformationEquipments());
        warnOnMissingMapping(LOG, frame.getRouteLinks());
        warnOnMissingMapping(LOG, frame.getRoutePoints());
        warnOnMissingMapping(LOG, frame.getRoutingConstraintZones());
        warnOnMissingMapping(LOG, frame.getScheduledStopPoints());
        warnOnMissingMapping(LOG, frame.getServiceExclusions());
        warnOnMissingMapping(LOG, frame.getServicePatterns());
        warnOnMissingMapping(LOG, frame.getStopAreas());
        warnOnMissingMapping(LOG, frame.getTariffZones());
        warnOnMissingMapping(LOG, frame.getTimeDemandTypes());
        warnOnMissingMapping(LOG, frame.getTimeDemandTypeAssignments());
        warnOnMissingMapping(LOG, frame.getTimingPoints());
        warnOnMissingMapping(LOG, frame.getTimingLinks());
        warnOnMissingMapping(LOG, frame.getTimingLinkGroups());
        warnOnMissingMapping(LOG, frame.getTimingPatterns());
        warnOnMissingMapping(LOG, frame.getTransferRestrictions());

        verifyCommonUnusedPropertiesIsNotSet(LOG, frame);
    }

    @Override
    void setResultOnIndex(NetexEntityDataIndex index) {
        // update entities
        index.destinationDisplayById.addAll(destinationDisplays);
        index.groupOfLinesById.addAll(groupOfLines);
        index.journeyPatternsById.addAll(journeyPatterns);
        index.flexibleLineByid.addAll(flexibleLines);
        index.lineById.addAll(lines);
        index.networkById.addAll(networks);
        noticeParser.setResultOnIndex(index);
        index.quayIdByStopPointRef.addAll((quayIdByStopPointRef));
        index.flexibleStopPlaceByStopPointRef.addAll(flexibleStopPlaceByStopPointRef);
        index.routeById.addAll(routes);
        index.serviceLinkById.addAll(serviceLinks);

        // update references
        index.networkIdByGroupOfLineId.addAll(networkIdByGroupOfLineId);
    }

    private void parseStopAssignments(StopAssignmentsInFrame_RelStructure stopAssignments) {
        if (stopAssignments == null) return;

        for (JAXBElement<?> stopAssignment : stopAssignments.getStopAssignment()) {
            if (stopAssignment.getValue() instanceof PassengerStopAssignment) {
                PassengerStopAssignment assignment = (PassengerStopAssignment) stopAssignment
                        .getValue();
                String quayRef = assignment.getQuayRef().getRef();

                // TODO OTP2 - This check belongs to the mapping or as a separate validation
                //           - step. The problem is that we do not want to relay on the
                //           - the order in witch elements are loaded.
                Quay quay = quayById.lookupLastVersionById(quayRef);
                if (quay != null) {
                    String stopPointRef = assignment.getScheduledStopPointRef().getValue().getRef();
                    quayIdByStopPointRef.put(stopPointRef, quay.getId());
                } else {
                    LOG.warn("Quay {} not found in stop place file.", quayRef);
                }
            }
            else if (stopAssignment.getValue() instanceof FlexibleStopAssignment) {
                if(OTPFeature.FlexRouting.isOn()) {
                    FlexibleStopAssignment assignment = (FlexibleStopAssignment) stopAssignment.getValue();
                    String flexibleStopPlaceRef = assignment.getFlexibleStopPlaceRef().getRef();

                    // TODO OTP2 - This check belongs to the mapping or as a separate validation
                    //           - step. The problem is that we do not want to relay on the
                    //           - the order in witch elements are loaded.
                    FlexibleStopPlace flexibleStopPlace = flexibleStopPlaceById.lookup(
                        flexibleStopPlaceRef);

                    if (flexibleStopPlace != null) {
                        String stopPointRef = assignment.getScheduledStopPointRef().getValue().getRef();
                        flexibleStopPlaceByStopPointRef.put(stopPointRef, flexibleStopPlace.getId());
                    }
                    else {
                        LOG.warn(
                            "FlexibleStopPlace {} not found in stop place file.",
                            flexibleStopPlaceRef
                        );
                    }
                }
            }
            else {
                warnOnMissingMapping(LOG, stopAssignment.getValue());
            }
        }
    }

    private void parseRoutes(RoutesInFrame_RelStructure routes) {
        if (routes == null) return;

        for (JAXBElement<?> element : routes.getRoute_()) {
            if (element.getValue() instanceof Route) {
                Route route = (Route) element.getValue();
                this.routes.add(route);
            }
        }
    }

    private void parseNetwork(Network network) {
        if (network == null) return;

        networks.add(network);

        GroupsOfLinesInFrame_RelStructure groupsOfLines = network.getGroupsOfLines();

        if (groupsOfLines != null) {
            parseGroupOfLines(groupsOfLines.getGroupOfLines(), network);
        }
    }

    private void parseAdditionalNetworks(NetworksInFrame_RelStructure additionalNetworks) {
        if (additionalNetworks == null) { return; }

        for (Network additionalNetwork : additionalNetworks.getNetwork()) {
            parseNetwork(additionalNetwork);
        }
    }

    private void parseGroupOfLines(Collection<GroupOfLines> groupOfLines, Network network) {
        for (GroupOfLines group : groupOfLines) {
            networkIdByGroupOfLineId.put(group.getId(), network.getId());
            this.groupOfLines.add(group);
        }
    }

    private void parseLines(LinesInFrame_RelStructure lines) {
        if (lines == null) return;

        for (JAXBElement<?> element : lines.getLine_()) {
            if (element.getValue() instanceof Line) {
                this.lines.add((Line) element.getValue());
            } else if (element.getValue() instanceof FlexibleLine) {
                if(OTPFeature.FlexRouting.isOn()) {
                    this.flexibleLines.add((FlexibleLine) element.getValue());
                }
            }
            else {
                warnOnMissingMapping(LOG, element.getValue());
            }
        }
    }

    private void parseJourneyPatterns(JourneyPatternsInFrame_RelStructure journeyPatterns) {
        if (journeyPatterns == null) return;

        for (JAXBElement<?> pattern : journeyPatterns.getJourneyPattern_OrJourneyPatternView()) {
            if (pattern.getValue() instanceof JourneyPattern) {
                this.journeyPatterns.add((JourneyPattern) pattern.getValue());
            }
            else {
                warnOnMissingMapping(LOG, pattern.getValue());
            }
        }
    }

    private void parseDestinationDisplays(DestinationDisplaysInFrame_RelStructure destDisplays) {
        if (destDisplays == null) return;

        this.destinationDisplays.addAll(destDisplays.getDestinationDisplay());
    }

    private void parseServiceLinks(ServiceLinksInFrame_RelStructure serviceLinks) {
        if (serviceLinks == null) return;

        this.serviceLinks.addAll(serviceLinks.getServiceLink());
    }
}
