package analytics
import static org.edx.jenkins.dsl.AnalyticsConstants.secure_scm
import static org.edx.jenkins.dsl.AnalyticsConstants.secure_scm_parameters
import static org.edx.jenkins.dsl.AnalyticsConstants.common_log_rotator

class GenerateWarehouseDocs {
    public static def job = { dslFactory, allVars ->
        dslFactory.job('generate-warehouse-docs') {
            parameters secure_scm_parameters(allVars)
            parameters {
                stringParam('TOOLS_REPO', allVars.get('TOOLS_REPO_URL'), '')
                stringParam('TOOLS_BRANCH', 'refs/remotes/origin/master', '')
                stringParam('DOCBOT_SCHEMATA', 'production,business_intelligence,finance,experimental_events_run14', 'CSV list of schemata to generate docs')
                stringParam('DOCBOT_SPACE', 'DE', '')
                stringParam('DOCBOT_ROOT_PAGE', 'Data Model Documentation', '')
                stringParam('CONFIG_FILE_PATH', 'analytics-secure/warehouse-docs/config.yml', '')
            }
            logRotator common_log_rotator(allVars)
            multiscm secure_scm(allVars) << {
                git {
                    remote {
                        url('$TOOLS_REPO')
                        branch('$TOOLS_BRANCH')
                        credentials('1')
                    }
                    extensions {
                        relativeTargetDirectory('analytics-tools')
                        pruneBranches()
                    }
                }
            }
            // This job doesn't define its own upstream triggers, but rather sql-scripts specifies to trigger this job
            // via downstream().
            triggers {}
            wrappers {
                timestamps()
            }
            steps {
                shell(dslFactory.readFileFromWorkspace("dataeng/resources/generate-warehouse-docs.sh"))
            }
        }
    }
}

