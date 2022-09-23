#!/usr/bin/env bash

# Enables an Opsgenie Heartbeat, while also setting the expected duration.
#
# Assumes that the heartbeat is already created in the Opsgenie settings here:
# https://2u-internal.app.opsgenie.com/settings/heartbeat
#
# Required environment variables:
# OPSGENIE_HEARTBEAT_NAME: Name of the already-created Opsgenie heartbeat.
# OPSGENIE_HEARTBEAT_CONFIG_KEY: API key provided by Opsgenie which is authorized to modify heartbeat configuration.
# OPSGENIE_HEARTBEAT_DURATION_NUM: Specifies how often a heartbeat message should be expected.
# OPSGENIE_HEARTBEAT_DURATION_UNIT: Interval specified as minutes, hours or days.


OPSGENIE_HEARTBEAT_API_URL="https://api.opsgenie.com/v2/heartbeats"

if [ -n "$OPSGENIE_HEARTBEAT_NAME" ] && \
   [ -n "$OPSGENIE_HEARTBEAT_CONFIG_KEY" ] &&  \
   [ -n "$OPSGENIE_HEARTBEAT_DURATION_NUM" ] && \
   [ -n "$OPSGENIE_HEARTBEAT_DURATION_UNIT" ]; then

    AUTH_HEADER="Authorization: GenieKey $OPSGENIE_HEARTBEAT_CONFIG_KEY"
    JSON_HEADER="Content-Type: application/json"
    HEARTBEAT_API_URL="$OPSGENIE_HEARTBEAT_API_URL/$OPSGENIE_HEARTBEAT_NAME"
    HEARTBEAT_API_PING_URL="$HEARTBEAT_API_URL/ping"
    ALERT_MESSAGE="Heartbeat [$OPSGENIE_HEARTBEAT_NAME] expired - job is likely stuck."
    
    # Make API request to get existing heartbeat
    # If the heartbeat already exists, then GET_EXISTING_HEARTBEAT will set to heartbeat name "GET_EXISTING_HEARTBEAT=heatbeat_name"
    # If heartbeat does not exist GET_EXISTING_HEARTBEAT is set to empty
    GET_EXISTING_HEARTBEAT=$(curl -X GET $HEARTBEAT_API_URL --header "$AUTH_HEADER" | grep -o $OPSGENIE_HEARTBEAT_NAME | sort -u)
     # Add the heartbeat if heartbeat doesn't exist
    if [ -z "$GET_EXISTING_HEARTBEAT" ]; then
        JSON_REQ_DATA_CREATE_HEARTBEAT="{\"name\":\"$OPSGENIE_HEARTBEAT_NAME\"
        ,\"intervalUnit\": \"$OPSGENIE_HEARTBEAT_DURATION_UNIT\"
        ,\"interval\": \"$OPSGENIE_HEARTBEAT_DURATION_NUM\"
        ,\"ownerTeam\": {\"name\": \"Data Engineering\"}
        ,\"alertMessage\": \"$ALERT_MESSAGE\"
        ,\"alertPriority\": \"P3\"
        ,\"enabled\" : true}"
        # Add heartbeat
        curl -X POST $OPSGENIE_HEARTBEAT_API_URL --header "$AUTH_HEADER" --header "$JSON_HEADER" --data "$JSON_REQ_DATA_CREATE_HEARTBEAT"
    fi
    # Json request body to update existing heartbeat
    JSON_REQ_DATA="{ \"interval\": \"$OPSGENIE_HEARTBEAT_DURATION_NUM\", \"intervalUnit\": \"$OPSGENIE_HEARTBEAT_DURATION_UNIT\", \"alertMessage\": \"$ALERT_MESSAGE\", \"enabled\": true }"
    # Enable the heartbeat and set the duration num/units.
    curl -X PATCH "$HEARTBEAT_API_URL" --header "$AUTH_HEADER" --header "$JSON_HEADER" --data "$JSON_REQ_DATA"
    # A re-enabled heartbeat will likely be expired, so ping it once to make it active and begin a new duration countdown.
    curl -X GET "$HEARTBEAT_API_PING_URL" --header "$AUTH_HEADER"
fi
