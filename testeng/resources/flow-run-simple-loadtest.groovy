import hudson.FilePath
import hudson.model.*

def toolbox = extension."build-flow-toolbox"
def driver_build = null

/* Kick off the load test, directly copying all parameters.  Ignore the
 * scenario where the load test is manually aborted by the user because this is
 * considered normal behavior.
 */
ignore(ABORTED) {
    driver_build = build(
        "loadtest-driver",
        TARGET_URL: params["TARGET_URL"],
        TEST_COMPONENT: params["TEST_COMPONENT"],
        REMOTE_BRANCH: params["REMOTE_BRANCH"],
        NUM_CLIENTS: params["NUM_CLIENTS"],
        HATCH_RATE: params["HATCH_RATE"],
        MAX_RUN_TIME: params["MAX_RUN_TIME"],
        LOADTEST_OVERRIDES: params["LOADTEST_OVERRIDES"]
    )
}
toolbox.slurpArtifacts(driver_build)

/* Kick off a build to generate a load test summary */
def summary_build = build(
    'loadtest-summary',
    REMOTE_BRANCH: params["REMOTE_BRANCH"]
)
toolbox.slurpArtifacts(summary_build)
