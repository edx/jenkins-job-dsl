import hudson.model.*
def res = manager.build.getResult().toString()
def buildStatusParam = new StringParameterValue('BUILD_STATUS', res)
manager.build.replaceAction(new ParametersAction(buildStatusParam))
