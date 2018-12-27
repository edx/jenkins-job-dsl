package analytics
import static org.edx.jenkins.dsl.AnalyticsConstants.common_multiscm
import static org.edx.jenkins.dsl.AnalyticsConstants.common_parameters
import static org.edx.jenkins.dsl.AnalyticsConstants.common_log_rotator
import static org.edx.jenkins.dsl.AnalyticsConstants.common_wrappers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_publishers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_triggers

class Enterprise {
    public static def job = { dslFactory, allVars ->
        allVars.get('JOBS').each { job, job_config ->
            dslFactory.job("enterprise-$job") {
                authorization {
                    allVars.get('USER_ROLES').each { github_id, roles ->
                        roles.each {
                            permission(it, github_id)     // it is an implicit parameter corresponding to the current element
                        }
                    }
                }
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
