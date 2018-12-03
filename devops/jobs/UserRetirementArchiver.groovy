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

*/
package devops.jobs
import static org.edx.jenkins.dsl.UserRetirementConstants.common_access_controls
import static org.edx.jenkins.dsl.UserRetirementConstants.common_closures_extra
import static org.edx.jenkins.dsl.UserRetirementConstants.common_multiscm
import static org.edx.jenkins.dsl.UserRetirementConstants.common_parameters
import static org.edx.jenkins.dsl.UserRetirementConstants.common_publishers
import static org.edx.jenkins.dsl.UserRetirementConstants.common_triggers
import static org.edx.jenkins.dsl.UserRetirementConstants.common_wrappers

class UserRetirementArchiver {
    public static def job = { dslFactory, extraVars ->

        // Job config assertions:
        assert (extraVars.get('MAILING_LIST', '') instanceof String) :
            "Make sure MAILING_LIST is a single string (containing a comma-delimited list) and not a proper Yaml " +
            "list or some other non-string type."

        def job_name = extraVars.get("FOLDER_NAME") + '/archiver-' + extraVars.get('ENVIRONMENT_DEPLOYMENT')
        dslFactory.job(job_name) {

            description('Archive the old user retirement status rows to AWS S3.')

            // First we write the DSL statements which should be common to ALL user retirement jobs.
            authorization common_access_controls(extraVars)
            triggers common_triggers(extraVars)
            wrappers common_wrappers(extraVars)
            parameters common_parameters(extraVars)
            checkoutRetryCount(5)  // retry cloning repositories
            multiscm common_multiscm(extraVars)
            publishers common_publishers(extraVars)
            logRotator {
                daysToKeep(30) // keep jobs around for 30 days.
            }

            ////
            // Now, everything which follows is custom to this particular job.
            ////

            // Only one of these jobs should be running at a time.
            concurrentBuild(false)

            wrappers {
                credentialsBinding {
                    file('AWS_CONFIG_FILE','tools-edx-jenkins-aws-credentials')
                    string('ROLE_ARN', extraVars.get('ENVIRONMENT_DEPLOYMENT') + '-retirement-archive-upload-role')
                }
            }

            parameters {
                stringParam(
                    'COOL_OFF_DAYS',
                    '67',
                    'Number of days after retirement request when a user retirement status should be archived in S3.'
                )
            }

            steps {
                virtualenv {
                    pythonName('System-CPython-3.5')
                    name('user-retirement-archiver')
                    nature('shell')
                    systemSitePackages(false)
                    command(readFileFromWorkspace('devops/resources/user-retirement-archiver.sh'))
                }
            }
        }
    }
}
