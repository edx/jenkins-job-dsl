package analytics
import static org.edx.jenkins.dsl.AnalyticsConstants.common_multiscm
import static org.edx.jenkins.dsl.AnalyticsConstants.common_parameters
import static org.edx.jenkins.dsl.AnalyticsConstants.date_interval_parameters
import static org.edx.jenkins.dsl.AnalyticsConstants.common_log_rotator
import static org.edx.jenkins.dsl.AnalyticsConstants.common_wrappers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_publishers

class Enrollment {
    public static def job = { dslFactory, extraVars ->
        extraVars.get('ENVIRONMENTS').each { environment, env_config ->
            dslFactory.job("enrollment-$environment") {
                logRotator common_log_rotator(extraVars)
                parameters common_parameters(extraVars, env_config)
                parameters date_interval_parameters(extraVars)
                parameters {
                    stringParam('EXTRA_ARGS', '')
                }
                multiscm common_multiscm(extraVars)
                triggers {
                    cron(env_config.get('JOB_FREQUENCY', ''))
                }
                wrappers common_wrappers(extraVars)
                publishers common_publishers(extraVars)
                steps {
                    shell(dslFactory.readFileFromWorkspace('dataeng/resources/enrollment.sh'))
                    shell('curl https://nosnch.in/' + env_config.get('SNITCH', ''))
                }
            }
        }
    }
}
