import hudson.FilePath
import hudson.model.*

def toolbox = extension."build-flow-toolbox"
def driver_build = null

build.setDisplayName '#${BUILD_NUMBER} by ${ENV,var="BUILD_USER_ID"}'

def loadtests = [
    [
        TARGET_URL: 'https://perf-nightly-test.sandbox.edx.org',
        TEST_COMPONENT: 'lms',
        NUM_CLIENTS: 15,
        HATCH_RATE: 2,
        LOADTEST_OVERRIDES: ''
    ],
    [
        TARGET_URL: 'https://perf-nightly-test.sandbox.edx.org',
        TEST_COMPONENT: 'lms',
        NUM_CLIENTS: 10,
        HATCH_RATE: 2,
        LOADTEST_OVERRIDES: ''
    ],
]

loadtests.each { local_params ->
  driver_build = build(
      "run-simple-loadtest",
      TARGET_URL: local_params["TARGET_URL"],
      TEST_COMPONENT: local_params['TEST_COMPONENT'],
      REMOTE_BRANCH: params["REMOTE_BRANCH"],
      NUM_CLIENTS: local_params["NUM_CLIENTS"],
      HATCH_RATE: local_params["HATCH_RATE"],
      MAX_RUN_TIME: params["MAX_RUN_TIME_EACH"],
      LOADTEST_OVERRIDES: local_params["LOADTEST_OVERRIDES"]
  )
  /*
  toolbox.slurpArtifacts(driver_build)
  # driver_build contains a summary artifact which we need to read
  */
}

/* Kick off a build to generate a load test summary */
/*
def summary_build = build(
    'loadtest-summary',
    REMOTE_BRANCH: params["REMOTE_BRANCH"]
)
toolbox.slurpArtifacts(summary_build)
*/
