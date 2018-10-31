package se.kalen.tibber.client;

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.aexp.nodes.graphql.Argument;
import io.aexp.nodes.graphql.Arguments;
import io.aexp.nodes.graphql.GraphQLRequestEntity;
import io.aexp.nodes.graphql.GraphQLResponseEntity;
import io.aexp.nodes.graphql.GraphQLTemplate;
import no.tibber.api.model.Home;
import no.tibber.api.model.HomeRequest;

/**
 * Java Client for Tibber GraphQL API.
 * 
 * @author Martin Kalén
 */
public class TibberClient {

    private static final String API_ENDPOINT = "https://api.tibber.com/v1-beta/gql";

    private final Logger logger = LoggerFactory.getLogger(TibberClient.class);
    private final String apiKey;
    private GraphQLTemplate graphQLTemplate;

    public TibberClient(String apiKey) {
        this.apiKey = apiKey;
        graphQLTemplate = new GraphQLTemplate();
    }

    public Home getHomeById(String homeId) throws IllegalStateException, MalformedURLException {
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "bearer " + apiKey);
        GraphQLRequestEntity requestEntity = GraphQLRequestEntity.Builder()
                .url(API_ENDPOINT)
                .headers(headers)
                .arguments(new Arguments("viewer.home", new Argument<String>("id", homeId))).request(HomeRequest.class)
                .build();
        debugRequest(requestEntity);

        GraphQLResponseEntity<HomeRequest> responseEntity = graphQLTemplate.query(requestEntity, HomeRequest.class);
        debugResponse(responseEntity);
        return responseEntity.getResponse().getHome();
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
