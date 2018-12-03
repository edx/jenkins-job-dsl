package analytics
import static org.edx.jenkins.dsl.AnalyticsConstants.common_multiscm
import static org.edx.jenkins.dsl.AnalyticsConstants.common_parameters
import static org.edx.jenkins.dsl.AnalyticsConstants.common_log_rotator
import static org.edx.jenkins.dsl.AnalyticsConstants.common_wrappers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_publishers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_triggers

class SqlScripts {

    public static def sql_script_params = { allVars ->
        return {
            stringParam('SCHEMA', allVars.get('SCHEMA'))
            stringParam('CREDENTIALS', allVars.get('CREDENTIALS'))
            stringParam('SCRIPTS_REPO', allVars.get('SCRIPTS_REPO_URL'))
            stringParam('SCRIPTS_BRANCH', 'origin/master')
            stringParam('EXTRA_ARGS', '')
        }
    }

    public static def multiple_scripts_job = { dslFactory, allVars ->
        dslFactory.job("sql-scripts") {
            logRotator common_log_rotator(allVars)
            parameters SqlScripts.sql_script_params(allVars)
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

    public static def single_script_job = { dslFactory, allVars ->
        dslFactory.job("single-sql-script") {
            logRotator common_log_rotator(allVars)
            parameters SqlScripts.sql_script_params(allVars)
            parameters {
                stringParam('SOURCE_SCRIPT', '', 'Path to the source script, relative to the root of the repository. Most scripts just require a script name.')
                stringParam('SCRIPT_NAME')
            }
            parameters common_parameters(allVars)
            multiscm common_multiscm(allVars)
            wrappers common_wrappers(allVars)
            publishers common_publishers(allVars)
            steps {
                shell(dslFactory.readFileFromWorkspace('dataeng/resources/single-sql-script.sh'))
            }
        }
    }
}
