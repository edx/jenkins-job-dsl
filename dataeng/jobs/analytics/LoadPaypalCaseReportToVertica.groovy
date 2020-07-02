package analytics
import static org.edx.jenkins.dsl.AnalyticsConstants.common_multiscm
import static org.edx.jenkins.dsl.AnalyticsConstants.common_parameters
import static org.edx.jenkins.dsl.AnalyticsConstants.common_log_rotator
import static org.edx.jenkins.dsl.AnalyticsConstants.common_wrappers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_publishers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_triggers

class LoadPaypalCaseReportToVertica{
    public static def job = { dslFactory, allVars ->
        dslFactory.job("load-paypal-casereport-to-vertica"){

            // DENG-633
            disabled(true)

            logRotator common_log_rotator(allVars)
            parameters common_parameters(allVars)
            parameters {
                stringParam('SCHEMA', allVars.get('SCHEMA'))
                stringParam('RUN_DATE', allVars.get('RUN_DATE', 'yesterday'), 'The date for which to pull and load Case Report. A string that can be parsed by the GNU coreutils "date" utility.')
            }
            multiscm common_multiscm(allVars)
            triggers common_triggers(allVars)
            wrappers common_wrappers(allVars)
            publishers common_publishers(allVars)
            steps {
                shell(dslFactory.readFileFromWorkspace('dataeng/resources/load-paypal-casereport-to-vertica.sh'))
            }
        }
    }
}
