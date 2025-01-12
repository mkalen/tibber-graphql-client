package se.kalen.tibber.client;

import java.net.MalformedURLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.aexp.nodes.graphql.Argument;
import io.aexp.nodes.graphql.Arguments;
import io.aexp.nodes.graphql.GraphQLRequestEntity;
import io.aexp.nodes.graphql.GraphQLRequestEntity.RequestBuilder;
import io.aexp.nodes.graphql.GraphQLResponseEntity;
import io.aexp.nodes.graphql.GraphQLTemplate;
import no.tibber.api.model.Consumption;
import no.tibber.api.model.EnergyResolution;
import no.tibber.api.model.Home;
import no.tibber.api.model.Production;
import no.tibber.api.model.query.HomeProsumptionRequest;
import no.tibber.api.model.query.HomeRequest;
import no.tibber.api.model.query.HomesRequest;
import se.kalen.tibber.util.FormatUtil;

/**
 * Java Client for Tibber GraphQL API.
 * 
 * @author Martin Kalén
 */
public class TibberClient {

    private static final String API_ENDPOINT = "https://api.tibber.com/v1-beta/gql";
    private static final int API_CONSUMPTION_CAP = 744; // 31 days

    private final Logger logger = LoggerFactory.getLogger(TibberClient.class);
    private final Map<String, String> headers;
    private GraphQLTemplate graphQLTemplate;

    public TibberClient(String apiKey) {
        headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + apiKey);
        graphQLTemplate = new GraphQLTemplate();
    }

    public List<Home> getHomes() throws IllegalStateException, MalformedURLException {
        GraphQLRequestEntity requestEntity = getRequestBuilder() 
                .request(HomesRequest.class)
                .build();
        debugRequest(requestEntity);

        GraphQLResponseEntity<HomesRequest> responseEntity = graphQLTemplate.query(requestEntity, HomesRequest.class);
        debugResponse(responseEntity);
        return responseEntity.getResponse().getHomes();
    }

    public Home getHomeById(String homeId) throws IllegalStateException, MalformedURLException {
        GraphQLRequestEntity requestEntity = getRequestBuilder() 
                .arguments(new Arguments("viewer.home", new Argument<String>("id", homeId)))
                .request(HomeRequest.class)
                .build();
        debugRequest(requestEntity);

        GraphQLResponseEntity<HomeRequest> responseEntity = graphQLTemplate.query(requestEntity, HomeRequest.class);
        debugResponse(responseEntity);
        return responseEntity.getResponse().getHome();
    }

    public List<Consumption> getConsumption(String homeId, EnergyResolution resolution) throws IllegalStateException, MalformedURLException {
        return getConsumption(homeId,
                new Argument<EnergyResolution>("resolution", resolution),
                new Argument<String>("after", FormatUtil.toCursorString("1990-01-01T00:00:00+01:00")),
                new Argument<Integer>("first", 1),
                new Argument<Boolean>("filterEmptyNodes", false)
        );
    }

    public List<Consumption> getConsumptionSince(String homeId, EnergyResolution resolution, OffsetDateTime since) throws IllegalStateException, MalformedURLException {
        final String after = FormatUtil.toCursorString(since);
        logger.debug("after=" + after);
        // TODO: How to filter directly in API? Contact Tibber regarding "after" parameter...
        final List<Consumption> candidates = getConsumption(homeId,
                new Argument<EnergyResolution>("resolution", resolution),
                new Argument<String>("after", after),
                new Argument<Integer>("first", API_CONSUMPTION_CAP),
                new Argument<Boolean>("filterEmptyNodes", false)
        );
        final List<Consumption> result = new ArrayList<Consumption>();
        for (final Consumption candidate : candidates) {
            if (candidate != null && candidate.getFrom() != null && !candidate.getFrom().isBefore(since)) {
                result.add(candidate);
            }
        }
        return result;
    }

    /*
    public List<Consumption> getConsumptionSince(String homeId, EnergyResolution resolution,
            OffsetDateTime since, OffsetDateTime until) throws IllegalStateException, MalformedURLException {
        final String after = FormatUtil.toCursorString(since);
        logger.debug("after=" + after);
        // TODO: How to filter directly in API? Contact Tibber regarding "after" parameter...
        final List<Consumption> candidates = getConsumption(homeId,
                new Argument<EnergyResolution>("resolution", resolution),
                new Argument<String>("after", after),
                new Argument<Integer>("first", API_CONSUMPTION_CAP),
                new Argument<Boolean>("filterEmptyNodes", false)
        );
        final List<Consumption> result = new ArrayList<Consumption>();
        for (final Consumption candidate : candidates) {
            if (candidate != null
                    && candidate.getFrom() != null
                    && candidate.getTo() != null
                    && !candidate.getFrom().isBefore(since)
                    && (candidate.getTo().isBefore(until) || candidate.getTo().toInstant().equals(until.toInstant()))) {
                result.add(candidate);
            }
        }
        return result;
    }
    */

    public List<Consumption> getConsumptionSince(String homeId, EnergyResolution resolution,
            OffsetDateTime since, OffsetDateTime until) throws IllegalStateException, MalformedURLException {
        final List<Consumption> result = new ArrayList<>();
        OffsetDateTime currentSince = since;
        boolean hasMoreData = true;

        while (hasMoreData) {
            final String after = FormatUtil.toCursorString(currentSince);
            logger.debug("Fetching consumption data with after=" + currentSince.toString());

            final List<Consumption> candidates = getConsumption(homeId,
                    new Argument<EnergyResolution>("resolution", resolution),
                    new Argument<String>("after", after),
                    new Argument<Integer>("first", API_CONSUMPTION_CAP),
                    new Argument<Boolean>("filterEmptyNodes", false)
            );

            // If we got no results or less than the cap, we've reached the end
            if (candidates == null || candidates.isEmpty() || candidates.size() < API_CONSUMPTION_CAP) {
                hasMoreData = false;
            }

            // Filter and add valid entries to result
            for (final Consumption candidate : candidates) {
                if (isValidConsumption(candidate, since, until)) {
                    result.add(candidate);
                    // Update currentSince to the latest valid entry's timestamp for next iteration
                    if (candidate.getTo() != null) {
                        currentSince = candidate.getTo();
                    }
                }
            }

            // Safety check: if we've reached or passed the until date, we're done
            if (currentSince.isEqual(until) || currentSince.isAfter(until)) {
                hasMoreData = false;
            }

            // Add small delay to avoid overwhelming the API
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while fetching consumption data", e);
            }
        }

        return result;
    }

    public List<Production> getProductionSince(String homeId, EnergyResolution resolution,
            OffsetDateTime since, OffsetDateTime until) throws IllegalStateException, MalformedURLException {
        final List<Production> result = new ArrayList<>();
        OffsetDateTime currentSince = since;
        boolean hasMoreData = true;

        while (hasMoreData) {
            final String after = FormatUtil.toCursorString(currentSince);
            logger.debug("Fetching consumption data with after=" + currentSince.toString());

            final List<Production> candidates = getProduction(homeId,
                    new Argument<EnergyResolution>("resolution", resolution),
                    new Argument<String>("after", after),
                    new Argument<Integer>("first", API_CONSUMPTION_CAP),
                    new Argument<Boolean>("filterEmptyNodes", false)
            );

            // If we got no results or less than the cap, we've reached the end
            if (candidates == null || candidates.isEmpty() || candidates.size() < API_CONSUMPTION_CAP) {
                hasMoreData = false;
            }

            // Filter and add valid entries to result
            for (final Production candidate : candidates) {
                if (isValidProduction(candidate, since, until)) {
                    result.add(candidate);
                    // Update currentSince to the latest valid entry's timestamp for next iteration
                    if (candidate.getTo() != null) {
                        currentSince = candidate.getTo();
                    }
                }
            }

            // Safety check: if we've reached or passed the until date, we're done
            if (currentSince.isEqual(until) || currentSince.isAfter(until)) {
                hasMoreData = false;
            }

            // Add small delay to avoid overwhelming the API
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while fetching consumption data", e);
            }
        }

        return result;
    }

    public List<Consumption> getConsumptionFromEnd(String homeId, EnergyResolution resolution, int last) throws IllegalStateException, MalformedURLException {
        return getConsumption(homeId,
                new Argument<EnergyResolution>("resolution", resolution),
                new Argument<Integer>("last", last),
                new Argument<Boolean>("filterEmptyNodes", false)
        );
    }

    private List<Consumption> getConsumption(String homeId, Argument<?> ... arguments) throws IllegalStateException, MalformedURLException {
        GraphQLRequestEntity requestEntity = getProsumptionRequest(homeId, arguments);
        debugRequest(requestEntity);

        GraphQLResponseEntity<HomeProsumptionRequest> responseEntity = graphQLTemplate.query(requestEntity, HomeProsumptionRequest.class);
        warnAboutResponseErrors(responseEntity);
        //debugResponse(responseEntity);
        if (responseEntity != null
                && responseEntity.getResponse() != null
                && responseEntity.getResponse().getHome() != null
                && responseEntity.getResponse().getHome().getConsumption() != null) {
            return responseEntity.getResponse().getHome().getConsumption().getNodes();
        }
        return Collections.emptyList();
    }

    private List<Production> getProduction(String homeId, Argument<?> ... arguments) throws IllegalStateException, MalformedURLException {
        GraphQLRequestEntity requestEntity = getProsumptionRequest(homeId, arguments);
        debugRequest(requestEntity);

        GraphQLResponseEntity<HomeProsumptionRequest> responseEntity = graphQLTemplate.query(requestEntity, HomeProsumptionRequest.class);
        warnAboutResponseErrors(responseEntity);
        //debugResponse(responseEntity);
        if (responseEntity != null
                && responseEntity.getResponse() != null
                && responseEntity.getResponse().getHome() != null
                && responseEntity.getResponse().getHome().getProduction() != null) {
            return responseEntity.getResponse().getHome().getProduction().getNodes();
        }
        return Collections.emptyList();
    }

    private GraphQLRequestEntity getProsumptionRequest(String homeId, Argument<?> ... arguments) throws IllegalStateException, MalformedURLException {
        return getRequestBuilder() 
                .arguments(
                        new Arguments("viewer.home",
                                new Argument<String>("id", homeId)
                        ),
                        new Arguments("viewer.home.consumption",
                                arguments
                        ),
                        new Arguments("viewer.home.production",
                                arguments
                        ))
                .request(HomeProsumptionRequest.class)
                .build();
    }

    // Helper method to check if a consumption entry is valid
    private boolean isValidConsumption(Consumption candidate, OffsetDateTime since, OffsetDateTime until) {
        return candidate != null
                && candidate.getFrom() != null
                && candidate.getTo() != null
                && !candidate.getFrom().isBefore(since)
                && (candidate.getTo().isBefore(until) || candidate.getTo().toInstant().equals(until.toInstant()));
    }

    // Helper method to check if a consumption entry is valid
    private boolean isValidProduction(Production candidate, OffsetDateTime since, OffsetDateTime until) {
        return candidate != null
                && candidate.getFrom() != null
                && candidate.getTo() != null
                && !candidate.getFrom().isBefore(since)
                && (candidate.getTo().isBefore(until) || candidate.getTo().toInstant().equals(until.toInstant()));
    }

    private RequestBuilder getRequestBuilder() throws MalformedURLException {
        return GraphQLRequestEntity.Builder()
                .url(API_ENDPOINT)
                .headers(headers)
                .scalars(no.tibber.api.model.Status.class);
    }

    private void debugRequest(GraphQLRequestEntity requestEntity) {
        if (!logger.isDebugEnabled()) {
            return;
        }
        logger.debug("Request:");
        if (requestEntity != null) {
            logger.debug(" {}", requestEntity.getRequest());
        } else {
            logger.debug(" (null)");
        }
    }

    private void debugResponse(GraphQLResponseEntity<?> responseEntity) {
        if (!logger.isDebugEnabled()) {
            return;
        }
        logger.debug("Response:");
        if (responseEntity != null) {
            if (responseEntity.getResponse() != null) {
                logger.debug(" Entity: {}", responseEntity.getResponse());
            } else {
                logger.debug(" Entity: (null)");
            }
        } else {
            logger.debug("(null)");
        }
    }

    private void warnAboutResponseErrors(GraphQLResponseEntity<?> responseEntity) {
        if (responseEntity == null) {
            return;
        }
        if (responseEntity.getErrors() != null) {
            for (io.aexp.nodes.graphql.internal.Error error : responseEntity.getErrors()) {
                logger.warn(" Error: {}", error.getMessage());
            }
        }
    }

}
