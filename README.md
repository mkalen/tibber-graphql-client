# Tibber GraphQL Java Model

Java GraphQL client for [Tibber GraphQL API](https://developer.tibber.com/docs/overview) using [Nodes GraphQL library](https://github.com/americanexpress/nodes).

Depends on the [Tibber GraphQL Model](https://github.com/mkalen/tibber-graphql-model) library.

Uses the Tibber [GraphQL v1 beta API](https://api.tibber.com/v1-beta/gql) endpoint.

## Pre-Requisites

1. Access Token for Tibber Authorization. Aquire via the [Tibber Developer](https://developer.tibber.com/) site.

## Building

1. Build the model library and install in local Maven repo using `mvn install`

1. Build the client library and install in local Maven repo using `mvn install`

## Usage

Simple example usage without dynamic configuration:


```
private static final String AUTH_TOKEN = "(your Tibber authorization Access Token)";
private static final String HOME_ID = "(Primary home id)";
private TibberClient client;

...

        client = new TibberClient(AUTH_TOKEN);

	# Get homes
        List<Home> homes = client.getHomes();
        for (Home home : homes) {
            logger.info("Homes.Home: {}", home);
        }


	# Get consumption for specific time period for a certain home id
        OffsetDateTime since = OffsetDateTime.of(2018, 12, 01, 0, 0, 0, 0, ZoneOffset.of("+01:00"));
        OffsetDateTime until = OffsetDateTime.of(2019, 02, 16, 0, 0, 0, 0, ZoneOffset.of("+01:00"));
        List<Consumption> consumptions = client.getConsumptionSince(HOME_ID, ConsumptionResolution.HOURLY, since, until);
```

