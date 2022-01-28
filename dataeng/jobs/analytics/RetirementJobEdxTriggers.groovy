package analytics

import static org.edx.jenkins.dsl.AnalyticsConstants.common_log_rotator
import static org.edx.jenkins.dsl.AnalyticsConstants.common_wrappers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_publishers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_triggers
import static org.edx.jenkins.dsl.AnalyticsConstants.secure_scm_parameters
import static org.edx.jenkins.dsl.AnalyticsConstants.common_authorization

class RetirementJobEdxTriggers{
    public static def job = { dslFactory, allVars ->
        allVars.get('ENVIRONMENTS').each { environment, env_config ->
            dslFactory.job("$environment"){
                // Creates user_retirement_trigger_<environment> and retirement_partner_report_trigger_<environment> jobs
                // This defines the job which triggers the collector and reporter job for a given environment.
                description("Scheduled trigger of the " + env_config.get('DOWNSTREAM_JOB_NAME') + " job for the " + env_config.get('DOWNSTREAM_JOB_NAME') + " environment")
                // Marking the jobs disabled, they will be enabled as part of DENG-1101
                disabled(env_config.get('DISABLED'))

                authorization common_authorization(env_config)
                // Disallow this job to have simultaneous instances building at the same
                // time.  This might help prevent race conditions related to triggering
                // multiple retirement driver jobs against the same user.
                concurrentBuild(false)
                triggers common_triggers(allVars, env_config)

                // keep jobs around for 30 days
                // allVars contains the value for DAYS_TO_KEEP_BUILD which will be used inside AnalyticsConstants.common_log_rotator
                logRotator common_log_rotator(allVars)

                wrappers {
                    buildName('#${BUILD_NUMBER}')
                    timestamps()
                    colorizeOutput('xterm')
                }
                wrappers common_wrappers(allVars)
                parameters secure_scm_parameters(allVars)
                steps {
                    downstreamParameterized {
                        trigger(env_config.get('DOWNSTREAM_JOB_NAME')) {
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
                                predefinedProp('ENVIRONMENT', env_config.get('ENVIRONMENTS_DEPLOYMENT'))
                            }
                        }
                    }
                }
            }
        }

    }
}
