package analytics
import static org.edx.jenkins.dsl.AnalyticsConstants.common_multiscm
import static org.edx.jenkins.dsl.AnalyticsConstants.common_parameters
import static org.edx.jenkins.dsl.AnalyticsConstants.date_interval_parameters
import static org.edx.jenkins.dsl.AnalyticsConstants.common_log_rotator
import static org.edx.jenkins.dsl.AnalyticsConstants.common_wrappers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_publishers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_triggers

class ModuleEngagement {
    public static def job = { dslFactory, allVars ->
        allVars.get('ENVIRONMENTS').each { environment, env_config ->
            dslFactory.job("module-engagement-$environment") {
                //TODO: Remove this
                disabled()
                logRotator common_log_rotator(allVars)
                parameters common_parameters(allVars, env_config)
                parameters date_interval_parameters(allVars)
                parameters {
                    stringParam('ADDITIONAL_TASK_ARGS', '', 'provide additional args, such as --overwrite-n-days 14')
                }
                multiscm common_multiscm(allVars)
                triggers common_triggers(allVars, env_config)
                wrappers common_wrappers(allVars)
                publishers common_publishers(allVars)
                steps {
                    shell(dslFactory.readFileFromWorkspace('dataeng/resources/module-engagement.sh'))
                    if (env_config.get('SNITCH')) {
                        shell('curl https://nosnch.in/' + env_config.get('SNITCH'))
                    }
                }
            }
        }
    }
}
