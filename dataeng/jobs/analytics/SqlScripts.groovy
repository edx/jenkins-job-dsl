package analytics
import static org.edx.jenkins.dsl.AnalyticsConstants.common_multiscm
import static org.edx.jenkins.dsl.AnalyticsConstants.common_parameters
import static org.edx.jenkins.dsl.AnalyticsConstants.common_log_rotator
import static org.edx.jenkins.dsl.AnalyticsConstants.common_wrappers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_publishers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_triggers

class SqlScripts {
    public static def job = { dslFactory, allVars ->
        dslFactory.job("sql-scripts") {
            disabled()
            logRotator common_log_rotator(allVars)
            parameters {
                stringParam('SCHEMA', allVars.get('SCHEMA'))
                stringParam('CREDENTIALS', allVars.get('CREDENTIALS'))
                stringParam('SCRIPTS_REPO', allVars.get('SCRIPTS_REPO_URL'))
                stringParam('SCRIPTS_BRANCH', 'origin/master')
                stringParam('EXTRA_ARGS', '')
            }
            parameters common_parameters(allVars)
            multiscm common_multiscm(allVars)
            triggers common_triggers(allVars)
            wrappers common_wrappers(allVars)
            publishers common_publishers(allVars)
            publishers {
                downstream('generate-warehouse-docs', 'SUCCESS')
            }
            steps {
                shell(dslFactory.readFileFromWorkspace('dataeng/resources/sql-scripts.sh'))
            }
        }
    }
}
