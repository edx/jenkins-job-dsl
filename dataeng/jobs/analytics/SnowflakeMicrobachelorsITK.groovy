/*
Creates the MicroBachelors InsideTrack job. This job is responsible for running the Python script
that queries Snowflake for MicroBachelors learner data and transmits it to edX's coaching partner.
*/

package analytics

import static org.edx.jenkins.dsl.AnalyticsConstants.common_publishers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_triggers
import static org.edx.jenkins.dsl.AnalyticsConstants.secure_scm
import static org.edx.jenkins.dsl.AnalyticsConstants.secure_scm_parameters
import static org.edx.jenkins.dsl.AnalyticsConstants.common_authorization


class SnowflakeMicrobachelorsITK {

    public static def job = { dslFactory, allVars ->
        dslFactory.job('snowflake-microbachelors-send-coaching-data-itk') {
            logRotator {
                daysToKeep(30)
                artifactDaysToKeep(7)
            }
            authorization common_authorization(allVars)
            parameters secure_scm_parameters(allVars)
            parameters {
                stringParam('ANALYTICS_TOOLS_URL', allVars.get('ANALYTICS_TOOLS_URL'), 'URL for the analytics tools repo.')
                stringParam('ANALYTICS_TOOLS_BRANCH', allVars.get('ANALYTICS_TOOLS_BRANCH'), 'Branch of analytics tools repo to use.')
                stringParam('NOTIFY', allVars.get('NOTIFY','$PAGER_NOTIFY'), 'Space separated list of emails to send notifications to.')
            }
            environmentVariables {
                env('KEY_PATH', allVars.get('KEY_PATH'))
                env('PASSPHRASE_PATH', allVars.get('PASSPHRASE_PATH'))
                env('USER', allVars.get('USER'))
                env('ACCOUNT', allVars.get('ACCOUNT'))
                env('SFTP_CREDENTIALS_BUCKET', allVars.get('SFTP_CREDENTIALS_BUCKET'))
                env('SFTP_STUDENT_PATH', allVars.get('SFTP_STUDENT_PATH'))
                env('SFTP_COURSE_PATH', allVars.get('SFTP_COURSE_PATH'))
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
            triggers {
                cron('H 6 * * 1-5')
            }
            wrappers {
                timestamps()
            }
            publishers {
                archiveArtifacts {
                    // job normally will generate two unique CSV files, each ending with timestamps as part of the file name
                    pattern('analytics-tools/snowflake/*.csv')
                    allowEmpty()
                    defaultExcludes()
                }
            }
            publishers common_publishers(allVars)
            steps {
                virtualenv {
                    pythonName('PYTHON_3.7')
                    nature("shell")
                    systemSitePackages(false)
                    command(
                        dslFactory.readFileFromWorkspace('dataeng/resources/snowflake-microbachelors-send-coaching-data-itk.sh')
                    )
                }
            }
        }
    }
}
