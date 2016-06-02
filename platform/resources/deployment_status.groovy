// This will make it easier to quickly review the builds to make sure the deployments are working correctly.

def logger = { logThis, msg -> logThis ? manager.listener.logger.println(msg) : null }
logger(true, "Executing the Groovy Postbuild Script")

def messageKey = '^.*"message": '
def messagePattern = messageKey + '"(.+)".*'
def taskKey = '^.*"task": '
def taskPattern = taskKey + '(.+),.*'

if (manager.logContains(messageKey + '.*')) {
    // Did not successfully create the deployment, but
    // got a response from GitHub with a message explaining why.
    def matcher = manager.getLogMatcher(messagePattern)
    def message = matcher.group(1)
    manager.addInfoBadge(message)
    return
} else if (manager.logContains(taskKey + '.*')) {
    // Created a new deployment
    def matcher = manager.getLogMatcher(taskPattern)
    def message = matcher.group(1)
    manager.addBadge('completed.gif', message)
    return
}
