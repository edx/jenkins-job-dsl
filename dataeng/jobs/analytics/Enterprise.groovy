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
                disabled(job_config.get('DISABLED', false))
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
