package analytics
import static org.edx.jenkins.dsl.AnalyticsConstants.analytics_configuration_scm
import static org.edx.jenkins.dsl.AnalyticsConstants.common_log_rotator
import static org.edx.jenkins.dsl.AnalyticsConstants.common_wrappers

class TerminateCluster {
    public static def job = { dslFactory, allVars ->
        dslFactory.job("terminate-cluster") {
            logRotator common_log_rotator(allVars)
            parameters {
                stringParam('CLUSTER_NAME', ' ', 'Name of the EMR cluster to terminate.')
                stringParam('CONFIG_REPO', 'git@github.com:edx/edx-analytics-configuration.git', '')
                stringParam('CONFIG_BRANCH', '$ANALYTICS_CONFIGURATION_RELEASE', 'e.g. tagname or origin/branchname, or $ANALYTICS_CONFIGURATION_RELEASE')
            }
            multiscm analytics_configuration_scm(allVars)
            steps {
                virtualenv {
                    nature("shell")
                    command('EXTRA_VARS="name=$CLUSTER_NAME" make terminate.emr')
                }
            }
        }
    }
}
