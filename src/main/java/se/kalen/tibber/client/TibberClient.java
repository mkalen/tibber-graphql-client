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
import no.tibber.api.model.ConsumptionResolution;
import no.tibber.api.model.Home;
import no.tibber.api.model.query.HomeConsumptionRequest;
import no.tibber.api.model.query.HomeRequest;
import no.tibber.api.model.query.HomesRequest;
import se.kalen.tibber.util.FormatUtil;

/**
 * Java Client for Tibber GraphQL API.
 * 
 * @author Martin Kal�n
 */
public class TibberClient {

    private static final String API_ENDPOINT = "https://api.tibber.com/v1-beta/gql";

    private final Logger logger = LoggerFactory.getLogger(TibberClient.class);
    private final Map<String, String> headers;
    private GraphQLTemplate graphQLTemplate;

    public TibberClient(String apiKey) {
        headers = new HashMap<>();
        headers.put("Authorization", "bearer " + apiKey);
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

    public List<Consumption> getConsumption(String homeId, ConsumptionResolution resolution) throws IllegalStateException, MalformedURLException {
        return getConsumption(homeId,
                new Argument<ConsumptionResolution>("resolution", resolution),
                new Argument<String>("after", "1990-01-01T00:00:00+01:00"),
                new Argument<Integer>("first", 1),
                new Argument<Boolean>("filterEmptyNodes", false)
        );
    }

    public List<Consumption> getConsumptionSince(String homeId, ConsumptionResolution resolution, OffsetDateTime since) throws IllegalStateException, MalformedURLException {
        // TODO: How to filter directly in API? Contact Tibber regarding "after" parameter...
        final List<Consumption> candidates = getConsumption(homeId,
                new Argument<ConsumptionResolution>("resolution", resolution),
                new Argument<String>("after", FormatUtil.toString(since)),
                new Argument<Integer>("last", 100000),
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

    public List<Consumption> getConsumptionSince(String homeId, ConsumptionResolution resolution,
            OffsetDateTime since, OffsetDateTime until) throws IllegalStateException, MalformedURLException {
        // TODO: How to filter directly in API? Contact Tibber regarding "after" parameter...
        final List<Consumption> candidates = getConsumption(homeId,
                new Argument<ConsumptionResolution>("resolution", resolution),
                new Argument<String>("after", FormatUtil.toString(since)),
                new Argument<Integer>("last", 100000),
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

    public List<Consumption> getConsumptionFromEnd(String homeId, ConsumptionResolution resolution, int last) throws IllegalStateException, MalformedURLException {
        return getConsumption(homeId,
                new Argument<ConsumptionResolution>("resolution", resolution),
                new Argument<Integer>("last", last),
                new Argument<Boolean>("filterEmptyNodes", false)
        );
    }

    private List<Consumption> getConsumption(String homeId, Argument<?> ... arguments) throws IllegalStateException, MalformedURLException {
        GraphQLRequestEntity requestEntity = getRequestBuilder() 
                .arguments(
                        new Arguments("viewer.home",
                                new Argument<String>("id", homeId)
                        ),
                        new Arguments("viewer.home.consumption",
                                arguments
                        ))
                .request(HomeConsumptionRequest.class)
                .build();
        debugRequest(requestEntity);

        GraphQLResponseEntity<HomeConsumptionRequest> responseEntity = graphQLTemplate.query(requestEntity, HomeConsumptionRequest.class);
        if (responseEntity != null
                && responseEntity.getResponse() != null
                && responseEntity.getResponse().getHome() != null
                && responseEntity.getResponse().getHome().getConsumption() != null) {
            return responseEntity.getResponse().getHome().getConsumption().getNodes();
        }
        return Collections.emptyList();
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
            if (responseEntity.getErrors() != null) {
                for (io.aexp.nodes.graphql.internal.Error error : responseEntity.getErrors()) {
                    logger.debug(" Error: {}", error.getMessage());
                }
            }
            if (responseEntity.getResponse() != null) {
                logger.debug(" Entity: {}", responseEntity.getResponse());
            } else {
                logger.debug(" Entity: (null)");
            }
        } else {
            logger.debug("(null)");
        }
    }

}
