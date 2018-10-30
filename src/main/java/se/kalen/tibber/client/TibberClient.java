package se.kalen.tibber.client;

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

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
        System.out.println("Request:");
        if (requestEntity == null) {
            System.out.println("(null)");
        } else {
            System.out.println(requestEntity.getRequest());

        }
    }

    private void debugResponse(GraphQLResponseEntity<?> responseEntity) {
        System.out.println("Response:");
        System.out.println(responseEntity);

        if (responseEntity.getErrors() != null) {
            for (io.aexp.nodes.graphql.internal.Error error : responseEntity.getErrors()) {
                System.err.println("Error: " + error.getMessage());
            }
        }
        if (responseEntity.getResponse() == null) {
            System.out.println("(null)");
        } else {
            System.out.println("Response");
            System.out.println(responseEntity.getResponse().toString());
        }
    }

}
