/*
Transform a comma-separated list of environment variables into a mapping of variables for a Jenkins job
*/

if (ENV_VARS == null || ENV_VARS == "") {
    return null
}

def map = [:]
ENV_VARS.split(",").each {param ->
def nameAndValue = param.split("=")
    map[nameAndValue[0]] = nameAndValue[1]
}

return map
