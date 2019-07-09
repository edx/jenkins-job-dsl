package analytics
import static org.edx.jenkins.dsl.AnalyticsConstants.common_multiscm
import static org.edx.jenkins.dsl.AnalyticsConstants.common_parameters
import static org.edx.jenkins.dsl.AnalyticsConstants.common_log_rotator
import static org.edx.jenkins.dsl.AnalyticsConstants.common_wrappers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_publishers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_triggers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_authorization

class SqlScripts {

    public static def sql_script_params = { allVars, env=[:] ->
        return {
            stringParam('SCHEMA', env.get('SCHEMA', allVars.get('SCHEMA')))
            stringParam('CREDENTIALS', allVars.get('CREDENTIALS'))
            stringParam('SCRIPTS_REPO', allVars.get('SCRIPTS_REPO_URL'))
            stringParam('SCRIPTS_BRANCH', 'origin/master')
            stringParam('SCRIPTS_CONFIG', env.get('SCRIPTS_CONFIG', ''))
        }
    }

    public static def multiple_scripts_job = { dslFactory, allVars ->
        allVars.get('ENVIRONMENTS').each { environment, env_config ->
            dslFactory.job("sql-scripts-$environment") {
                authorization common_authorization(allVars)
                logRotator common_log_rotator(allVars)
                parameters SqlScripts.sql_script_params(allVars, env_config)
                parameters common_parameters(allVars, env_config)
                multiscm common_multiscm(allVars)
                triggers common_triggers(allVars, env_config)
                wrappers common_wrappers(allVars)
                publishers common_publishers(allVars)
                steps {
                    shell(dslFactory.readFileFromWorkspace('dataeng/resources/sql-scripts.sh'))
                }
            }
        }
    }

    public static def single_script_job = { dslFactory, allVars ->
        dslFactory.job("single-sql-script") {
            authorization common_authorization(allVars)
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
