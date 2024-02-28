/*

Defines the job for archiving retired user statuses.

Job config variables consumed for this job (via extraVars):
  * ENVIRONMENT_DEPLOYMENT: The environment and deployment written as $environment-$deployment
  * FOLDER_NAME: Name of jenkins "folder" under which to put this job.
  * SECURE_GIT_CREDENTIALS: Name of jenkins git credential scoped to the UserRetirement "folder" in jenkins.
  * ACCESS_CONTROL: List of github groups with read and build access to this job.
  * ADMIN_ACCESS_CONTROL: List of github groups with full admin access to this job.
  * CRON: Optional. Defines the cron line for scheduling the job.
  * MAILING_LIST: Optional. A string containing a comma-delimited list of emails to notify when a build fails.

This job expects the following credentials to be defined on the folder:
  * tools-edx-jenkins-aws-credentials: file with key/secret in boto config format
  * ${environment}-${deployment}-retirement-archive-upload-role: the role to aws sts assume-role

This job archives and deletes user retirement statuses from the LMS database. However, these deletions
are replicated to Snowflake as "soft-deletes". In other words, they are marked as deleted and filtered
from downstream data views, but remain within Snowflake. Therefore, the statuses deleted by this job
are deleted from Snowflake with the following job:
https://github.com/edx/jenkins-job-dsl/blob/master/dataeng/jobs/analytics/SnowflakeUserRetirementStatusCleanup.groovy
*/
package devops.jobs
import static org.edx.jenkins.dsl.UserRetirementConstants.common_access_controls
import static org.edx.jenkins.dsl.UserRetirementConstants.common_publishers
import static org.edx.jenkins.dsl.UserRetirementConstants.common_triggers
import static org.edx.jenkins.dsl.UserRetirementConstants.common_wrappers
import static org.edx.jenkins.dsl.UserRetirementConstants.configuration_parameters
import static org.edx.jenkins.dsl.UserRetirementConstants.configuration_repo
import static org.edx.jenkins.dsl.UserRetirementConstants.edx_platform_parameters
import static org.edx.jenkins.dsl.UserRetirementConstants.edx_platform_repo

class UserRetirementArchiver {
    public static def job = { dslFactory, extraVars ->

        // Job config assertions:
        assert (extraVars.get('MAILING_LIST', '') instanceof String) :
            "Make sure MAILING_LIST is a single string (containing a comma-delimited list) and not a proper Yaml " +
            "list or some other non-string type."

        def job_name = 'archiver-' + extraVars.get('ENVIRONMENT_DEPLOYMENT')
        if (extraVars.containsKey("FOLDER_NAME")) {
            job_name = extraVars.get("FOLDER_NAME") + '/' + job_name
        }
        dslFactory.job(job_name) {

            description('Archive the old user retirement status rows to AWS S3.')

            ////
            // First we write the DSL statements which should be common to ALL user retirement jobs.
            ////

            authorization common_access_controls(extraVars)
            triggers common_triggers(extraVars)
            wrappers common_wrappers(extraVars)
            publishers common_publishers(extraVars)
            disabled(extraVars.get('DISABLED'))  // Jobs may be disabled for testing/rollout.
            checkoutRetryCount(5)  // Retry cloning repositories.

            ////
            // Now, everything which follows is custom to this particular job.
            ////

            parameters edx_platform_parameters(extraVars) << configuration_parameters(extraVars)
            multiscm edx_platform_repo(extraVars) << configuration_repo(extraVars)

            // Only one of these jobs should be running at a time.
            concurrentBuild(false)

            logRotator {
                daysToKeep(30) // keep jobs around for 30 days.
            }

            wrappers {
                credentialsBinding {
                    string('ROLE_ARN', extraVars.get('ENVIRONMENT_DEPLOYMENT') + '-retirement-archive-upload-role')
                    string('SECRET_ARN', extraVars.get('ENVIRONMENT_DEPLOYMENT') + '-retirement-archive-secret-role')
                }
            }

            parameters {
                stringParam(
                    'COOL_OFF_DAYS',
                    '74',
                    'Number of days after retirement request when a user retirement status should be archived in S3.'
                )
                stringParam(
                    'BATCH_SIZE',
                    '1000',
                    'Size of batches of learner retirments to process.'
                )
                stringParam(
                    'START_DATE',
                    '2018-01-01',
                    'Start of window used to select user retirements for archival. Only user retirements added to the retirement queue after this date will be processed.'
                )
                stringParam(
                    'END_DATE',
                    '',
                    'End of window used to select user retirments for archival. Only user retirments added to the retirement queue before this date will be processed. In the case that this date is more recent than the value specified in the `cool_off_days` parameter, an error will be thrown. If this parameter is not used, the script will default to using an end_date based upon the `cool_off_days` parameter.'
                )
                stringParam(
                    'DRY_RUN',
                    'False',
                    'Run this script with the `dry_run` flag, which will not perform the archival or deletion of a user.'
                )
            }

            steps {
                  shell(dslFactory.readFileFromWorkspace('devops/resources/user-retirement-archiver.sh'))
            }
        }
    }
}
