package analytics
import static org.edx.jenkins.dsl.AnalyticsConstants.analytics_configuration_scm
import static org.edx.jenkins.dsl.AnalyticsConstants.common_log_rotator
import static org.edx.jenkins.dsl.AnalyticsConstants.common_wrappers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_groovy_postbuild
import static org.edx.jenkins.dsl.AnalyticsConstants.common_datadog_build_start
import static org.edx.jenkins.dsl.AnalyticsConstants.common_datadog_build_end

class UpdateUsers {
    public static def job = { dslFactory, allVars ->
        dslFactory.job("update-users") {
            logRotator common_log_rotator(allVars)
            parameters {
                textParam('EXTRA_VARS', allVars.get('EXTRA_VARS'), '')
                stringParam('REMOTE_USER', allVars.get('REMOTE_USER'), 'User which runs the analytics task on the EMR cluster.')
                stringParam('CONFIG_REPO', 'git@github.com:edx/edx-analytics-configuration.git', '')
                stringParam('CONFIG_BRANCH', allVars.get('BRANCH'), 'e.g. tagname or origin/branchname, or $ANALYTICS_CONFIGURATION_RELEASE')
                stringParam('PYTHON_VENV_VERSION', 'python3.7', 'Python virtual environment version to used.')
                stringParam('BUILD_STATUS')
            }
            multiscm analytics_configuration_scm(allVars)
            wrappers common_wrappers(allVars)
            steps {
                shell(dslFactory.readFileFromWorkspace('dataeng/resources/update-users.sh'))
            }
            publishers common_datadog_build_end(dslFactory, allVars) << common_groovy_postbuild(dslFactory, allVars) << {
                postBuildTask {
                    task('skipping: no hosts matched', 'exit 1', true, true)
                }
            }
            // publishers {
            //     groovyPostBuild('println "hello, world"', Behavior.MarkFailed)
            // }
        }
    }
}
