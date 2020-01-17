package analytics

import static org.edx.jenkins.dsl.AnalyticsConstants.common_multiscm
import static org.edx.jenkins.dsl.AnalyticsConstants.common_parameters
import static org.edx.jenkins.dsl.AnalyticsConstants.common_log_rotator
import static org.edx.jenkins.dsl.AnalyticsConstants.common_wrappers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_publishers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_triggers



class ProgramEnrollmentReports {

    public static def job = { dslFactory, allVars ->

        dslFactory.job('program-enrollment-reports') {

            // disabled until downstream reporting work is complete
            disabled(true)

            logRotator common_log_rotator(allVars)
            parameters common_parameters(allVars)
            parameters {
                stringParam('OUTPUT_ROOT', allVars.get('OUTPUT_ROOT'), 'Report output location')
                stringParam('VERTICA_CREDENTIALS', allVars.get('VERTICA_CREDENTIALS'), 'The path to the Vertica credentials file.')
            }
 
            multiscm common_multiscm(allVars)
            triggers common_triggers(allVars)
            wrappers common_wrappers(allVars)
            publishers common_publishers(allVars)
            steps {
                shell(dslFactory.readFileFromWorkspace('dataeng/resources/program-enrollment-reports.sh'))
            }
        }
    }
}
