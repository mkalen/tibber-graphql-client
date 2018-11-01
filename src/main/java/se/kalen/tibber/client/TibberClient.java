package se.kalen.tibber.client;

import java.net.MalformedURLException;
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

/**
 * Java Client for Tibber GraphQL API.
 * 
 * @author Martin Kalén
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
        debugResponse(responseEntity);
        if (responseEntity != null && responseEntity.getResponse() != null && responseEntity.getResponse().getHome() != null) {
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
