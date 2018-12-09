if (SPIGOT_STATE.equals("ON")) {
    def map = ["SPIGOT_MESSAGE": "Builds will run as normal"]; return map
} else {
    def map = ["SPIGOT_MESSAGE": "Builds will be queued"]; return map
}
