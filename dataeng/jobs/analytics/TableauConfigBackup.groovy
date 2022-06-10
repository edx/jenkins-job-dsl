package analytics
import static org.edx.jenkins.dsl.AnalyticsConstants.secure_scm
import static org.edx.jenkins.dsl.AnalyticsConstants.common_log_rotator
import static org.edx.jenkins.dsl.AnalyticsConstants.common_wrappers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_publishers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_triggers
import static org.edx.jenkins.dsl.AnalyticsConstants.secure_scm_parameters

class TableauConfigBackup{
    public static def job = { dslFactory, allVars ->
        dslFactory.job("tableau-config-backup"){
            description(
                "This jenkins job will ssh into Tableau instance and create config backup. " +
                "Further a python script serialize the json object and compares baseline config with newly created backup config. " +
                "If it finds differences it creates a pull request to update baseline config. "
            )
            logRotator common_log_rotator(allVars)
            parameters secure_scm_parameters(allVars)
            multiscm secure_scm(allVars)
            parameters {
                stringParam('ANALYTICS_TOOLS_URL', allVars.get('ANALYTICS_TOOLS_URL'), 'URL for the analytics tools repo.')
                stringParam('ANALYTICS_TOOLS_BRANCH', allVars.get('ANALYTICS_TOOLS_BRANCH'), , 'Branch of analytics tools repo to use.')
                stringParam('TABLEAU_SERVER_DNS', allVars.get('TABLEAU_SERVER_DNS'), 'DNS address of Tableau Server.')
                stringParam('USER_NAME', allVars.get('USER_NAME'), 'User name to be used for ssh into instance.')
                stringParam('S3_BUCKET', allVars.get('S3_BUCKET'), 'S3 backup bucket name with path. This bucket will be used to store rotating config backup for 60 days.')
                stringParam('NOTIFY', allVars.get('NOTIFY','$PAGER_NOTIFY'), 'Space separated list of emails to send notifications to.')
            }
            triggers common_triggers(allVars)
            wrappers {
                colorizeOutput('xterm')
                timestamps()
                credentialsBinding {
                    string('GITHUB_TOKEN', 'GHPRB_BOT_TOKEN');
                }
            }
            multiscm secure_scm(allVars) << {
                git {
                    remote {
                        url('$ANALYTICS_TOOLS_URL')
                        branch('$ANALYTICS_TOOLS_BRANCH')
                        credentials('1')
                    }
                    extensions {
                        relativeTargetDirectory('analytics-tools')
                        pruneBranches()
                        cleanAfterCheckout()
                    }
                }
            }
            wrappers common_wrappers(allVars)
            publishers common_publishers(allVars)
            steps {
                shell(dslFactory.readFileFromWorkspace('dataeng/resources/tableau-config-backup.sh'))
            }
        }
    }
}
