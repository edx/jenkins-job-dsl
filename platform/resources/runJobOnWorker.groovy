import hudson.model.*

JOB_TO_RUN = build.environment.get('JOB_TO_RUN')
ghprbActualCommit = build.environment.get('ghprbActualCommit')
WORKER_LABEL = build.environment.get('WORKER_LABEL')

// Note: This is a build-flow-dsl script, NOT a job-dsl-script.
// Please reference https://wiki.jenkins-ci.org/display/JENKINS/Build+Flow+Plugin
// for more information
build(JOB_TO_RUN, ghprbActualCommit: ghprbActualCommit, WORKER_LABEL: WORKER_LABEL)
