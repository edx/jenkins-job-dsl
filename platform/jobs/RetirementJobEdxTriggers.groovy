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
        downstreamJobName: 'user-retirement-collector',
        triggerJobNamePrefix: 'user-retirement-trigger',
        environmentDeployment: 'prod-edx',
        extraMembersCanBuild: [],
        cron: 'H * * * *',  // Hourly at an arbitrary, but consistent minute.
        disabled: false
    ],
    [
        downstreamJobName: 'user-retirement-collector',
        triggerJobNamePrefix: 'user-retirement-trigger',
        environmentDeployment: 'prod-edge',
        extraMembersCanBuild: [],
        cron: 'H * * * *',  // Hourly at an arbitrary, but consistent minute.
        disabled: false
    ],
    [
        downstreamJobName: 'retirement-partner-reporter',
        triggerJobNamePrefix: 'retirement-partner-reporter-trigger',
        environmentDeployment: 'prod-edx',
        extraMembersCanBuild: [],
        cron: '0 8 * * 2',  // 09:00 UTC every Tuesday.
        disabled: true
    ],
    [
        downstreamJobName: 'retirement-partner-reporter',
        triggerJobNamePrefix: 'retirement-partner-reporter-trigger',
        environmentDeployment: 'prod-edge',
        extraMembersCanBuild: [],
        cron: '30 8 * * 2',  // 09:30 UTC every Tuesday.
        disabled: false
    ],
    [
        downstreamJobName: 'retirement-partner-report-cleanup',
        triggerJobNamePrefix: 'retirement-partner-report-cleanup-trigger',
        environmentDeployment: 'prod-edx',
        extraMembersCanBuild: [],
        cron: '0 7 * * *',  // 08:00 UTC every day.
        disabled: false
    ],
    [
        downstreamJobName: 'retirement-partner-report-cleanup',
        triggerJobNamePrefix: 'retirement-partner-report-cleanup-trigger',
        environmentDeployment: 'prod-edge',
        extraMembersCanBuild: [],
        cron: '30 7 * * *',  // 08:30 UTC every day.
        disabled: false
    ]
]

jobConfigs.each { jobConfig ->

    // ########### user-retirement-trigger-<environment> ###########
    // This defines the job which triggers the collector job for a given environment.
    job("${jobConfig.triggerJobNamePrefix}-${jobConfig.environmentDeployment}") {
        description("Scheduled trigger of the ${jobConfig.downstreamJobName} job for the ${jobConfig.environmentDeployment} environment")

        disabled(jobConfig.disabled)

        // Only a subset of edx employees should be allowed to control this job,
        // but customer support can read and discover.
        authorization {
            blocksInheritance(true)
            List membersWithFullControl = ['edx*edx-data-engineering', 'edx*testeng', 'edx*devops']
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

        // retirement-workers are configured to only execute one build at a time
        label('retirement-worker')

        // Disallow this job to have simultaneous instances building at the same
        // time.  This might help prevent race conditions related to triggering
        // multiple retirement driver jobs against the same user.
        concurrentBuild(false)

        triggers {
            // Build every hour.
            cron(jobConfig.cron)
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
                trigger("${jobConfig.downstreamJobName}") {
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
