# Disables an Opsgenie Heartbeat.
#
# Assumes that the heartbeat is already created in the Opsgenie settings here:
# https://edx.app.opsgenie.com/settings/heartbeat
#
# Required environment variables:
# OPSGENIE_HEARTBEAT_NAME: Name of the already-created Opsgenie heartbeat.
# OPSGENIE_HEARTBEAT_CONFIG_KEY: API key provided by Opsgenie which is authorized to modify heartbeat configuration.


OPSGENIE_HEARTBEAT_API_URL="https://api.opsgenie.com/v2/heartbeats"

if [ -n "$OPSGENIE_HEARTBEAT_NAME" ] && \
   [ -n "$OPSGENIE_HEARTBEAT_CONFIG_KEY" ]; then

    AUTH_HEADER="Authorization: GenieKey $OPSGENIE_HEARTBEAT_CONFIG_KEY"
    POST_URL="$OPSGENIE_HEARTBEAT_API_URL/$OPSGENIE_HEARTBEAT_NAME/disable"

    # Disable the existing heartbeat.
    curl -X POST "$POST_URL" --header "$AUTH_HEADER"
fi
