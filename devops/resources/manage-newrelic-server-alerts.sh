cd sysadmin/newrelic

pip install -r requirements.txt

python add_app_servers_to_server_policy.py --new-relic-api-key ${NEW_RELIC_API_KEY} --alert-policy-name "${ALERT_POLICY_NAME}" ${APP_NAMES}
