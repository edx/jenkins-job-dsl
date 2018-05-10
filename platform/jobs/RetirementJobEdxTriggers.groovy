/* UserRetirementJobEdxTriggers.groovy
 *
 * Defines jobs which trigger downstream user retirement jobs for each edX
 * environment.
 */

package platform

import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_TEAM_SECURITY
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_LOG_ROTATOR

List jobConfigs = [
    [
        environmentDeployment: 'prod-edx',
        extraMembersCanBuild: [],
        disabled: true
    ],
    [
        environmentDeployment: 'stage-edx',
        extraMembersCanBuild: ['edx*educator-all', 'edx*learner'],
        disabled: true
    ],
    [
        environmentDeployment: 'loadtest-edx',
        extraMembersCanBuild: ['edx*educator-all', 'edx*learner'],
        disabled: true
    ],
    [
        environmentDeployment: 'prod-edge',
        extraMembersCanBuild: [],
        disabled: true
    ]
]


jobConfigs.each { jobConfig ->

    // ########### user-retirement-trigger-<environment> ###########
    // This defines the job which triggers the collector job for a given environment.
    job("user-retirement-trigger-${jobConfig.environmentDeployment}") {
        description("Scheduled trigger of the user-retirement-collector job for the ${jobConfig.environmentDeployment} environment")

        disabled(jobConfig.disabled)

        // Only a subset of edx employees should be allowed to control this job,
        // but customer support can read and discover.
        authorization {
            blocksInheritance(true)
            List membersWithFullControl = ['edx*platform-team', 'edx*testeng', 'edx*devops']
            membersWithFullControl.each { emp ->
                permissionAll(emp)
            }
            jobConfig.extraMembersCanBuild.each { emp ->
                permission('hudson.model.Item.Read', emp)
                permission('hudson.model.Item.Discover', emp)
                permission('hudson.model.Item.Build', emp)
                permission('hudson.model.Item.Cancel', emp)
            }
            // TODO PLAT-2036: uncomment the following two lines when we add the
            // appropriate github group.
            //permission('hudson.model.Item.Read', 'edx/customer-support')
            //permission('hudson.model.Item.Discover', 'edx/customer-support')
        }

        // jenkins-worker is way overkill for this job, but it'll do for now.
        // TODO: either create a new lightweight worker label, or wait until we
        // convert this job to use Pipelines.
        label('jenkins-worker')

        // Disallow this job to have simultaneous instances building at the same
        // time.  This might help prevent race conditions related to triggering
        // multiple retirement driver jobs against the same user.
        concurrentBuild(false)

        triggers {
            // Build every hour.
            cron('H * * * *')
        }

        // keep jobs around for 30 days
        logRotator JENKINS_PUBLIC_LOG_ROTATOR(30)

        wrappers {
            buildName('#${BUILD_NUMBER}')
            timestamps()
            colorizeOutput('xterm')
        }

        steps {
            downstreamParameterized {
                trigger('user-retirement-collector') {
                    // This section causes the build to block on completion of downstream builds.
                    block {
                        // Mark this build step as FAILURE if at least one of the downstream builds were marked FAILED.
                        buildStepFailure('FAILURE')
                        // Mark this entire build as FAILURE if at least one of the downstream builds were marked FAILED.
                        failure('FAILURE')
                        // Mark this entire build as UNSTABLE if at least one of the downstream builds were marked UNSTABLE.
                        unstable('UNSTABLE')
                    }
                    parameters {
                        predefinedProp('ENVIRONMENT', "${jobConfig.environmentDeployment}")
                    }
                }
            }
        }
    }
}
