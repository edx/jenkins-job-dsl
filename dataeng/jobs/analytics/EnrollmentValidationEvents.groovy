package analytics
import static org.edx.jenkins.dsl.AnalyticsConstants.common_multiscm
import static org.edx.jenkins.dsl.AnalyticsConstants.common_parameters
import static org.edx.jenkins.dsl.AnalyticsConstants.from_date_interval_parameter
import static org.edx.jenkins.dsl.AnalyticsConstants.to_date_interval_parameter
import static org.edx.jenkins.dsl.AnalyticsConstants.common_log_rotator
import static org.edx.jenkins.dsl.AnalyticsConstants.common_wrappers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_publishers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_triggers

class EnrollmentValidationEvents {
    public static def job = { dslFactory, allVars ->
        allVars.get('ENVIRONMENTS').each { environment, env_config ->
            dslFactory.job("enrollment-validation-events-$environment") {
                // As part of the MySQL upgrade for Insights/Data API, we need to disable the jobs that
                // interface with the resultstore.
                // TODO: once the upgrade is complete for both prod and edge environments, remove this line
                disabled(env_config.get('JOB_DISABLED'))

                logRotator common_log_rotator(allVars)
                parameters common_parameters(allVars, env_config)
                parameters from_date_interval_parameter(allVars)
                parameters to_date_interval_parameter(allVars)
                parameters {
                    stringParam('OUTPUT_ROOT', env_config.get('OUTPUT_ROOT'), '')
                    stringParam('CREDENTIALS', env_config.get('CREDENTIALS'), '')
                    stringParam('FILE_THRESHOLD', env_config.get('FILE_THRESHOLD'),
                        'Threshold to apply to synthetic event files per day.  Gzipped files that are bigger' +
                        ' than this threshold trigger a job failure and an alert.  (Sadly, we ignore the smaller' +
                        ' files as being "normal".)')
                }
                multiscm common_multiscm(allVars)
                triggers common_triggers(allVars, env_config)
                wrappers common_wrappers(allVars)
                publishers {
                    postBuildTask {
                        task('OVER THRESHOLD', 'exit 1', true)
                    }
                }
                publishers common_publishers(allVars)
                steps {
                    shell(dslFactory.readFileFromWorkspace('dataeng/resources/enrollment-validation-events.sh'))
                }
            }
        }
    }
}
