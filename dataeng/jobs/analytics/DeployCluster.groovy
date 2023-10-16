package analytics
import static org.edx.jenkins.dsl.AnalyticsConstants.analytics_configuration_scm
import static org.edx.jenkins.dsl.AnalyticsConstants.emr_cluster_parameters
import static org.edx.jenkins.dsl.AnalyticsConstants.common_log_rotator
import static org.edx.jenkins.dsl.AnalyticsConstants.common_wrappers

class DeployCluster {
    public static def job = { dslFactory, allVars ->
        dslFactory.job("deploy-cluster") {
            description('Job to provision an EMR cluster, typically for adhoc usage.')
            logRotator common_log_rotator(allVars)
            parameters {
                stringParam('CONFIG_REPO', 'git@github.com:edx/edx-analytics-configuration.git', '')
                stringParam('CONFIG_BRANCH', '$ANALYTICS_CONFIGURATION_RELEASE', 'e.g. tagname or origin/branchname, or $ANALYTICS_CONFIGURATION_RELEASE')
            }
            parameters emr_cluster_parameters(allVars)
            multiscm analytics_configuration_scm(allVars)
            wrappers common_wrappers(allVars)
            steps {
                shell(dslFactory.readFileFromWorkspace('dataeng/resources/deploy-cluster.sh'))
            }
        }
    }
}
