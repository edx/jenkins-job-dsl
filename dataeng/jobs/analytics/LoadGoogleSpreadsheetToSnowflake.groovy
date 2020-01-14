package analytics
import static org.edx.jenkins.dsl.AnalyticsConstants.common_multiscm
import static org.edx.jenkins.dsl.AnalyticsConstants.common_parameters
import static org.edx.jenkins.dsl.AnalyticsConstants.to_date_interval_parameter
import static org.edx.jenkins.dsl.AnalyticsConstants.common_log_rotator
import static org.edx.jenkins.dsl.AnalyticsConstants.common_wrappers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_publishers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_triggers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_authorization

class LoadGoogleSpreadsheetToSnowflake {
    public static def job = { dslFactory, allVars ->
        dslFactory.job("load-google-spreadsheet-to-snowflake") {
            authorization common_authorization(allVars)
            logRotator common_log_rotator(allVars)
            parameters common_parameters(allVars)
            parameters to_date_interval_parameter(allVars)
            parameters {
                stringParam('GOOGLE_CREDENTIALS', allVars.get('GOOGLE_CREDENTIALS'))
            }
            multiscm common_multiscm(allVars)
            triggers common_triggers(allVars)
            wrappers common_wrappers(allVars)
            publishers common_publishers(allVars)
            steps {
                shell(dslFactory.readFileFromWorkspace('dataeng/resources/load-google-spreadsheet-snowflake.sh'))
            }
        }
    }
}
