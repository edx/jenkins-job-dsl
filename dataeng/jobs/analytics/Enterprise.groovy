package analytics
import static org.edx.jenkins.dsl.AnalyticsConstants.common_multiscm
import static org.edx.jenkins.dsl.AnalyticsConstants.common_parameters
import static org.edx.jenkins.dsl.AnalyticsConstants.common_log_rotator
import static org.edx.jenkins.dsl.AnalyticsConstants.common_wrappers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_publishers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_triggers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_authorization

class Enterprise {
    public static def job = { dslFactory, allVars ->
        allVars.get('JOBS').each { job, job_config ->
            dslFactory.job("enterprise-$job") {
                // As part of the MySQL upgrade for Insights/Data API, we need to disable the jobs that
                // interface with the resultstore.
                // TODO: once the upgrade is complete for both prod and edge environments, remove this line
                disabled(job_config.get('JOB_DISABLED'))

                authorization common_authorization(allVars)
                logRotator common_log_rotator(allVars)
                parameters common_parameters(allVars, job_config)
                parameters {
                    stringParam('REPORT_DATE', allVars.get('REPORT_DATE'), '')
                }
                multiscm common_multiscm(allVars)
                triggers common_triggers(allVars)
                wrappers common_wrappers(allVars)
                publishers common_publishers(allVars)
                steps {
                    shell(dslFactory.readFileFromWorkspace("dataeng/resources/enterprise-${job}.sh"))
                }
            }
        }
    }
}
